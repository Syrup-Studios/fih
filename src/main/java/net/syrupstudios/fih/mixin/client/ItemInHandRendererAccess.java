package net.syrupstudios.fih.mixin.client;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccess {
    @Accessor("mainHandItem") ItemStack fih$mainHandItem();
    @Accessor("offHandItem") ItemStack fih$offHandItem();
    @Accessor("mainHandHeight") float fih$mainHandHeight();
    @Accessor("oMainHandHeight") float fih$oldMainHandHeight();
    @Accessor("offHandHeight") float fih$offHandHeight();
    @Accessor("oOffHandHeight") float fih$oldOffHandHeight();
    @Invoker("renderPlayerArm")
    void fih$renderPlayerArm(PoseStack pose, MultiBufferSource buffers, int light,
            float equipProgress, float swingProgress, HumanoidArm arm);
}
