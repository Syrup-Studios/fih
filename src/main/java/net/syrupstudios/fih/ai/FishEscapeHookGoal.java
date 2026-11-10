package net.syrupstudios.fih.ai;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

public final class FishEscapeHookGoal extends Goal {
    private final AbstractFish fish;
    private int nextPathTick;

    public FishEscapeHookGoal(AbstractFish fish) {
        this.fish = fish;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() { return hook() != null; }
    @Override public boolean canContinueToUse() { return hook() != null; }

    @Override public void tick() {
        FishingHook hook = hook();
        if (hook == null || !fish.isInWater() || fish.tickCount < nextPathTick) return;
        nextPathTick = fish.tickCount + 15;
        Vec3 player = hook.getPlayerOwner().position();
        for (int attempt = 0; attempt < 4; attempt++) {
            Vec3 target = DefaultRandomPos.getPosAway(fish, 8, 4, player);
            if (target != null && target.distanceToSqr(player) > fish.position().distanceToSqr(player)
                    && fish.level().getFluidState(BlockPos.containing(target)).is(FluidTags.WATER)
                    && fish.getNavigation().moveTo(target.x, target.y, target.z, 1.0)) return;
        }
    }

    @Override public void stop() { fish.getNavigation().stop(); }

    private FishingHook hook() {
        FishingHook hook = ((HookedFishAccess) fish).fih$fightingHook();
        return hook != null && hook.getPlayerOwner() != null && fish.isAlive() ? hook : null;
    }
}
