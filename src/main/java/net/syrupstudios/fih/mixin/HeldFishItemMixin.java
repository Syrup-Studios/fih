package net.syrupstudios.fih.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.syrupstudios.fih.ai.HeldFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class HeldFishItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void fih$stowHeldFish(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (hand != InteractionHand.OFF_HAND || player.fishing != null || !HeldFish.isFish(player.getOffhandItem())) return;
        if (!level.isClientSide) HeldFish.stow(player);
        cir.setReturnValue(InteractionResultHolder.sidedSuccess(player.getOffhandItem(), level.isClientSide));
    }
}
