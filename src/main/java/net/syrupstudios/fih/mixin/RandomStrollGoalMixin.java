package net.syrupstudios.fih.mixin;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RandomStrollGoal.class)
public abstract class RandomStrollGoalMixin {
    @Redirect(method = "canUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/PathfinderMob;getNoActionTime()I"))
    private int fih$ignoreNoActionTimeForFish(PathfinderMob mob) {
        return mob instanceof AbstractFish ? 0 : mob.getNoActionTime();
    }
}
