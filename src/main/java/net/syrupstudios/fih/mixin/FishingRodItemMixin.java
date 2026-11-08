package net.syrupstudios.fih.mixin;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.syrupstudios.fih.ai.FishingHookAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingRodItem.class)
public abstract class FishingRodItemMixin extends Item {
    protected FishingRodItemMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void fih$startReeling(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (player.fishing == null) return;
        Entity hooked = player.fishing.getHookedIn();
        if (hooked != null && !(hooked instanceof AbstractFish)) return;
        player.startUsingItem(hand);
        cir.setReturnValue(InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide()));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(user instanceof Player player)) return;
        FishingHook hook = player.fishing;
        if (hook == null || hook.isRemoved()) {
            player.stopUsingItem();
            return;
        }

        Entity hooked = hook.getHookedIn();
        if (hooked != null && (!(hooked instanceof AbstractFish fish) || !fish.isAlive())) {
            player.stopUsingItem();
            return;
        }

        ((FishingHookAccess) hook).fih$reelIn();
        Entity reeledEntity = hooked == null ? hook : hooked;
        if (reeledEntity.distanceToSqr(player) > 6.25) return;

        InteractionHand hand = player.getUsedItemHand();
        int damage = hook.retrieve(stack);
        stack.hurtAndBreak(damage, player, LivingEntity.getSlotForHand(hand));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        player.stopUsingItem();
    }
}
