package net.syrupstudios.fih.ai;

import java.util.Comparator;
import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.syrupstudios.fih.Fih;
import net.syrupstudios.fih.FihGameRules;

public final class FishApproachHookGoal extends Goal {
    private static final int HOOK_SEARCH_RADIUS = 12;
    private static final double HOOK_SEARCH_RADIUS_SQUARED = 144;
    private static final double CONTINUE_RADIUS_SQUARED = 256;
    private static final double CLOSE_LUNGE_DISTANCE_SQUARED = 2.25;
    private static final double PATH_LUNGE_DISTANCE_SQUARED = 9;
    private static final int WAYPOINT_ATTEMPTS = 6;
    private static final int CHANCE_SCALE = 10000;
    private static final double MAX_LUNGE_SPEED = 0.18;
    private static final double MAX_LUNGE_SPEED_SQUARED = 0.0324;

    private final AbstractFish fish;
    private FishingHook hook;
    private boolean biting;
    private boolean pathRequested;
    private int lungeTicks;
    private int nextPathLogTick;

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
        AABB searchArea = new AABB(fish.blockPosition()).inflate(HOOK_SEARCH_RADIUS);
        FishingHook candidate = fish.level().getEntitiesOfClass(FishingHook.class, searchArea, this::isEligible)
                .stream()
                .min(Comparator.comparingDouble(fish::distanceToSqr))
                .orElse(null);
        if (candidate == null) return false;

