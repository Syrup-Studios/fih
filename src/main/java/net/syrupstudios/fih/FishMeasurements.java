package net.syrupstudios.fih;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import com.google.gson.JsonObject;

public final class FishMeasurements {
    public record Range(float minLength, float maxLength, float minWeight, float maxWeight) {}

    private static final Map<ResourceLocation, Range> RANGES = new HashMap<>();
    private static final Range DEFAULT = new Range(12, 45, 0.15F, 1.8F);

    public static void reload(ResourceManager manager) {
        Map<ResourceLocation, Range> loaded = new HashMap<>();
        manager.listResources("fih/fish_measurements", id -> id.getPath().endsWith(".json"))
                .forEach((file, resource) -> {
                    String path = file.getPath();
                    String entityPath = path.substring("fih/fish_measurements/".length(), path.length() - 5);
                    int separator = entityPath.indexOf('/');
                    if (separator <= 0 || separator == entityPath.length() - 1) {
                        Fih.LOGGER.warn("Ignoring fish measurement resource with no entity namespace: {}", file);
                        return;
                    }
                    ResourceLocation entity = ResourceLocation.fromNamespaceAndPath(
                            entityPath.substring(0, separator), entityPath.substring(separator + 1));
                    try (var reader = resource.openAsReader()) {
                        JsonObject json = GsonHelper.parse(reader);
                        float minLength = GsonHelper.getAsFloat(json, "min_length_cm");
                        float maxLength = GsonHelper.getAsFloat(json, "max_length_cm");
                        float minWeight = GsonHelper.getAsFloat(json, "min_weight_kg");
                        float maxWeight = GsonHelper.getAsFloat(json, "max_weight_kg");
                        if (Float.isFinite(minLength) && Float.isFinite(maxLength) && Float.isFinite(minWeight)
                                && Float.isFinite(maxWeight) && minLength > 0 && maxLength >= minLength
                                && minWeight > 0 && maxWeight >= minWeight) {
                            loaded.put(entity, new Range(minLength, maxLength, minWeight, maxWeight));
                        } else {
                            Fih.LOGGER.warn("Ignoring invalid fish measurement range for {}", entity);
                        }
                    } catch (Exception e) {
                        Fih.LOGGER.warn("Could not load fish measurement range {}", file, e);
                    }
                });
        synchronized (RANGES) {
            RANGES.clear();
            RANGES.putAll(loaded);
        }
        Fih.LOGGER.info("Loaded {} fish measurement ranges", loaded.size());
    }

    public static Range range(ResourceLocation entity) {
        synchronized (RANGES) {
            return RANGES.getOrDefault(entity, DEFAULT);
        }
    }

}
