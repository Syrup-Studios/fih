package net.syrupstudios.fih.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.syrupstudios.fih.ai.FishingHookAccess;

public final class FishingRodMotion {
    private FishingRodMotion() {}

    public static void apply(PoseStack pose, Player player, float partialTick) {
        FishingHook hook = player.fishing;
        if (hook == null || !(hook.getHookedIn() instanceof AbstractFish)
                || !(hook instanceof FishingHookAccess access)) return;
        float tension = access.fih$visualTension(partialTick);
        pose.mulPose(Axis.XP.rotationDegrees(-12.0F * tension));
    }
}
