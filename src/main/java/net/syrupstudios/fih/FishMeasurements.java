package net.syrupstudios.fih;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.syrupstudios.fih.ai.FishMeasurementAccess;

public final class FishMeasurements {
    public record Range(float minLength, float maxLength, float minWeight, float maxWeight) {}

    private record Definition(Range range, ResourceLocation item, List<ResourceLocation> additionalDrops) {}
    private static final Map<ResourceLocation, Definition> DEFINITIONS = new HashMap<>();
    private static final Range DEFAULT = new Range(12, 45, 0.15F, 1.8F);

    public static void reload(ResourceManager manager) {
        Map<ResourceLocation, Definition> loaded = new HashMap<>();
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
                            ResourceLocation item = ResourceLocation.parse(GsonHelper.getAsString(json, "item", entity.toString()));
                            List<ResourceLocation> additionalDrops = new ArrayList<>();
                            if (json.has("additional_drops")) {
                                for (var entry : GsonHelper.getAsJsonArray(json, "additional_drops")) {
                                    additionalDrops.add(ResourceLocation.parse(entry.getAsString()));
                                }
                            }
                            loaded.put(entity, new Definition(new Range(minLength, maxLength, minWeight, maxWeight),
                                    item, List.copyOf(additionalDrops)));
                        } else {
                            Fih.LOGGER.warn("Ignoring invalid fish measurement range for {}", entity);
                        }
                    } catch (Exception e) {
                        Fih.LOGGER.warn("Could not load fish measurement range {}", file, e);
                    }
                });
        synchronized (DEFINITIONS) {
            DEFINITIONS.clear();
            DEFINITIONS.putAll(loaded);
        }
        Fih.LOGGER.info("Loaded {} fish measurement ranges", loaded.size());
    }

    public static Range range(ResourceLocation entity) {
        synchronized (DEFINITIONS) {
            Definition definition = DEFINITIONS.get(entity);
            return definition != null ? definition.range() : DEFAULT;
        }
    }

    public static float scaleForLength(float length, Range range) {
        return 0.65F + 0.7F * (length / Math.max(0.01F, range.maxLength()));
    }

    private static Definition definition(AbstractFish fish) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType());
        synchronized (DEFINITIONS) {
            Definition definition = DEFINITIONS.get(id);
            return definition != null ? definition : new Definition(DEFAULT, id, List.of());
        }
    }

    public static ItemStack catchItem(AbstractFish fish) {
        return BuiltInRegistries.ITEM.getOptional(definition(fish).item())
                .map(ItemStack::new).orElseGet(fish::getBucketItemStack);
    }

    public static boolean isFishDrop(AbstractFish fish, ItemStack stack) {
        Definition definition = definition(fish);
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return item.equals(definition.item()) || definition.additionalDrops().contains(item);
    }

    public static void copyToItem(AbstractFish fish, ItemStack stack) {
        Definition definition = definition(fish);
        boolean displayFish = BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(definition.item());
        if (displayFish) {
            ItemStack bucket = fish.getBucketItemStack();
            fish.saveToBucketTag(bucket);
            var bucketData = bucket.get(DataComponents.BUCKET_ENTITY_DATA);
            if (bucketData != null) stack.set(DataComponents.BUCKET_ENTITY_DATA, bucketData);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            CompoundTag fishData = data.getCompound("fih");
            fishData.putString("entity_type", BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType()).toString());
            fishData.putBoolean("display_fish", displayFish);
            if (fish instanceof FishMeasurementAccess measurements
                    && measurements.fih$lengthCm() > 0 && measurements.fih$weightKg() > 0) {
                fishData.putFloat("length_cm", measurements.fih$lengthCm());
                fishData.putFloat("weight_kg", measurements.fih$weightKg());
                fishData.putFloat("scale", fish.getScale());
            }
            data.put("fih", fishData);
        });
    }
}
