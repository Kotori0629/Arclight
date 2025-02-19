package io.izzel.arclight.common.mixin.core.world.entity.item;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.item.ItemEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerInventoryBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.datasync.SynchedEntityDataBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends EntityMixin implements ItemEntityBridge {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<ItemStack> DATA_ITEM;
    @Shadow public int pickupDelay;
    @Shadow public abstract ItemStack getItem();
    @Shadow public UUID target;
    @Shadow public int age;
    // @formatter:on

    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", cancellable = true, at = @At("HEAD"))
    private static void arclight$itemMerge(ItemEntity from, ItemStack stack1, ItemEntity to, ItemStack stack2, CallbackInfo ci) {
        if (!CraftEventFactory.callItemMergeEvent(to, from)) {
            ci.cancel();
        }
    }

    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private static void arclight$itemMergeCause(ItemEntity from, ItemStack stack1, ItemEntity to, ItemStack stack2, CallbackInfo ci) {
        to.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.MERGE);
    }

    @Inject(method = "hurt", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;markHurt()V"))
    private void arclight$damageNonLiving(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ItemEntity) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void arclight$dead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DEATH);
    }

    @Decorate(method = "playerTouch", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/item/ItemStack;getCount()I"))
    private int arclight$playerPickup(ItemStack instance, Player entity, @Local(ordinal = -1) ItemStack itemstack) throws Throwable {
        var count = (int) DecorationOps.callsite().invoke(instance);

        final int canHold = ((PlayerInventoryBridge) entity.getInventory()).bridge$canHold(itemstack);
        final int remaining = count - canHold;
        if (this.pickupDelay <= 0 && canHold > 0) {
            itemstack.setCount(canHold);
            final PlayerPickupItemEvent playerEvent = new PlayerPickupItemEvent(((ServerPlayerEntityBridge) entity).bridge$getBukkitEntity(), (Item) this.getBukkitEntity(), remaining);
            playerEvent.setCancelled(!((PlayerEntityBridge) entity).bridge$canPickUpLoot());
            Bukkit.getPluginManager().callEvent(playerEvent);
            if (playerEvent.isCancelled()) {
                itemstack.setCount(canHold + remaining);
                return (int) DecorationOps.cancel().invoke();
            }
            final EntityPickupItemEvent entityEvent = new EntityPickupItemEvent(((LivingEntityBridge) entity).bridge$getBukkitEntity(), (Item) this.getBukkitEntity(), remaining);
            entityEvent.setCancelled(!((PlayerEntityBridge) entity).bridge$canPickUpLoot());
            Bukkit.getPluginManager().callEvent(entityEvent);
            if (entityEvent.isCancelled()) {
                itemstack.setCount(canHold + remaining);
                return (int) DecorationOps.cancel().invoke();
            }
            ItemStack current = this.getItem();
            if (!itemstack.equals(current)) {
                itemstack = current;
            } else {
                itemstack.setCount(canHold + remaining);
            }
            this.pickupDelay = 0;
        } else if (this.pickupDelay == 0) {
            this.pickupDelay = -1;
        }
        DecorationOps.blackhole().invoke(itemstack);
        return count;
    }

    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void arclight$discardReason(Player player, CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
    }

    @Decorate(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/item/ItemEntity;age:I")))
    private void arclight$itemDespawn(ItemEntity instance) throws Throwable {
        if (CraftEventFactory.callItemDespawnEvent((ItemEntity) (Object) this).isCancelled()) {
            this.age = 0;
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.callsite().invoke(instance);
    }

    @Inject(method = "setItem", at = @At("RETURN"))
    private void arclight$markDirty(ItemStack stack, CallbackInfo ci) {
        ((SynchedEntityDataBridge) this.getEntityData()).bridge$markDirty(DATA_ITEM);
    }

    @Redirect(method = "mergeWithNeighbours", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"))
    private AABB arclight$mergeRadius(AABB instance, double pX, double pY, double pZ) {
        double radius = ((WorldBridge) level()).bridge$spigotConfig().itemMerge;
        return instance.inflate(radius);
    }
}
