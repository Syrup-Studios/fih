package net.syrupstudios.fih.mixin;

import java.util.stream.Stream;

import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSchoolingFish.class)
public abstract class SchoolingFishMixin {
    @Shadow private AbstractSchoolingFish leader;

    @ModifyVariable(method = "addFollowers", at = @At("HEAD"), argsOnly = true)
    private Stream<? extends AbstractSchoolingFish> fih$excludeLeadersFromFollowers(
            Stream<? extends AbstractSchoolingFish> followers) {
        return followers.filter(fish -> !fish.hasFollowers());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void fih$releaseLeaderWhenDead(CallbackInfo ci) {
        AbstractSchoolingFish fish = (AbstractSchoolingFish) (Object) this;
        if (!fish.isAlive() && leader != null) fish.stopFollowing();
    }

    @Inject(method = "stopFollowing", at = @At("HEAD"), cancellable = true)
    private void fih$skipRepeatedStopFollowing(CallbackInfo ci) {
        if (leader == null) ci.cancel();
    }
}
