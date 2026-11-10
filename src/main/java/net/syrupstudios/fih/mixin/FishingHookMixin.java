package net.syrupstudios.fih.mixin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.syrupstudios.fih.FishDefinitions;
import net.syrupstudios.fih.FihGameRules;
import net.syrupstudios.fih.Fih;
import net.syrupstudios.fih.ai.FishingHookAccess;
import net.syrupstudios.fih.ai.FishMeasurementAccess;
import net.syrupstudios.fih.ai.HeldFish;
import net.syrupstudios.fih.ai.HookedFishAccess;
import net.syrupstudios.fih.client.FishPitchAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin implements FishingHookAccess {
    @Shadow private Entity hookedIn;
    @Shadow private void setHookedEntity(Entity entity) {}

    @Unique private UUID fih$reservedFish;
    @Unique private final Map<UUID, Boolean> fih$ignoredFish = new HashMap<>();
    @Unique private double fih$lineLength = -1.0;
    @Unique private double fih$sublineLength = -1.0;
    @Unique private int fih$configuredDepth = -1;
    @Unique private boolean fih$lineLocked;
    @Unique private float fih$previousTension;
    @Unique private float fih$currentTension;
    @Unique private float fih$previousLowerTension;
    @Unique private float fih$currentLowerTension;
    @Unique private Vec3 fih$subhookPosition;
    @Unique private Vec3 fih$subhookVelocity = Vec3.ZERO;
    @Unique private Vec3 fih$bobberBeforeTick;
    @Unique private boolean fih$enteredWater;
    @Unique private Vec3 fih$previousHookOffset = Vec3.ZERO;
    @Unique private Vec3 fih$currentHookOffset = Vec3.ZERO;
    @Unique private long fih$lineMotionTick = Long.MIN_VALUE;
    @Unique private float fih$previousLineMotion, fih$currentLineMotion;
    @Unique private float fih$reelSpeed;

    @Unique private static final double fih$MIN_LINE_LENGTH = 2.0;
    @Unique private static final double fih$SOUND_FULL_REEL_RATE = 0.07;
    @Unique private static final float fih$REEL_IMPULSE = 1.0F / 3.0F;
    @Unique private static final float fih$REEL_ACCELERATION = 1.0F / 30.0F;
    @Unique private static final float fih$REEL_DECELERATION = 1.0F / 20.0F;
    @Unique private static final EntityDataAccessor<Integer> fih$TENSION =
            SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Integer> fih$LOWER_TENSION =
            SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Float> fih$HOOK_X = SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.FLOAT);
    @Unique private static final EntityDataAccessor<Float> fih$HOOK_Y = SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.FLOAT);
    @Unique private static final EntityDataAccessor<Float> fih$HOOK_Z = SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.FLOAT);
    @Unique private static final EntityDataAccessor<Float> fih$LINE_MOTION = SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.FLOAT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void fih$defineTensionData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(fih$TENSION, 0);
        builder.define(fih$LOWER_TENSION, 0);
        builder.define(fih$HOOK_X, 0.0F);
        builder.define(fih$HOOK_Y, 0.0F);
        builder.define(fih$HOOK_Z, 0.0F);
        builder.define(fih$LINE_MOTION, 0.0F);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void fih$smoothTension(CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;
        fih$bobberBeforeTick = hook.position();
        if (!hook.level().isClientSide) {
            int depth = Math.clamp(hook.level().getGameRules().getInt(FihGameRules.HOOK_DEPTH), 0, 16);
            if (fih$configuredDepth < 0) {
                fih$sublineLength = depth;
            } else if (depth != fih$configuredDepth) {
                fih$sublineLength = Math.clamp(fih$sublineLength + depth - fih$configuredDepth, 0.0, depth);
            }
            fih$configuredDepth = depth;
            return;
        }
        fih$previousTension = fih$currentTension;
        fih$previousLineMotion = fih$currentLineMotion;
        fih$currentLineMotion = hook.getEntityData().get(fih$LINE_MOTION);
        float target = hook.getEntityData().get(fih$TENSION) / 20.0F;
        fih$currentTension = Mth.lerp(0.3F, fih$currentTension, target);
        fih$previousLowerTension = fih$currentLowerTension;
        fih$currentLowerTension = Mth.lerp(0.3F, fih$currentLowerTension,
                hook.getEntityData().get(fih$LOWER_TENSION) / 20.0F);
        fih$previousHookOffset = fih$currentHookOffset;
        fih$currentHookOffset = new Vec3(hook.getEntityData().get(fih$HOOK_X),
                hook.getEntityData().get(fih$HOOK_Y), hook.getEntityData().get(fih$HOOK_Z));
    }

    @Override
    @Unique
    public float fih$visualTension(float partialTick) {
        return Mth.lerp(partialTick, fih$previousTension, fih$currentTension);
    }

    @Override
    @Unique
    public float fih$visualLowerTension(float partialTick) {
        return Mth.lerp(partialTick, fih$previousLowerTension, fih$currentLowerTension);
    }

    @Override
    @Unique
    public float fih$lineSoundSpeed(float partialTick) {
        return Mth.lerp(partialTick, fih$previousLineMotion, fih$currentLineMotion);
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

    @ModifyConstant(method = "shouldStopFishing", constant = @Constant(doubleValue = 1024.0))
    private double fih$allowTautLineToPullBeforeBreaking(double vanillaDistanceSquared) {
        // Leave room for the elastic line to move the float before vanilla's distance cutoff.
        double maxDistance = Math.max(Math.sqrt(vanillaDistanceSquared), fih$lineLength + 8.0);
        return maxDistance * maxDistance;
    }

    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true)
    private void fih$renderLongCasts(double distanceSquared, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(distanceSquared < 256.0 * 256.0);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/FishingHook;setDeltaMovement(DDD)V"))
    private void fih$allowBobberToLeaveWater(FishingHook hook, double x, double y, double z) {
        Player player = hook.getPlayerOwner();
        if ((fih$isReeling(player) || fih$reelSpeed > 0.0F) && player != null && fih$lineLocked && fih$lineLength >= 0.0
                && hook.getDeltaMovement().y > 0.0) {
            Vec3 playerCenter = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
            if (playerCenter.distanceTo(hook.position()) > fih$lineLength) {
                y = Math.max(y, hook.getDeltaMovement().y);
            }
        }
        hook.setDeltaMovement(x, y, z);
    }

    @Unique
    private static boolean fih$isReeling(Player player) {
        return player != null && player.isUsingItem() && player.getUseItem().getItem() instanceof FishingRodItem;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void fih$keepHookedFishAttached(CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;
        if (hook.isRemoved()) return;
        if (hookedIn instanceof AbstractFish fish) {
            if (fish.isRemoved() || !fish.isAlive()) {
                if (!hook.level().isClientSide) {
                    setHookedEntity(null);
                    fih$syncTension(0.0);
                }
                return;
            }
            // Vanilla's hooked-entity state teleports the float onto the fish. Keep the float independent.
            if (fih$bobberBeforeTick != null && Math.abs(hook.getX() - fish.getX()) < 1.0E-6
                    && Math.abs(hook.getY() - fish.getY(0.8)) < 1.0E-6
                    && Math.abs(hook.getZ() - fish.getZ()) < 1.0E-6) {
                hook.setPos(fih$bobberBeforeTick);
                if (!hook.level().isClientSide) {
                    hook.move(MoverType.SELF, hook.getDeltaMovement());
                    hook.setDeltaMovement(hook.getDeltaMovement().scale(0.9));
                }
            }
            if (!hook.level().isClientSide) {
                ((HookedFishAccess) fish).fih$setFightingHook(hook);
                fih$tickFight(hook, fish);
            }
        }
        if (!hook.level().isClientSide) {
            fih$updateCastLine(hook);
            fih$coastReel(hook);
            if (hook.isRemoved()) return;
            fih$tickSubhook(hook);
            fih$payOutLine(hook);
            if (fih$lineMotionTick != hook.level().getGameTime()) hook.getEntityData().set(fih$LINE_MOTION, 0.0F);
            if (fih$lineLength >= 0.0) fih$applyRodLineTension(hook);
        }
    }

    @Unique
    private void fih$payOutLine(FishingHook hook) {
        Player player = hook.getPlayerOwner();
        if (player == null || !(hookedIn instanceof AbstractFish fish) || !fish.isAlive()) return;
        Vec3 playerCenter = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        Vec3 mouth = fih$fishMouth(fish, 1.0F);
        Vec3 away = mouth.subtract(playerCenter);
        double distance = away.length();
        double rodDistance = hook.position().distanceTo(playerCenter);
        double lowerDistance = mouth.distanceTo(hook.position());
        double stretch = rodDistance + lowerDistance - Math.max(0.0, fih$lineLength) - Math.max(0.0, fih$sublineLength);
        double outwardSpeed = distance > 1.0E-6
                ? fish.getDeltaMovement().subtract(player.getDeltaMovement()).dot(away.scale(1.0 / distance)) : 0.0;
        String reason;
        double payout = 0.0;
        if (fih$isReeling(player)) reason = "reeling";
        else if (fih$reelSpeed > 0.0F) reason = "reel_coasting";
        else if (!fih$lineLocked || fih$lineLength < 0.0 || fih$sublineLength < 0.0) reason = "line_not_ready";
        else if (fih$lineLength >= 128.0) reason = "line_limit";
        else if (stretch <= 0.0) reason = "slack";
        else if (outwardSpeed <= 0.0) reason = "not_moving_away";
        else {
            payout = Math.min(stretch, 128.0 - fih$lineLength);
            reason = "paying_out";
        }
        fih$lineLength += payout;
        if (payout > 0.0) fih$recordLineMovement(hook, -payout / 0.12);
        if (hook.tickCount % 20 == 0 && hook.level().getGameRules().getInt(FihGameRules.DEBUG) > 0) {
            Fih.LOGGER.info("[fih-debug] payout hook={} fish={} reason={} rodDistance={} lowerDistance={} rodLine={} lowerLine={} combinedStretch={} outwardSpeed={} payout={} reelSpeed={} fishVelocity={} navTarget={}",
                    hook.getId(), fish.getId(), reason, rodDistance, lowerDistance, fih$lineLength, fih$sublineLength,
                    stretch, outwardSpeed, payout, fih$reelSpeed, fish.getDeltaMovement(), fish.getNavigation().getTargetPos());
        }
    }

    @Unique
    private void fih$recordLineMovement(FishingHook hook, double distance) {
        fih$lineMotionTick = hook.level().getGameTime();
        hook.getEntityData().set(fih$LINE_MOTION, (float) distance);
    }

    @Unique
    private void fih$updateCastLine(FishingHook hook) {
        Player player = hook.getPlayerOwner();
        if (player == null || fih$lineLocked) return;
        Vec3 playerCenter = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        double distance = playerCenter.distanceTo(hook.position());
        // Pay line out while the float is in flight, then keep that length until the player reels.
        fih$lineLength = Math.max(Math.max(fih$lineLength, fih$MIN_LINE_LENGTH), distance);
        if (fih$hasWaterAtOrBelow(hook) || hook.onGround() || hookedIn != null) fih$lineLocked = true;
    }

    @Unique
    private Vec3 fih$fishMouth(AbstractFish fish, float partialTick) {
        FishingHook hook = (FishingHook) (Object) this;
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, fish.yBodyRotO, fish.yBodyRot));
        double forward = (fish.getBbWidth() + hook.getBbWidth()) * 0.5;
        double pitch = hook.level().isClientSide && fish instanceof FishPitchAccess swimPitch
                ? Math.toRadians(swimPitch.fih$swimPitch(partialTick)) : 0.0;
        double horizontalForward = forward * Math.cos(pitch);
        return fish.getPosition(partialTick).add(-Math.sin(yaw) * horizontalForward,
                fish.getBbHeight() * 0.5 + Math.sin(pitch) * forward, Math.cos(yaw) * horizontalForward);
    }

    @Override
    @Unique
    public Vec3 fih$hookPosition() {
        FishingHook hook = (FishingHook) (Object) this;
        if (hookedIn instanceof AbstractFish fish && fish.isAlive()) {
            return fih$fishMouth(fish, 1.0F);
        }
        if (!hook.level().isClientSide) return fih$subhookPosition == null ? hook.position() : fih$subhookPosition;
        return hook.position().add(hook.getEntityData().get(fih$HOOK_X),
                hook.getEntityData().get(fih$HOOK_Y), hook.getEntityData().get(fih$HOOK_Z));
    }

    @Override
    @Unique
    public Vec3 fih$renderHookPosition(float partialTick) {
        FishingHook hook = (FishingHook) (Object) this;
        if (hookedIn instanceof AbstractFish fish && fish.isAlive()) {
            return fih$fishMouth(fish, partialTick);
        }
        Vec3 offset = fih$previousHookOffset.lerp(fih$currentHookOffset, partialTick);
        return hook.getPosition(partialTick).add(offset);
    }

    @Unique
    private void fih$tickSubhook(FishingHook hook) {
        Vec3 bobber = hook.position();
        double maxLength = Math.max(0.0, fih$sublineLength);
        if (fih$subhookPosition == null) fih$subhookPosition = bobber;
        if (fih$hasWaterAtOrBelow(hook)) fih$enteredWater = true;

        if (hookedIn instanceof AbstractFish fish && fish.isAlive()) {
            fih$subhookPosition = fih$fishMouth(fish, 1.0F);
            fih$subhookVelocity = fish.getDeltaMovement();
        } else if (!fih$enteredWater) {
            // Keep the hook with the float during the cast, before the line enters the water.
            fih$subhookPosition = bobber;
            fih$subhookVelocity = hook.getDeltaMovement();
        } else {
            boolean inWater = hook.level().getFluidState(BlockPos.containing(fih$subhookPosition)).is(FluidTags.WATER);
            fih$subhookVelocity = fih$subhookVelocity.scale(inWater ? 0.86 : 0.98)
                    .add(0.0, inWater ? -0.015 : -0.03, 0.0);
            Vec3 next = fih$subhookPosition.add(fih$subhookVelocity);
            var hit = hook.level().clip(new ClipContext(fih$subhookPosition, next,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, hook));
            if (hit.getType() != HitResult.Type.MISS) {
                next = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.01));
                fih$subhookVelocity = Vec3.ZERO;
            }
            fih$subhookPosition = next;
        }

        Vec3 floatToHook = fih$subhookPosition.subtract(bobber);
        double distance = floatToHook.length();
        boolean freeSpool = hookedIn instanceof AbstractFish && !fih$isReeling(hook.getPlayerOwner()) && fih$reelSpeed <= 0.0F;
        Player owner = hook.getPlayerOwner();
        if (freeSpool && owner != null && fih$lineLength >= 0.0) {
            Vec3 playerCenter = owner.position().add(0.0, owner.getBbHeight() * 0.5, 0.0);
            maxLength = Math.max(maxLength, fih$lineLength + maxLength - bobber.distanceTo(playerCenter));
        }
        double stretch = distance - maxLength;
        float lowerTension = maxLength > 1.0E-6
                ? Mth.clamp((float) ((distance / maxLength - 0.85) / 0.15), 0.0F, 1.0F)
                : distance > 1.0E-6 ? 1.0F : 0.0F;
        hook.getEntityData().set(fih$LOWER_TENSION, Math.round(lowerTension * 20.0F));
        if (stretch > 0.0 && distance > 1.0E-6) {
            Vec3 direction = floatToHook.scale(1.0 / distance);
            double pull = Math.min(0.12, 0.02 + stretch * 0.05);
            hook.setDeltaMovement(hook.getDeltaMovement().add(direction.scale(pull)));
            if (hookedIn instanceof AbstractFish fish && fish.isAlive()) {
                if (!freeSpool) fish.setDeltaMovement(fish.getDeltaMovement().add(direction.scale(-pull)));
            } else {
                fih$subhookPosition = bobber.add(direction.scale(maxLength));
                double outwardSpeed = fih$subhookVelocity.dot(direction);
                if (outwardSpeed > 0.0) fih$subhookVelocity = fih$subhookVelocity.subtract(direction.scale(outwardSpeed));
            }
        }
        Vec3 offset = fih$subhookPosition.subtract(bobber);
        hook.getEntityData().set(fih$HOOK_X, (float) offset.x);
        hook.getEntityData().set(fih$HOOK_Y, (float) offset.y);
        hook.getEntityData().set(fih$HOOK_Z, (float) offset.z);
    }

    @Unique
    private void fih$applyRodLineTension(FishingHook hook) {
        Player player = hook.getPlayerOwner();
        if (player == null) return;
        Vec3 towardPlayer = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0)
                .subtract(hook.position());
        double distance = towardPlayer.length();
        boolean freeSpool = hookedIn instanceof AbstractFish && !fih$isReeling(player) && fih$reelSpeed <= 0.0F;
        double allowedLength = freeSpool && fih$subhookPosition != null
                ? Math.max(0.0, fih$lineLength + fih$sublineLength - hook.position().distanceTo(fih$subhookPosition))
                : fih$lineLength;
        double stretch = distance - allowedLength;
        fih$syncTension(stretch);
        if (stretch > 0.0 && distance > 1.0E-6) {
            double pull = Math.min(0.12, 0.025 + stretch * 0.04);
            if (freeSpool) pull = Math.min(0.02, 0.005 + stretch * 0.02);
            if (hookedIn instanceof AbstractFish && (fih$isReeling(player) || fih$reelSpeed > 0.0F) && towardPlayer.y > 0.0) {
                // A fighting fish can pull the lower line downward at 0.12 per tick. Give the
                // actively reeled rod enough force to lift both the float and fish against it.
                pull = Math.min(0.32, 0.16 + stretch * 0.08);
            }
            hook.setDeltaMovement(hook.getDeltaMovement().add(towardPlayer.scale(pull / distance)));
            if (fih$isReeling(player) && hook.tickCount % 40 == 0
                    && hook.level().getGameRules().getInt(FihGameRules.DEBUG) > 0) {
                Fih.LOGGER.info("[fih-debug] reeling hook={} fish={} rodLine={} lowerLine={} stretch={} pull={} velocity={} playerY={} bobberY={}",
                        hook.getId(), hookedIn == null ? "none" : hookedIn.getId(), fih$lineLength,
                        fih$sublineLength, stretch, pull, hook.getDeltaMovement(), player.getY(), hook.getY());
            }
        }
    }

    @Unique
    private void fih$tickFight(FishingHook hook, AbstractFish fish) {
        Player player = hook.getPlayerOwner();
        if (player == null) return;

        Vec3 towardPlayer = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0)
                .subtract(fish.position());
        double distance = towardPlayer.length();
        if (distance < 1.0E-6) return;

        Vec3 direction = towardPlayer.scale(1.0 / distance);
        if (fish.isInWater()) {
            fish.setDeltaMovement(fish.getDeltaMovement().add(direction.scale(-0.035)));
        }

    }

    @Override
    @Unique
    public void fih$startReel() {
        if (hookedIn == null || hookedIn instanceof AbstractFish) {
            fih$reelSpeed = Math.min(1.0F, fih$reelSpeed + fih$REEL_IMPULSE);
        }
    }

    @Override
    @Unique
    public void fih$reelIn() {
        FishingHook hook = (FishingHook) (Object) this;
        if (hook.level().isClientSide) return;
        if (hookedIn != null && !(hookedIn instanceof AbstractFish)) return;
        Player player = hook.getPlayerOwner();
        if (player == null) return;
        fih$reelSpeed = Math.min(1.0F, fih$reelSpeed + fih$REEL_ACCELERATION);
        fih$reelStep(hook, player, fih$reelSpeed);
    }

    @Unique
    private void fih$coastReel(FishingHook hook) {
        Player player = hook.getPlayerOwner();
        if (player == null || fih$isReeling(player) || fih$reelSpeed <= 0.0F) return;
        if (hookedIn != null && !(hookedIn instanceof AbstractFish)) {
            fih$reelSpeed = 0.0F;
            return;
        }
        fih$reelSpeed = Math.max(0.0F, fih$reelSpeed - fih$REEL_DECELERATION);
        if (fih$reelSpeed <= 0.0F) return;
        fih$reelStep(hook, player, fih$reelSpeed);
        double distanceSquared = hookedIn == null ? fih$hookPosition().distanceToSqr(player.position())
                : hookedIn.distanceToSqr(player);
        if (distanceSquared > 6.25) return;
        InteractionHand hand = player.getMainHandItem().getItem() instanceof FishingRodItem
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack rod = player.getItemInHand(hand);
        if (!(rod.getItem() instanceof FishingRodItem)) return;
        int damage = hook.retrieve(rod);
        rod.hurtAndBreak(damage, player, LivingEntity.getSlotForHand(hand));
        hook.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    @Unique
    private void fih$reelStep(FishingHook hook, Player player, float speed) {
        if (fih$lineLength < 0.0) {
            fih$lineLength = Math.max(fih$MIN_LINE_LENGTH,
                    player.position().add(0.0, player.getBbHeight() * 0.5, 0.0).distanceTo(hook.position()));
        }
        fih$lineLocked = true;
        if (fih$sublineLength < 0.0) {
            fih$sublineLength = Math.clamp(hook.level().getGameRules().getInt(FihGameRules.HOOK_DEPTH), 0, 16);
            fih$configuredDepth = (int) fih$sublineLength;
        }
        int reelAmount = Math.clamp(hook.level().getGameRules().getInt(FihGameRules.REEL_AMOUNT), 0, 4000);
        double weight = hookedIn instanceof FishMeasurementAccess measurements ? measurements.fih$weightKg() : 0.0;
        double amount = speed * reelAmount / (1000.0 * (1.0 + weight * 0.15));
        double mainLineReeled = Math.min(amount, Math.max(0.0, fih$lineLength - fih$MIN_LINE_LENGTH));
        fih$lineLength -= mainLineReeled;
        double lowerBefore = fih$sublineLength;
        fih$sublineLength = Math.max(0.0, fih$sublineLength - (amount - mainLineReeled));
        double reeled = mainLineReeled + lowerBefore - fih$sublineLength;
        fih$recordLineMovement(hook, reeled / fih$SOUND_FULL_REEL_RATE);
    }

    @Override
    @Unique
    public boolean fih$targets(AbstractFish fish) {
        FishingHook hook = (FishingHook) (Object) this;
        boolean ignored = fih$ignoredFish.computeIfAbsent(fish.getUUID(), id -> hook.getRandom().nextInt(10000)
                < FihGameRules.chance(hook.level().getGameRules(), FihGameRules.IGNORE_CHANCE));
        return hookedIn == null && fih$hasWaterAtOrBelow(hook)
                && fish.isAlive() && fish.isInWater() && fish.position().distanceToSqr(fih$hookPosition()) <= 144
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
                || !(fish.level() instanceof ServerLevel)) return;
        Player player = ((FishingHook) (Object) this).getPlayerOwner();
        if (player == null) return;

        ItemStack catchStack = fih$catchStack(fish);
        HeldFish.holdCatch(player, catchStack);
        fish.discard();
    }

    private static ItemStack fih$catchStack(AbstractFish fish) {
        ItemStack stack = FishDefinitions.catchItem(fish);
        FishDefinitions.copyToItem(fish, stack);
        return stack;
    }
}
