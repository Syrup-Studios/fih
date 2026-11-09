package net.syrupstudios.fih.mixin;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
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
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.syrupstudios.fih.FihGameRules;
import net.syrupstudios.fih.ai.CastingAccess;
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
        Entity hooked = player.fishing == null ? null : player.fishing.getHookedIn();
        if (hooked != null && !(hooked instanceof AbstractFish)) return;
        ((CastingAccess) player).fih$setCasting(player.fishing == null);
        player.startUsingItem(hand);
        cir.setReturnValue(InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide()));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseDuration) {
        if (!(user instanceof Player player)) return;
        CastingAccess casting = (CastingAccess) player;
        if (!casting.fih$isCasting()) return;
        casting.fih$setCasting(false);
        if (!(level instanceof ServerLevel serverLevel) || player.fishing != null) return;
        float charge = BowItem.getPowerForTime(getUseDuration(stack, player) - remainingUseDuration);
        if (charge < 0.1F) return;
        float strength = Math.clamp(level.getGameRules().getInt(FihGameRules.CAST_STRENGTH), 0, 500) / 100.0F;
        int lure = (int) (EnchantmentHelper.getFishingTimeReduction(serverLevel, stack, player) * 20.0F);
        int luck = EnchantmentHelper.getFishingLuckBonus(serverLevel, stack, player);
        FishingHook hook = new FishingHook(player, level, luck, lure);
        hook.setDeltaMovement(hook.getDeltaMovement().scale(charge * strength));
        level.addFreshEntity(hook);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW,
                SoundSource.NEUTRAL, 0.5F, 1.0F);
        player.awardStat(Stats.ITEM_USED.get(this));
        player.gameEvent(GameEvent.ITEM_INTERACT_START);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(user instanceof Player player)) return;
        if (((CastingAccess) player).fih$isCasting()) return;
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
        double distanceSquared = hooked == null
                ? ((FishingHookAccess) hook).fih$hookPosition().distanceToSqr(player.position())
                : hooked.distanceToSqr(player);
        if (distanceSquared > 6.25) return;

        InteractionHand hand = player.getUsedItemHand();
        int damage = hook.retrieve(stack);
        stack.hurtAndBreak(damage, player, LivingEntity.getSlotForHand(hand));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        player.stopUsingItem();
    }
}
