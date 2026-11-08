package net.syrupstudios.fih.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererFovAccess {
    @Invoker("getFov")
    double fih$getFov(Camera camera, float partialTick, boolean useFovSetting);
}
