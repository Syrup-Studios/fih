package net.syrupstudios.fih.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.syrupstudios.fih.ai.HeldFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class HeldFishInputMixin {
    @Unique private boolean fih$hadFish;
    @Unique private boolean fih$readyToStow;

    @Inject(method = "tick", at = @At("HEAD"))
    private void fih$trackFishUseKey(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        boolean holdingFish = minecraft.player != null && HeldFish.isFish(minecraft.player.getOffhandItem());
        boolean useDown = minecraft.options.keyUse.isDown();
        if (holdingFish && !fih$hadFish) fih$readyToStow = !useDown;
        if (!useDown) fih$readyToStow = true;
        fih$hadFish = holdingFish;
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void fih$useHeldFish(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.player == null || minecraft.gameMode == null || minecraft.player.fishing != null
                || !HeldFish.isFish(minecraft.player.getOffhandItem())) return;
        if (fih$readyToStow && !minecraft.player.isHandsBusy() && !minecraft.gameMode.isDestroying()) {
            fih$readyToStow = false;
            minecraft.gameMode.useItem(minecraft.player, InteractionHand.OFF_HAND);
        }
        ci.cancel();
    }
}
