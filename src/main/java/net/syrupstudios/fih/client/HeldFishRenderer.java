package net.syrupstudios.fih.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.syrupstudios.fih.ai.HeldFish;

public final class HeldFishRenderer {
    private static ClientLevel cachedLevel;
    private static AbstractFish cod, salmon, tropicalFish, pufferfish;
    private static AbstractFish renderingFish;

    private HeldFishRenderer() {}

    public static boolean isRendering(AbstractFish fish) {
        return renderingFish == fish;
    }

    public static boolean render(ItemStack stack, ItemDisplayContext context, boolean leftHand,
            PoseStack pose, MultiBufferSource buffers, int light) {
        if ((!context.firstPerson() && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                || !HeldFish.isFish(stack)) return false;

        AbstractFish fish = preview(stack);
        if (fish == null) return false;
        pose.pushPose();
        boolean firstPerson = context.firstPerson();
        float mirror = leftHand ? 1.0F : -1.0F;
        pose.translate(mirror * (firstPerson ? HeldFishPose.fishX : HeldFishThirdPersonPose.fishX),
                firstPerson ? HeldFishPose.fishY : HeldFishThirdPersonPose.fishY,
                firstPerson ? HeldFishPose.fishZ : HeldFishThirdPersonPose.fishZ);
        pose.mulPose(Axis.XP.rotationDegrees(firstPerson ? HeldFishPose.fishPitch : HeldFishThirdPersonPose.fishPitch));
        pose.mulPose(Axis.YP.rotationDegrees(mirror * (firstPerson ? HeldFishPose.fishYaw : HeldFishThirdPersonPose.fishYaw)));
        pose.mulPose(Axis.ZP.rotationDegrees(mirror * (firstPerson ? HeldFishPose.fishRoll : HeldFishThirdPersonPose.fishRoll)));
        pose.translate(0.0, -0.08, 0.0);
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        float scale = firstPerson ? HeldFishPose.fishScale : HeldFishThirdPersonPose.fishScale;
        pose.scale(scale, scale, scale);
        float mouthHeight = stack.is(Items.SALMON) ? 0.251F : 0.126F;
        float mouthForward = stack.is(Items.SALMON) ? 0.5875F
                : stack.is(Items.COD) ? 0.25F : stack.is(Items.PUFFERFISH) ? 0.09375F : 0.1875F;
        pose.translate(0.0, -mouthHeight, 0.4F - mouthForward);
        renderingFish = fish;
        try {
            EntityRenderer<? super AbstractFish> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(fish);
            renderer.render(fish, 0.0F, 0.0F, pose, buffers, light);
        } finally {
            renderingFish = null;
            pose.popPose();
        }
        return true;
    }

    private static AbstractFish preview(ItemStack stack) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        if (cachedLevel != level) {
            cachedLevel = level;
            cod = salmon = tropicalFish = pufferfish = null;
        }
        if (stack.is(Items.COD)) return cod != null ? cod : (cod = EntityType.COD.create(level));
        if (stack.is(Items.SALMON)) return salmon != null ? salmon : (salmon = EntityType.SALMON.create(level));
        if (stack.is(Items.TROPICAL_FISH)) {
            if (tropicalFish == null) tropicalFish = EntityType.TROPICAL_FISH.create(level);
            if (tropicalFish instanceof TropicalFish tropical) {
                CustomData data = stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY);
                CompoundTag saved = data.copyTag();
                if (!saved.contains(TropicalFish.BUCKET_VARIANT_TAG, Tag.TAG_INT)) {
                    saved = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                }
                int variant = saved.contains(TropicalFish.BUCKET_VARIANT_TAG, Tag.TAG_INT)
                        ? saved.getInt(TropicalFish.BUCKET_VARIANT_TAG)
                        : saved.contains("Variant", Tag.TAG_INT) ? saved.getInt("Variant")
                        : TropicalFish.COMMON_VARIANTS.getFirst().getPackedId();
                CompoundTag variantTag = new CompoundTag();
                variantTag.putInt(TropicalFish.BUCKET_VARIANT_TAG, variant);
                tropical.loadFromBucketTag(variantTag);
            }
            return tropicalFish;
        }
        if (stack.is(Items.PUFFERFISH)) return pufferfish != null ? pufferfish : (pufferfish = EntityType.PUFFERFISH.create(level));
        return null;
    }
}
