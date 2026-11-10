package net.syrupstudios.fih.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.syrupstudios.fih.client.ReelSoundPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class ReelSoundRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void fih$renderReelSounds(DeltaTracker delta, boolean renderLevel, CallbackInfo ci) {
        ReelSoundPlayer.render(delta);
    }
}
