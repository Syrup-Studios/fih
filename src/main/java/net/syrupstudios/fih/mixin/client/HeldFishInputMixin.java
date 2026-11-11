package net.syrupstudios.fih.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.syrupstudios.fih.ai.HeldFish;
import net.syrupstudios.fih.FihItems;
import net.minecraft.world.item.FishingRodItem;
import net.syrupstudios.fih.FihMenus;
import net.syrupstudios.fih.client.TackleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class HeldFishInputMixin {
    @Unique private boolean fih$hadFish;
    @Unique private boolean fih$readyToStow;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fih$registerTackleScreens(CallbackInfo ci) {
        MenuScreensAccess.fih$register(FihMenus.TACKLE, TackleScreen::new);
    }

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
        if (minecraft.player != null && minecraft.player.fishing == null && minecraft.player.isShiftKeyDown()
                && (minecraft.player.getMainHandItem().getItem() instanceof FishingRodItem
                    || minecraft.player.getOffhandItem().getItem() instanceof FishingRodItem)
                && minecraft.player.getInventory().contains(stack -> stack.is(FihItems.TACKLE_BOX))) {
            InteractionHand hand = minecraft.player.getMainHandItem().getItem() instanceof FishingRodItem
                    ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (minecraft.gameMode != null) minecraft.gameMode.useItem(minecraft.player, hand);
            ci.cancel();
            return;
        }
        if (minecraft.player != null && minecraft.player.getMainHandItem().is(FihItems.TACKLE_BOX)) return;
        if (minecraft.player == null || minecraft.gameMode == null || minecraft.player.fishing != null
                || !HeldFish.isFish(minecraft.player.getOffhandItem())) return;
        if (fih$readyToStow && !minecraft.player.isHandsBusy() && !minecraft.gameMode.isDestroying()) {
            fih$readyToStow = false;
            minecraft.gameMode.useItem(minecraft.player, InteractionHand.OFF_HAND);
        }
        ci.cancel();
    }
}
