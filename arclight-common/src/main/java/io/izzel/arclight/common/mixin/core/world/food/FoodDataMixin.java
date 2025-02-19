package io.izzel.arclight.common.mixin.core.world.food;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.util.FoodStatsBridge;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin implements FoodStatsBridge {

    // @formatter:off
    @Shadow public int foodLevel;
    @Shadow public abstract void eat(int foodLevelIn, float foodSaturationModifier);
    @Shadow public float saturationLevel;
    @Shadow private int lastFoodLevel;
    // @formatter:on

    private Player entityhuman;
    public int saturatedRegenRate = 10;
    public int unsaturatedRegenRate = 80;
    public int starvationRate = 80;

    @ShadowConstructor
    public void arclight$constructor() {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void arclight$constructor(Player playerEntity) {
        arclight$constructor();
        this.entityhuman = playerEntity;
    }

    private transient ItemStack arclight$eatStack;

    @Override
    public void bridge$pushEatStack(ItemStack stack) {
        this.arclight$eatStack = stack;
    }

    @Decorate(method = "eat(Lnet/minecraft/world/food/FoodProperties;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;add(IF)V"))
    private void arclight$foodLevelChange(FoodData foodStats, int foodLevelIn, float foodSaturationModifier, FoodProperties food) throws Throwable {
        var stack = this.arclight$eatStack;
        this.arclight$eatStack = null;
        if (this.entityhuman != null && stack != null) {
            int oldFoodLevel = this.foodLevel;
            FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(this.entityhuman, foodLevel, stack);
            if (event.isCancelled()) {
                return;
            }
            foodLevelIn = event.getFoodLevel() - oldFoodLevel;
            ((ServerPlayerEntityBridge) this.entityhuman).bridge$getBukkitEntity().sendHealthUpdate();
        }
        DecorationOps.callsite().invoke(foodStats, foodLevelIn, foodSaturationModifier);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE_ASSIGN", remap = false, target = "Ljava/lang/Math;max(II)I"))
    public void arclight$foodLevelChange2(Player player, CallbackInfo ci) {
        if (entityhuman == null) {
            return;
        }
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(entityhuman, Math.max(this.lastFoodLevel - 1, 0));

        if (!event.isCancelled()) {
            this.foodLevel = event.getFoodLevel();
        } else {
            this.foodLevel = this.lastFoodLevel;
        }

        ((ServerPlayer) entityhuman).connection.send(new ClientboundSetHealthPacket(((ServerPlayerEntityBridge) entityhuman).bridge$getBukkitEntity().getScaledHealth(), this.foodLevel, this.saturationLevel));
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    public void arclight$heal(Player player, CallbackInfo ci) {
        if (entityhuman == null) {
            entityhuman = player;
        }
        ((LivingEntityBridge) player).bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.SATIATED);
        ((PlayerEntityBridge) player).bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.REGEN);
    }

    @Override
    public void bridge$setEntityHuman(Player playerEntity) {
        this.entityhuman = playerEntity;
    }

    @Override
    public Player bridge$getEntityHuman() {
        return this.entityhuman;
    }
}
