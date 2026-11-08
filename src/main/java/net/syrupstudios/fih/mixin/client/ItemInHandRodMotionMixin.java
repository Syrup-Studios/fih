package net.syrupstudios.fih.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.syrupstudios.fih.client.FishingRodMotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRodMotionMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void fih$pullRodDown(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            boolean leftHand, PoseStack pose, MultiBufferSource buffer, int light, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity == minecraft.player && entity instanceof Player player && context.firstPerson()
                && stack.is(Items.FISHING_ROD)) {
            FishingRodMotion.apply(pose, player, minecraft.getTimer().getGameTimeDeltaPartialTick(false));
        }
    }
}
