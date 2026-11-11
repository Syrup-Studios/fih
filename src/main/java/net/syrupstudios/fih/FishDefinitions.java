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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.core.BlockPos;
import net.syrupstudios.fih.ai.FishMeasurementAccess;

public final class FishDefinitions {
    public record Range(float minLength, float maxLength, float minWeight, float maxWeight) {}

    public record SpawnConditions(List<ResourceLocation> biomes, Float minTemperature, Float maxTemperature,
            List<String> weather, Integer startTime, Integer endTime, List<Integer> moonPhases) {}

    private record Definition(Range range, ResourceLocation item, List<ResourceLocation> additionalDrops, SpawnConditions spawnConditions) {}
    private static final Map<ResourceLocation, Definition> DEFINITIONS = new HashMap<>();
    private static volatile Map<ResourceLocation, ResourceLocation> displayItems = Map.of();
    private static final Range DEFAULT = new Range(12, 45, 0.15F, 1.8F);

    public static void reload(ResourceManager manager) {
        Map<ResourceLocation, Definition> loaded = new HashMap<>();
        manager.listResources("fish", id -> id.getPath().endsWith(".json"))
                .forEach((file, resource) -> {
                    String path = file.getPath();
                    String entityPath = path.substring("fish/".length(), path.length() - 5);
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
                            SpawnConditions spawnConditions = json.has("spawn_conditions")
                                    ? parseSpawnConditions(GsonHelper.getAsJsonObject(json, "spawn_conditions")) : null;
                            loaded.put(entity, new Definition(new Range(minLength, maxLength, minWeight, maxWeight),
                                    item, List.copyOf(additionalDrops), spawnConditions));
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
        Map<ResourceLocation, ResourceLocation> items = new HashMap<>();
        loaded.forEach((entity, definition) -> items.put(definition.item(), entity));
        displayItems = Map.copyOf(items);
        Fih.LOGGER.info("Loaded {} fish measurement ranges", loaded.size());
    }

    public static Map<ResourceLocation, ResourceLocation> displayItems() { return displayItems; }

    public static void setDisplayItems(Map<ResourceLocation, ResourceLocation> items) {
        displayItems = Map.copyOf(items);
    }

    public static ResourceLocation entityForItem(ItemStack stack) {
        return displayItems.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
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

    private static SpawnConditions parseSpawnConditions(JsonObject json) {
        List<ResourceLocation> biomes = new ArrayList<>();
        if (json.has("biomes")) for (var value : GsonHelper.getAsJsonArray(json, "biomes")) biomes.add(ResourceLocation.parse(value.getAsString()));
        Float minTemperature = json.has("min_temperature") ? finiteTemperature(json, "min_temperature") : null;
        Float maxTemperature = json.has("max_temperature") ? finiteTemperature(json, "max_temperature") : null;
        if (minTemperature != null && maxTemperature != null && minTemperature > maxTemperature) throw new IllegalArgumentException("min_temperature exceeds max_temperature");
        List<String> weather = new ArrayList<>();
        if (json.has("weather")) for (var value : GsonHelper.getAsJsonArray(json, "weather")) {
            String condition = value.getAsString();
            if (!List.of("clear", "rain", "thunder").contains(condition)) throw new IllegalArgumentException("invalid weather: " + condition);
            weather.add(condition);
        }
        Integer start = null, end = null;
        if (json.has("time")) {
            JsonObject time = GsonHelper.getAsJsonObject(json, "time");
            if (!time.has("start") || !time.has("end")) throw new IllegalArgumentException("time requires start and end");
            start = timeValue(time, "start"); end = timeValue(time, "end");
        }
        List<Integer> moons = new ArrayList<>();
        if (json.has("moon_phases")) for (var value : GsonHelper.getAsJsonArray(json, "moon_phases")) {
            int phase = integerValue(value.getAsJsonPrimitive(), "moon phase");
            if (phase < 0 || phase > 7) throw new IllegalArgumentException("moon phase must be an integer from 0 to 7");
            moons.add(phase);
        }
        return new SpawnConditions(List.copyOf(biomes), minTemperature, maxTemperature, List.copyOf(weather), start, end, List.copyOf(moons));
    }

    private static float finiteTemperature(JsonObject json, String key) {
        float value = GsonHelper.getAsFloat(json, key);
        if (!Float.isFinite(value)) throw new IllegalArgumentException(key + " must be finite");
        return value;
    }

    private static int timeValue(JsonObject json, String key) {
        int value = integerValue(json.getAsJsonPrimitive(key), key);
        if (value < 0 || value > 23999) throw new IllegalArgumentException(key + " must be from 0 to 23999");
        return value;
    }

    private static int integerValue(com.google.gson.JsonPrimitive value, String key) {
        if (!value.isNumber()) throw new IllegalArgumentException(key + " must be an integer");
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number != Math.rint(number) || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        return (int) number;
    }

    public static boolean allowsSpawn(ResourceLocation entity, MobSpawnType spawnType, ServerLevelAccessor level, BlockPos pos) {
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) return true;
        Definition definition;
        synchronized (DEFINITIONS) { definition = DEFINITIONS.get(entity); }
        if (definition == null || definition.spawnConditions() == null) return true;
        SpawnConditions c = definition.spawnConditions();
        var biome = level.getBiome(pos);
        ResourceLocation biomeId = biome.unwrapKey().orElseThrow().location();
        if (!c.biomes().isEmpty() && !c.biomes().contains(biomeId)) return false;
        float temperature = biome.value().getBaseTemperature();
        if (c.minTemperature() != null && temperature < c.minTemperature() || c.maxTemperature() != null && temperature > c.maxTemperature()) return false;
        boolean raining = level.getLevel().isRaining(), thundering = level.getLevel().isThundering();
        if (!c.weather().isEmpty() && !(c.weather().contains("thunder") && thundering || c.weather().contains("rain") && raining && !thundering || c.weather().contains("clear") && !raining)) return false;
        if (c.startTime() != null) {
            long time = Math.floorMod(level.getLevel().getDayTime(), 24000);
            boolean inTime = c.startTime() <= c.endTime() ? time >= c.startTime() && time <= c.endTime() : time >= c.startTime() || time <= c.endTime();
            if (!inTime) return false;
        }
        return c.moonPhases().isEmpty() || c.moonPhases().contains(level.getLevel().getMoonPhase());
    }

    private static Definition definition(AbstractFish fish) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType());
        synchronized (DEFINITIONS) {
            Definition definition = DEFINITIONS.get(id);
            return definition != null ? definition : new Definition(DEFAULT, id, List.of(), null);
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
