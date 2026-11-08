package net.syrupstudios.fih.mixin;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.syrupstudios.fih.ai.FishingHookAccess;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin implements FishingHookAccess {
    @Shadow private Entity hookedIn;
    @Shadow private void setHookedEntity(Entity entity) {}

    @Unique private UUID fih$reservedFish;
    @Unique private final Map<UUID, Boolean> fih$ignoredFish = new HashMap<>();

    @Inject(method = "catchingFish", at = @At("HEAD"), cancellable = true)
    private void fih$disableRandomBites(BlockPos pos, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void fih$keepHookedFishAttached(CallbackInfo ci) {
        if (!(hookedIn instanceof AbstractFish fish)) return;
        FishingHook hook = (FishingHook) (Object) this;
        if (fish.isRemoved() || !fish.isAlive()) {
            if (!hook.level().isClientSide) setHookedEntity(null);
            return;
        }
        hook.setDeltaMovement(Vec3.ZERO);
        hook.setPos(fish.getX(), fish.getY(0.8), fish.getZ());
    }

    @Override
    @Unique
    public boolean fih$targets(AbstractFish fish) {
        FishingHook hook = (FishingHook) (Object) this;
        boolean ignored = fih$ignoredFish.computeIfAbsent(fish.getUUID(), id -> hook.getRandom().nextFloat() < 0.15F);
        BlockPos hookPos = hook.blockPosition();
        BlockPos waterPos = hook.level().getFluidState(hookPos).is(FluidTags.WATER) ? hookPos : hookPos.below();
        return hookedIn == null && hook.isInWater() && hook.level().getFluidState(waterPos).is(FluidTags.WATER)
                && fish.isAlive() && fish.isInWater() && fish.distanceToSqr(hook) <= 144
                && !ignored && (fih$reservedFish == null || fih$reservedFish.equals(fish.getUUID()));
    }

    @Override
    @Unique
    public boolean fih$reserve(AbstractFish fish) {
        if (!fih$targets(fish)) return false;
        if (fih$reservedFish == null) fih$reservedFish = fish.getUUID();
        return fih$reservedFish.equals(fish.getUUID());
    }

    @Override
    @Unique
    public void fih$release(AbstractFish fish) {
        if (fish.getUUID().equals(fih$reservedFish)) fih$reservedFish = null;
    }

    @Override
    @Unique
    public void fih$completeApproach(AbstractFish fish) {
        if (!fish.getUUID().equals(fih$reservedFish) || !fih$targets(fish)) return;
        setHookedEntity(fish);
        FishingHook hook = (FishingHook) (Object) this;
        hook.level().playSound(null, hook.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH,
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        fih$release(fish);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void fih$clearFishStateOnRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        fih$reservedFish = null;
        fih$ignoredFish.clear();
    }

    @Inject(method = "retrieve", at = @At("RETURN"))
    private void fih$keepReeledFish(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        if (!(hookedIn instanceof AbstractFish fish) || fish.isRemoved() || !fish.isAlive() || !(fish.level() instanceof ServerLevel level)) return;
        Player player = ((FishingHook) (Object) this).getPlayerOwner();
        if (player == null) return;

        ItemStack catchStack = fih$catchStack(fish);
        if (!player.getInventory().add(catchStack)) {
            level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), catchStack));
        }
        fish.discard();
    }

    private static ItemStack fih$catchStack(AbstractFish fish) {
        if (fish instanceof Cod) return new ItemStack(Items.COD);
        if (fish instanceof Salmon) return new ItemStack(Items.SALMON);
        if (fish instanceof Pufferfish) return new ItemStack(Items.PUFFERFISH);
        if (fish instanceof TropicalFish) return new ItemStack(Items.TROPICAL_FISH);
        return fish.getBucketItemStack();
    }
}
