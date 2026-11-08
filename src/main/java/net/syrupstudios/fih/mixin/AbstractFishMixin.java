package net.syrupstudios.fih.mixin;

import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.syrupstudios.fih.ai.FishApproachHookGoal;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFish.class)
public abstract class AbstractFishMixin {
    @Redirect(method = "registerGoals", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/EntitySelector;NO_SPECTATORS:Ljava/util/function/Predicate;"))
    private java.util.function.Predicate<Entity> fih$avoidCreativePlayers() {
        return EntitySelector.NO_CREATIVE_OR_SPECTATOR;
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void fih$addHookApproachGoal(CallbackInfo ci) {
        ((MobGoalSelectorAccess) this).fih$goalSelector().addGoal(3, new FishApproachHookGoal((AbstractFish) (Object) this));
    }
}
