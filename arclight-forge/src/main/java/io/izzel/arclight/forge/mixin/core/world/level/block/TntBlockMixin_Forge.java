package io.izzel.arclight.forge.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.TNTPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
public abstract class TntBlockMixin_Forge extends BlockMixin_Forge {

    @Redirect(method = "playerWillDestroy", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/level/block/TntBlock;onCaughtFire(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$playerBreak(TntBlock instance, BlockState state, Level world, BlockPos pos, Direction face, LivingEntity igniter,
                                      Level p_57445_, BlockPos p_57446_, BlockState p_57447_, Player player) {
        if (CraftEventFactory.callTNTPrimeEvent(world, pos, TNTPrimeEvent.PrimeCause.BLOCK_BREAK, player, null)) {
            this.bridge$forge$onCaughtFire(state, world, pos, face, igniter);
        }
    }

    @Inject(method = "useItemOn", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/level/block/TntBlock;onCaughtFire(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$player(ItemStack arg, BlockState arg2, Level level, BlockPos pos, Player player, InteractionHand arg6, BlockHitResult arg7, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!CraftEventFactory.callTNTPrimeEvent(level, pos, TNTPrimeEvent.PrimeCause.PLAYER, player, null)) {
            cir.setReturnValue(ItemInteractionResult.CONSUME);
        }
    }

    @Inject(method = "onProjectileHit", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/level/block/TntBlock;onCaughtFire(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$entityChangeBlock(Level worldIn, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        if (!CraftEventFactory.callEntityChangeBlockEvent(projectile, hit.getBlockPos(), Blocks.AIR.defaultBlockState())
            || !CraftEventFactory.callTNTPrimeEvent(worldIn, hit.getBlockPos(), TNTPrimeEvent.PrimeCause.PROJECTILE, projectile, null)) {
            ci.cancel();
        }
    }
}
