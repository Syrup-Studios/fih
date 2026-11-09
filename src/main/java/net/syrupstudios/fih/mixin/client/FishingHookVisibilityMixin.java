package net.syrupstudios.fih.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FishingHookRenderer.class)
public abstract class FishingHookVisibilityMixin extends EntityRenderer<FishingHook> {
    protected FishingHookVisibilityMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(FishingHook hook, Frustum frustum, double x, double y, double z) {
        // Keep the line visible when the local player's float is outside the camera frustum.
        if (hook.getPlayerOwner() == Minecraft.getInstance().player) return hook.shouldRender(x, y, z);
        return super.shouldRender(hook, frustum, x, y, z);
    }
}
