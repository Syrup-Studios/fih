package net.syrupstudios.fih.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.syrupstudios.fih.ai.HeldFish;
import net.syrupstudios.fih.FishDefinitions;

public final class HeldFishRenderer {
    private static ClientLevel cachedLevel;
    private static final Map<String, AbstractFish> previews = new HashMap<>();
    private static AbstractFish renderingFish;

    private HeldFishRenderer() {}

    public static boolean isRendering(AbstractFish fish) {
        return renderingFish == fish;
    }

    public static boolean render(ItemStack stack, ItemDisplayContext context, boolean leftHand,
            PoseStack pose, MultiBufferSource buffers, int light) {
        if (!context.firstPerson() && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) return false;

        AbstractFish fish = preview(stack);
        if (fish == null) return false;
        EntityRenderer<? super AbstractFish> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(fish);
        FishModelBounds.Bounds bounds = FishModelBounds.get(fish, renderer);
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
        pose.translate(0.0, 0.0, 0.4F);
        float fishSize = fishSize(stack, fish);
        pose.scale(fishSize, fishSize, fishSize);
        pose.translate(0.0, -bounds.centerY(), -bounds.maxZ());
        renderPreview(fish, renderer, pose, buffers, light);
        pose.popPose();
        return true;
    }

    public static boolean renderInFrame(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light) {
        AbstractFish fish = preview(stack);
        if (fish == null) return false;
        EntityRenderer<? super AbstractFish> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(fish);
        FishModelBounds.Bounds bounds = FishModelBounds.get(fish, renderer);
        pose.pushPose();
        pose.translate(0.0F, 0.0F, -0.0625F);
        float scale = 2.0F * fishSize(stack, fish);
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.translate(0.0F, -bounds.centerY(), -bounds.centerZ());
        renderPreview(fish, renderer, pose, buffers, light);
        pose.popPose();
        return true;
    }

    static void renderPreview(AbstractFish fish, EntityRenderer<? super AbstractFish> renderer,
            PoseStack pose, MultiBufferSource buffers, int light) {
        renderingFish = fish;
        try {
            renderer.render(fish, 0.0F, 0.0F, pose, buffers, light);
        } finally {
            renderingFish = null;
        }
    }

    private static float fishSize(ItemStack stack, AbstractFish fish) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("fih");
        float scale = data.getFloat("scale");
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            float length = data.getFloat("length_cm");
            if (!Float.isFinite(length) || length <= 0.0F) return 1.0F;
            scale = FishDefinitions.scaleForLength(length,
                    FishDefinitions.range(BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType())));
        }
        return net.minecraft.util.Mth.clamp(scale, 0.0625F, 16.0F);
    }

    private static AbstractFish preview(ItemStack stack) {
        ClientLevel level = Minecraft.getInstance().level;
        ResourceLocation typeId = HeldFish.fishType(stack);
        if (level == null || typeId == null) return null;
        if (cachedLevel != level) {
            cachedLevel = level;
            previews.clear();
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null);
        if (type == null) return null;
        CompoundTag bucketData = stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
        bucketData.remove("fih");
        String key = typeId + ":" + bucketData;
        AbstractFish fish = previews.get(key);
        if (fish != null) return fish;
        Entity entity = type.create(level);
        if (!(entity instanceof AbstractFish created)) return null;
        created.loadFromBucketTag(bucketData);
        if (previews.size() >= 128) previews.clear();
        previews.put(key, created);
        return created;
    }
}