        hook = candidate;
        debug("selected hook={} distance={}", hook.getId(), Math.sqrt(hook.distanceToSqr(fish)));
        return true;
    }

    private boolean isEligible(FishingHook candidate) {
        return !candidate.isRemoved() && candidate.level() == fish.level()
                && candidate.distanceToSqr(fish) <= HOOK_SEARCH_RADIUS_SQUARED
                && candidate instanceof FishingHookAccess access && access.fih$targets(fish);
    }

    @Override
    public boolean canContinueToUse() {
        return hook != null && !hook.isRemoved() && fish.isAlive()
                && hook.level() == fish.level() && hook.distanceToSqr(fish) <= CONTINUE_RADIUS_SQUARED
                && ((FishingHookAccess) hook).fih$targets(fish)
                && (!biting || fish.hasLineOfSight(hook));
    }

    @Override
    public void tick() {
        if (hook == null) return;
        double distanceSquared = fish.distanceToSqr(hook);
        fish.lookAt(hook, 10.0F, 10.0F);

        tryStartLunge(distanceSquared);
        if (biting) {
            tickLunge(distanceSquared);
        } else if (fish.getNavigation().isDone()) {
            tryMoveToWaypoint(distanceSquared);
        }
    }

    private void tryStartLunge(double distanceSquared) {
        var navigation = fish.getNavigation();
        if (!biting && fish.hasLineOfSight(hook)
                && (distanceSquared <= CLOSE_LUNGE_DISTANCE_SQUARED
                || (pathRequested && navigation.isDone() && distanceSquared <= PATH_LUNGE_DISTANCE_SQUARED))) {
            biting = ((FishingHookAccess) hook).fih$reserve(fish);
            if (biting) lungeTicks = distanceSquared <= CLOSE_LUNGE_DISTANCE_SQUARED ? 20 : 40;
            debug("lunge reservation={} distance={} ticks={}", biting, Math.sqrt(distanceSquared), lungeTicks);
        }
    }

    private void tickLunge(double distanceSquared) {
        if (fish.getBoundingBox().inflate(0.15).intersects(hook.getBoundingBox()) && fish.hasLineOfSight(hook)) {
            debug("reached hook={} distance={}", hook.getId(), Math.sqrt(distanceSquared));
            ((FishingHookAccess) hook).fih$completeApproach(fish);
            stop();
            return;
        }

        if (!fish.hasLineOfSight(hook) || --lungeTicks == 0) {
            debug("lunge stopped distance={} lineOfSight={} ticksRemaining={}",
                    Math.sqrt(distanceSquared), fish.hasLineOfSight(hook), lungeTicks);
            stop();
            return;
        }

        fish.getNavigation().stop();
        Vec3 direction = hook.position().subtract(fish.position());
        if (direction.lengthSqr() > 1.0E-6) {
            Vec3 velocity = fish.getDeltaMovement().scale(0.8).add(direction.normalize().scale(0.04));
            if (velocity.lengthSqr() > MAX_LUNGE_SPEED_SQUARED) {
                velocity = velocity.normalize().scale(MAX_LUNGE_SPEED);
            }
            fish.setDeltaMovement(velocity);
        }
    }

    private void tryMoveToWaypoint(double distanceSquared) {
        int roll = fish.getRandom().nextInt(CHANCE_SCALE);
        int approach = FihGameRules.chance(fish.level().getGameRules(), FihGameRules.APPROACH_CHANCE);
        int retreat = FihGameRules.chance(fish.level().getGameRules(), FihGameRules.RETREAT_CHANCE);

        if (roll < approach) moveToWaypoint(false, distanceSquared);
        else if (roll - approach < retreat) moveToWaypoint(true, distanceSquared);
    }

    private void moveToWaypoint(boolean away, double currentDistanceSquared) {
        int radius = Math.clamp((int) Math.ceil(Math.sqrt(currentDistanceSquared)), 3, 8);
        int noCandidate = 0;
        int nonWater = 0;
        int noPath = 0;
        int wrongDirection = 0;
        int outOfRange = 0;
        int rejected = 0;
        for (int attempt = 0; attempt < WAYPOINT_ATTEMPTS; attempt++) {
            Vec3 waypoint = BehaviorUtils.getRandomSwimmablePos(fish, radius, Math.min(radius, 4));
            if (waypoint == null) {
                noCandidate++;
                continue;
            }
            BlockPos target = BlockPos.containing(waypoint);
            if (!fish.level().getFluidState(target).is(FluidTags.WATER)) {
                nonWater++;
                continue;
            }
            double targetDistanceSquared = Vec3.atCenterOf(target).distanceToSqr(hook.position());
            if (away ? targetDistanceSquared <= currentDistanceSquared : targetDistanceSquared >= currentDistanceSquared) {
                wrongDirection++;
                continue;
            }
            if (away && targetDistanceSquared > HOOK_SEARCH_RADIUS_SQUARED) {
                outOfRange++;
                continue;
            }
            Path path = fish.getNavigation().createPath(target, 0);
            if (path == null || !path.canReach() || path.getEndNode() == null) {
                noPath++;
                continue;
            }
            var end = path.getEndNode();
            Vec3 endpoint = new Vec3(end.x + 0.5, end.y + 0.5, end.z + 0.5);
            double endDistanceSquared = endpoint.distanceToSqr(hook.position());
            if (away ? endDistanceSquared <= currentDistanceSquared : endDistanceSquared >= currentDistanceSquared) {
                wrongDirection++;
                continue;
            }
            if (away && endDistanceSquared > HOOK_SEARCH_RADIUS_SQUARED) {
                outOfRange++;
                continue;
            }
            if (fish.getNavigation().moveTo(path, currentDistanceSquared < 4 ? 0.35 : 0.65)) {
                pathRequested = !away;
                debug("{} path target={} endpoint={} distanceBefore={} distanceAtEndpoint={}",
                        away ? "retreat" : "approach", target.toShortString(), endpoint,
                        Math.sqrt(currentDistanceSquared), Math.sqrt(endDistanceSquared));
                return;
            }
            rejected++;
        }
        if (fish.tickCount >= nextPathLogTick && debugEnabled()) {
            debug("{} path failed: noCandidate={} nonWater={} noReachablePath={} wrongDirection={} outOfRange={} moveRejected={} distance={}",
                    away ? "retreat" : "approach", noCandidate, nonWater, noPath, wrongDirection, outOfRange,
                    rejected, Math.sqrt(currentDistanceSquared));
            nextPathLogTick = fish.tickCount + 100;
        }
    }

    private boolean debugEnabled() {
        return !fish.level().isClientSide && fish.level().getGameRules().getInt(FihGameRules.DEBUG) > 0;
    }

    private void debug(String message, Object... args) {
        if (!debugEnabled()) return;
        Object[] values = new Object[args.length + 2];
        values[0] = fish.getId();
        values[1] = fish.getUUID();
        System.arraycopy(args, 0, values, 2, args.length);
        Fih.LOGGER.info("[fih-debug] fish={} uuid={} " + message, values);
    }

    @Override
    public void stop() {
        if (hook != null) debug("goal stopped hook={} biting={} distance={} lineOfSight={} navDone={}",
                hook.getId(), biting, fish.distanceTo(hook), fish.hasLineOfSight(hook), fish.getNavigation().isDone());
        fish.getNavigation().stop();
        if (biting && hook != null) ((FishingHookAccess) hook).fih$release(fish);
        biting = false;
        pathRequested = false;
        lungeTicks = 0;
        hook = null;
    }
}
