package net.syrupstudios.fih.ai;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FishApproachHookGoal extends Goal {
    private final AbstractFish fish;
    private FishingHook hook;
    private boolean biting;
    private boolean pathRequested;
    private int lungeTicks;

    public FishApproachHookGoal(AbstractFish fish) {
        this.fish = fish;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        if (!fish.isAlive() || fish.level().isClientSide) return false;
        FishingHook candidate = fish.level().getEntitiesOfClass(FishingHook.class, new AABB(fish.blockPosition()).inflate(12),
                this::isEligible)
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(fish), b.distanceToSqr(fish))).orElse(null);
        if (candidate == null) return false;

        hook = candidate;
        return true;
    }

    private boolean isEligible(FishingHook candidate) {
        return !candidate.isRemoved() && candidate.level() == fish.level() && candidate.distanceToSqr(fish) <= 144
                && candidate instanceof FishingHookAccess access && access.fih$targets(fish);
    }

    @Override
    public boolean canContinueToUse() {
        return hook != null && !hook.isRemoved() && fish.isAlive()
                && hook.level() == fish.level() && hook.distanceToSqr(fish) <= 256
                && ((FishingHookAccess) hook).fih$targets(fish)
                && (!biting || fish.hasLineOfSight(hook));
    }

    @Override
    public void tick() {
        if (hook == null) return;
        FishingHookAccess access = (FishingHookAccess) hook;
        var navigation = fish.getNavigation();
        double distance = fish.distanceToSqr(hook);
        fish.lookAt(hook, 10.0F, 10.0F);
        if (!biting && fish.hasLineOfSight(hook)
                && (distance <= 2.25 || (pathRequested && navigation.isDone() && distance <= 9))) {
            biting = access.fih$reserve(fish);
            if (biting) lungeTicks = distance <= 2.25 ? 20 : 40;
        }

        if (biting && fish.getBoundingBox().inflate(0.15).intersects(hook.getBoundingBox()) && fish.hasLineOfSight(hook)) {
            access.fih$completeApproach(fish);
            stop();
            return;
        }

        if (biting) {
            if (!fish.hasLineOfSight(hook) || --lungeTicks == 0) {
                stop();
                return;
            }
            navigation.stop();
            Vec3 direction = hook.position().subtract(fish.position());
            if (direction.lengthSqr() > 1.0E-6) {
                Vec3 velocity = fish.getDeltaMovement().scale(0.8).add(direction.normalize().scale(0.04));
                if (velocity.lengthSqr() > 0.0324) velocity = velocity.normalize().scale(0.18);
                fish.setDeltaMovement(velocity);
            }
            return;
        }

        double speed = distance < 4 ? 0.35 : 0.65;
        navigation.setSpeedModifier(speed);
        var target = hook.blockPosition();
        if (!hook.level().getFluidState(target).is(net.minecraft.tags.FluidTags.WATER)) target = target.below();
        if (navigation.isDone() || !navigation.getTargetPos().equals(target)) {
            navigation.moveTo(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, speed);
            pathRequested = true;
        }
    }

    @Override
    public void stop() {
        fish.getNavigation().stop();
        if (biting && hook != null) ((FishingHookAccess) hook).fih$release(fish);
        biting = false;
        pathRequested = false;
        lungeTicks = 0;
        hook = null;
    }
}
