package net.syrupstudios.fih.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.projectile.FishingHook;
import net.syrupstudios.fih.Fih;
import net.syrupstudios.fih.FihGameRules;
import net.syrupstudios.fih.ai.FishApproachHookGoal;
import net.syrupstudios.fih.ai.FishEscapeHookGoal;
import net.syrupstudios.fih.ai.HookedFishAccess;
import net.syrupstudios.fih.ai.FishingHookAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFish.class)
public abstract class AbstractFishMixin implements HookedFishAccess {
    @Unique private FishingHook fih$fightingHook;

    @Override public void fih$setFightingHook(FishingHook hook) { fih$fightingHook = hook; }
    @Override public FishingHook fih$fightingHook() {
        if (fih$fightingHook != null && (fih$fightingHook.isRemoved()
                || fih$fightingHook.getHookedIn() != (Object) this)) fih$fightingHook = null;
        return fih$fightingHook;
    }
    @Redirect(method = "registerGoals", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/EntitySelector;NO_SPECTATORS:Ljava/util/function/Predicate;"))
    private java.util.function.Predicate<Entity> fih$avoidCreativePlayers() {
        return EntitySelector.NO_CREATIVE_OR_SPECTATOR;
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void fih$addHookApproachGoal(CallbackInfo ci) {
        var goals = ((MobGoalSelectorAccess) this).fih$goalSelector();
        goals.addGoal(1, new FishEscapeHookGoal((AbstractFish) (Object) this));
        goals.addGoal(3, new FishApproachHookGoal((AbstractFish) (Object) this));
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void fih$logFishStatus(CallbackInfo ci) {
        AbstractFish fish = (AbstractFish) (Object) this;
        if (fish.level().isClientSide || fish.tickCount % 100 != 0
                || fish.level().getGameRules().getInt(FihGameRules.DEBUG) <= 0) return;
        var hooks = fish.level().getEntitiesOfClass(FishingHook.class, fish.getBoundingBox().inflate(16));
        if (hooks.isEmpty()) return;
        var runningGoals = ((MobGoalSelectorAccess) this).fih$goalSelector().getAvailableGoals().stream()
                .filter(goal -> goal.isRunning())
                .map(goal -> goal.getPriority() + ":" + goal.getGoal().getClass().getSimpleName())
                .toList();
        for (FishingHook hook : hooks) {
            if (!(hook instanceof FishingHookAccess access)) continue;
            Fih.LOGGER.info("[fih-debug] fish={} uuid={} type={} pos={} alive={} navDone={} navTarget={} activeGoals={} {}",
                    fish.getId(), fish.getUUID(), fish.getType(), fish.blockPosition().toShortString(), fish.isAlive(),
                    fish.getNavigation().isDone(), fish.getNavigation().getTargetPos(), runningGoals, access.fih$diagnostics(fish));
        }
    }
}
