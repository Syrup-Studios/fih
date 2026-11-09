package net.syrupstudios.fih.client;

import java.io.Reader;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.syrupstudios.fih.Fih;
import net.syrupstudios.fih.ModIdentity;

/** Draws the Blockbench bobber model with two-sided, single-quad planes. */
public final class BobberModelRenderer {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ModIdentity.MOD_ID, "models/entity/bobber.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ModIdentity.MOD_ID, "textures/entity/hook_and_bobber.png");
    private static ResourceManager loadedManager;
    private static JsonObject model;

    private BobberModelRenderer() {}

    public static void renderBobber(PoseStack pose, MultiBufferSource buffers, int light) {
        JsonObject definition = model();
        if (definition == null) return;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (var element : definition.getAsJsonArray("elements")) {
            JsonObject part = element.getAsJsonObject();
            JsonArray from = part.getAsJsonArray("from");
            JsonArray to = part.getAsJsonArray("to");
            float x0 = coordinate(from, 0), y0 = coordinate(from, 1), z0 = coordinate(from, 2);
            float x1 = coordinate(to, 0), y1 = coordinate(to, 1), z1 = coordinate(to, 2);
            pose.pushPose();
            if (part.has("rotation")) {
                JsonObject rotation = part.getAsJsonObject("rotation");
                JsonArray origin = rotation.getAsJsonArray("origin");
                float ox = coordinate(origin, 0), oy = coordinate(origin, 1), oz = coordinate(origin, 2);
                pose.translate(ox, oy, oz);
                float angle = rotation.get("angle").getAsFloat();
                switch (rotation.get("axis").getAsString()) {
                    case "x" -> pose.mulPose(Axis.XP.rotationDegrees(angle));
                    case "y" -> pose.mulPose(Axis.YP.rotationDegrees(angle));
                    case "z" -> pose.mulPose(Axis.ZP.rotationDegrees(angle));
                }
                pose.translate(-ox, -oy, -oz);
            }
            for (Map.Entry<String, com.google.gson.JsonElement> entry : part.getAsJsonObject("faces").entrySet()) {
                JsonObject face = entry.getValue().getAsJsonObject();
                if (!face.has("texture") || !face.get("texture").getAsString().equals("#0")) continue;
                String side = entry.getKey();
                // For a zero-thickness part, draw just one of the opposite faces; the render type is two-sided.
                if (x0 == x1 && side.equals("west") || z0 == z1 && side.equals("south")
                        || y0 == y1 && side.equals("down")) continue;
                float[][] points = switch (side) {
                    case "north" -> new float[][] {{x1,y0,z0},{x0,y0,z0},{x0,y1,z0},{x1,y1,z0}};
                    case "south" -> new float[][] {{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
                    case "west" -> new float[][] {{x0,y0,z0},{x0,y0,z1},{x0,y1,z1},{x0,y1,z0}};
                    case "east" -> new float[][] {{x1,y0,z1},{x1,y0,z0},{x1,y1,z0},{x1,y1,z1}};
                    case "up" -> new float[][] {{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}};
                    case "down" -> new float[][] {{x0,y0,z1},{x0,y0,z0},{x1,y0,z0},{x1,y0,z1}};
                    default -> null;
                };
                if (points == null || (x0 == x1 && (side.equals("north") || side.equals("south") || side.equals("up") || side.equals("down")))
                        || (z0 == z1 && (side.equals("east") || side.equals("west") || side.equals("up") || side.equals("down")))) continue;
                JsonArray uv = face.getAsJsonArray("uv");
                float u0 = uv.get(0).getAsFloat() / 16.0F, v0 = uv.get(1).getAsFloat() / 16.0F;
                float u1 = uv.get(2).getAsFloat() / 16.0F, v1 = uv.get(3).getAsFloat() / 16.0F;
                float[][] tex = {{u0,v1},{u1,v1},{u1,v0},{u0,v0}};
                int rotation = face.has("rotation") ? face.get("rotation").getAsInt() / 90 : 0;
                float[] normal = switch (side) {
                    case "north" -> new float[] {0,0,-1}; case "south" -> new float[] {0,0,1};
                    case "west" -> new float[] {-1,0,0}; case "east" -> new float[] {1,0,0};
                    case "up" -> new float[] {0,1,0}; default -> new float[] {0,-1,0};
                };
                for (int i = 0; i < 4; i++) {
                    float[] p = points[i], t = tex[Math.floorMod(i + rotation, 4)];
                    vertex(consumer, pose.last(), light, p[0], p[1], p[2], t[0], t[1], normal[0], normal[1], normal[2]);
                }
            }
            pose.popPose();
        }
    }

    public static void renderHook(PoseStack pose, MultiBufferSource buffers, int light, float cameraYaw) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        float halfWidth = 1.5F / 16.0F, halfHeight = 2.0F / 16.0F;
        float stemOffset = 1.0F / 16.0F;
        vertex(consumer, pose.last(), light, -halfWidth - stemOffset, -halfHeight, 0, 10F / 16, 1, 0, 0, 1);
        vertex(consumer, pose.last(), light, halfWidth - stemOffset, -halfHeight, 0, 1, 1, 0, 0, 1);
        vertex(consumer, pose.last(), light, halfWidth - stemOffset, halfHeight, 0, 1, 8F / 16, 0, 0, 1);
        vertex(consumer, pose.last(), light, -halfWidth - stemOffset, halfHeight, 0, 10F / 16, 8F / 16, 0, 0, 1);
        pose.popPose();
    }

    private static float coordinate(JsonArray array, int index) {
        return (array.get(index).getAsFloat() - 1.5F) / 16.0F;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light, float x, float y, float z,
            float u, float v, float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    private static JsonObject model() {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (manager != loadedManager) {
            loadedManager = manager;
            try (Reader reader = manager.openAsReader(MODEL)) {
                model = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception exception) {
                model = null;
                Fih.LOGGER.error("Could not load bobber model {}", MODEL, exception);
            }
        }
        return model;
    }
}
