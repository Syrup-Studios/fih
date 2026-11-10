package net.syrupstudios.fih.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;

/** Bounds in the coordinate space produced by the actual entity renderer. */
final class FishModelBounds implements VertexConsumer {
    private static final Map<EntityRenderer<?>, Map<String, Bounds>> CACHE = new WeakHashMap<>();
    record Bounds(float minY, float maxY, float minZ, float maxZ) {
        float centerY() { return (minY + maxY) * 0.5F; }
        float centerZ() { return (minZ + maxZ) * 0.5F; }
    }

    private float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
    private float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

    static Bounds get(AbstractFish fish, ItemStack stack, EntityRenderer<? super AbstractFish> renderer) {
        Map<String, Bounds> byVariant = CACHE.computeIfAbsent(renderer, ignored -> new HashMap<>());
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType()) + ":"
                + stack.get(DataComponents.BUCKET_ENTITY_DATA) + ":" + stack.get(DataComponents.CUSTOM_DATA);
        if (byVariant.size() >= 128 && !byVariant.containsKey(key)) byVariant.clear();
        return byVariant.computeIfAbsent(key, ignored -> measure(fish, renderer));
    }

    private static Bounds measure(AbstractFish fish, EntityRenderer<? super AbstractFish> renderer) {
        FishModelBounds vertices = new FishModelBounds();
        HeldFishRenderer.renderPreview(fish, renderer, new PoseStack(), type -> vertices, 0xF000F0);
        if (!Float.isFinite(vertices.minZ)) {
            float radius = fish.getBbWidth() * 0.5F;
            return new Bounds(0.0F, fish.getBbHeight(), -radius, radius);
        }
        return new Bounds(vertices.minY, vertices.maxY, vertices.minZ, vertices.maxZ);
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) {
        minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
        return this;
    }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer setUv(float u, float v) { return this; }
    @Override public VertexConsumer setUv1(int u, int v) { return this; }
    @Override public VertexConsumer setUv2(int u, int v) { return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
}
