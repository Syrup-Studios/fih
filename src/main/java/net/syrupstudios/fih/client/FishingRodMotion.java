package net.syrupstudios.fih.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.util.Mth;
import net.syrupstudios.fih.ai.CastingAccess;
import net.syrupstudios.fih.ai.FishingHookAccess;

public final class FishingRodMotion {
    private FishingRodMotion() {}

    public static void apply(PoseStack pose, Player player, float partialTick) {
        if (player instanceof CastingAccess casting && casting.fih$isCasting() && player.isUsingItem()) {
            float ticks = player.getUseItem().getUseDuration(player) - player.getUseItemRemainingTicks() + partialTick;
            float charge = ticks / 20.0F;
            float power = Mth.clamp((charge * charge + charge * 2.0F) / 3.0F, 0.0F, 1.0F);
            pose.mulPose(Axis.XP.rotationDegrees(18.0F * power));
            return;
        }
        FishingHook hook = player.fishing;
        if (hook == null || !(hook.getHookedIn() instanceof AbstractFish)
                || !(hook instanceof FishingHookAccess access)) return;
        float tension = access.fih$visualTension(partialTick);
        pose.mulPose(Axis.XP.rotationDegrees(-12.0F * tension));
    }
}
