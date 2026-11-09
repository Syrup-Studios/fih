package net.syrupstudios.fih.mixin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.syrupstudios.fih.FihGameRules;
import net.syrupstudios.fih.ai.FishingHookAccess;
import net.syrupstudios.fih.ai.FishMeasurementAccess;
import net.syrupstudios.fih.client.FishPitchAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    @Unique private double fih$lineLength = -1.0;
    @Unique private int fih$nextEscapePathTick;
    @Unique private float fih$previousTension;
    @Unique private float fih$currentTension;

    @Unique private static final double fih$MIN_LINE_LENGTH = 2.0;
    @Unique private static final EntityDataAccessor<Integer> fih$TENSION =
            SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void fih$defineTensionData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(fih$TENSION, 0);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void fih$smoothTension(CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;
        if (!hook.level().isClientSide) return;
        fih$previousTension = fih$currentTension;
        float target = hook.getEntityData().get(fih$TENSION) / 20.0F;
        fih$currentTension = Mth.lerp(0.3F, fih$currentTension, target);
    }

    @Override
    @Unique
    public float fih$visualTension(float partialTick) {
        return Mth.lerp(partialTick, fih$previousTension, fih$currentTension);
    }

    @Unique
    private void fih$syncTension(double stretch) {
        FishingHook hook = (FishingHook) (Object) this;
        int tension = stretch > 0.0 ? Math.round(Mth.clamp(0.5F + (float) stretch * 0.8F, 0.0F, 1.0F) * 20.0F) : 0;
        hook.getEntityData().set(fih$TENSION, tension);
    }

    @Inject(method = "catchingFish", at = @At("HEAD"), cancellable = true)
    private void fih$disableRandomBites(BlockPos pos, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void fih$keepHookedFishAttached(CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;
        if (hookedIn == null && !hook.level().isClientSide && fih$lineLength >= 0.0) {
            fih$pullEmptyHook(hook);
        }
        if (!(hookedIn instanceof AbstractFish fish)) return;
        if (fish.isRemoved() || !fish.isAlive()) {
            if (!hook.level().isClientSide) {
                setHookedEntity(null);
                fih$lineLength = -1.0;
                fih$syncTension(0.0);
            }
            return;
        }
        if (!hook.level().isClientSide) fih$tickFight(hook, fish);
        hook.setDeltaMovement(Vec3.ZERO);
        double yaw = Math.toRadians(fish.yBodyRot);
        double forward = (fish.getBbWidth() + hook.getBbWidth()) * 0.5;
        double pitch = hook.level().isClientSide && fish instanceof FishPitchAccess swimPitch
                ? Math.toRadians(swimPitch.fih$swimPitch(1.0F)) : 0.0;
        double horizontalForward = forward * Math.cos(pitch);
        hook.setPos(fish.getX() - Math.sin(yaw) * horizontalForward,
                fish.getY(0.5) + Math.sin(pitch) * forward,
                fish.getZ() + Math.cos(yaw) * horizontalForward);
    }

    @Unique
    private void fih$pullEmptyHook(FishingHook hook) {
        Player player = hook.getPlayerOwner();
        if (player == null) return;
        Vec3 towardPlayer = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0)
                .subtract(hook.position());
        double distance = towardPlayer.length();
        double stretch = distance - fih$lineLength;
        fih$syncTension(stretch);
        if (stretch > 0.0 && distance > 1.0E-6) {
            double pull = Math.min(0.12, 0.025 + stretch * 0.04);
            hook.setDeltaMovement(hook.getDeltaMovement().add(towardPlayer.scale(pull / distance)));
        }
    }

    @Unique
    private void fih$tickFight(FishingHook hook, AbstractFish fish) {
        Player player = hook.getPlayerOwner();
        if (player == null) return;

        Vec3 towardPlayer = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0)
                .subtract(fish.position());
        double distance = towardPlayer.length();
        if (fih$lineLength < 0.0) fih$lineLength = Math.max(fih$MIN_LINE_LENGTH, distance);
        if (distance < 1.0E-6) return;

        Vec3 direction = towardPlayer.scale(1.0 / distance);
        if (fish.isInWater()) {
            fish.setDeltaMovement(fish.getDeltaMovement().add(direction.scale(-0.012)));
            if (fish.tickCount >= fih$nextEscapePathTick) {
                fih$nextEscapePathTick = fish.tickCount + 30;
                Vec3 escape = DefaultRandomPos.getPosAway(fish, 8, 4, player.position());
                if (escape != null && fish.level().getFluidState(BlockPos.containing(escape)).is(FluidTags.WATER)) {
                    fish.getNavigation().moveTo(escape.x, escape.y, escape.z, 1.0);
                }
            }
        }

        double stretch = distance - fih$lineLength;
        fih$syncTension(stretch);
        if (stretch > 0.0) {
            // Like a lead, the line only pulls back when taut; cap the force to avoid violent launches.
            double pull = Math.min(0.12, 0.025 + stretch * 0.04);
            fish.setDeltaMovement(fish.getDeltaMovement().add(direction.scale(pull)));
        }
    }

    @Override
    @Unique
    public void fih$reelIn() {
        FishingHook hook = (FishingHook) (Object) this;
        if (hook.level().isClientSide) return;
        Entity target = hookedIn == null ? hook : hookedIn;
        if (target != hook && !(target instanceof AbstractFish)) return;
        Player player = hook.getPlayerOwner();
        if (player == null) return;
        if (fih$lineLength < 0.0) {
            fih$lineLength = Math.max(fih$MIN_LINE_LENGTH,
                    player.position().add(0.0, player.getBbHeight() * 0.5, 0.0).distanceTo(target.position()));
        }
        int reelAmount = Math.clamp(hook.level().getGameRules().getInt(FihGameRules.REEL_AMOUNT), 0, 4000);
        double weight = hookedIn instanceof FishMeasurementAccess measurements ? measurements.fih$weightKg() : 0.0;
        fih$lineLength = Math.max(fih$MIN_LINE_LENGTH, fih$lineLength - reelAmount / (1000.0 * (1.0 + weight * 0.15)));
    }

    @Override
    @Unique
    public boolean fih$targets(AbstractFish fish) {
        FishingHook hook = (FishingHook) (Object) this;
        boolean ignored = fih$ignoredFish.computeIfAbsent(fish.getUUID(), id -> hook.getRandom().nextInt(10000)
                < FihGameRules.chance(hook.level().getGameRules(), FihGameRules.IGNORE_CHANCE));
        return hookedIn == null && fih$hasWaterAtOrBelow(hook)
                && fish.isAlive() && fish.isInWater() && fish.distanceToSqr(hook) <= 144
                && !ignored && (fih$reservedFish == null || fih$reservedFish.equals(fish.getUUID()));
    }

    @Override
    @Unique
    public String fih$diagnostics(AbstractFish fish) {
        FishingHook hook = (FishingHook) (Object) this;
        return "hook=" + hook.getId() + " pos=" + hook.blockPosition().toShortString()
                + " distance=" + String.format(Locale.ROOT, "%.2f", fish.distanceTo(hook))
                + " hookInWater=" + hook.isInWater() + " waterAtOrBelow=" + fih$hasWaterAtOrBelow(hook)
                + " fishInWater=" + fish.isInWater() + " hookedEntity="
                + (hookedIn == null ? "none" : hookedIn.getId()) + " reservedFish=" + fih$reservedFish
                + " ignored=" + fih$ignoredFish.get(fish.getUUID());
    }

    @Unique
    private static boolean fih$hasWaterAtOrBelow(FishingHook hook) {
        BlockPos pos = hook.blockPosition();
        return hook.level().getFluidState(pos).is(FluidTags.WATER)
                || hook.level().getFluidState(pos.below()).is(FluidTags.WATER);
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
        FishingHook hook = (FishingHook) (Object) this;
        if (!hook.level().isClientSide && hook.isRemoved()) {
            Player owner = hook.getPlayerOwner();
            int cooldown = Math.max(0, hook.level().getGameRules().getInt(FihGameRules.REEL_COOLDOWN));
            if (owner != null && cooldown > 0) owner.getCooldowns().addCooldown(rod.getItem(), cooldown);
        }
        if (!(hookedIn instanceof AbstractFish fish) || fish.isRemoved() || !fish.isAlive()
                || !(fish.level() instanceof ServerLevel level)) return;
        Player player = ((FishingHook) (Object) this).getPlayerOwner();
        if (player == null) return;

        ItemStack catchStack = fih$catchStack(fish);
        if (!player.getInventory().add(catchStack)) {
            level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), catchStack));
        }
        fish.discard();
    }

    private static ItemStack fih$catchStack(AbstractFish fish) {
        ItemStack stack;
        if (fish instanceof Cod) stack = new ItemStack(Items.COD);
        else if (fish instanceof Salmon) stack = new ItemStack(Items.SALMON);
        else if (fish instanceof Pufferfish) stack = new ItemStack(Items.PUFFERFISH);
        else if (fish instanceof TropicalFish) stack = new ItemStack(Items.TROPICAL_FISH);
        else stack = fish.getBucketItemStack();
        if (fish instanceof FishMeasurementAccess measurements && measurements.fih$lengthCm() > 0) {
            CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            CompoundTag fishData = data.getCompound("fih");
            fishData.putFloat("length_cm", measurements.fih$lengthCm());
            fishData.putFloat("weight_kg", measurements.fih$weightKg());
            data.put("fih", fishData);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return stack;
    }
}
