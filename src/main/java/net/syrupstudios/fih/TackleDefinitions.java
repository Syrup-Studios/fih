package net.syrupstudios.fih;

import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

/** Server-side datapack definitions for tackle items and their future properties. */
public final class TackleDefinitions {
    public enum Type {
        HOOK("hooks"), BOBBER("bobbers"), BAIT("bait");
        private final String directory;
        Type(String directory) { this.directory = directory; }
    }

    public record Definition(ResourceLocation id, ResourceLocation item, JsonObject data) {
        public Definition { data = data.deepCopy(); }
        @Override public JsonObject data() { return data.deepCopy(); }
    }

    private static volatile Map<Type, Map<ResourceLocation, Definition>> definitions = Map.of();
    private static volatile Map<Type, Set<ResourceLocation>> clientItems = Map.of();

    private TackleDefinitions() {}

    public static void reload(ResourceManager manager) {
        Map<Type, Map<ResourceLocation, Definition>> loaded = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
            String prefix = "tackle/" + type.directory + "/";
            Map<ResourceLocation, Definition> byItem = new HashMap<>();
            manager.listResources("tackle/" + type.directory, id -> id.getPath().endsWith(".json"))
                    .entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                        ResourceLocation file = entry.getKey();
                        String name = file.getPath().substring(prefix.length(), file.getPath().length() - 5);
                        try (var reader = entry.getValue().openAsReader()) {
                            JsonObject data = GsonHelper.parse(reader);
                            ResourceLocation item = ResourceLocation.parse(GsonHelper.getAsString(data, "item"));
                            if (!BuiltInRegistries.ITEM.containsKey(item)) {
                                Fih.LOGGER.warn("Ignoring tackle definition {}: item {} is not registered", file, item);
                                return;
                            }
                            Definition definition = new Definition(ResourceLocation.fromNamespaceAndPath(file.getNamespace(), name), item, data);
                            if (byItem.putIfAbsent(item, definition) != null) {
                                Fih.LOGGER.warn("Ignoring duplicate {} definition {} for item {}", type, file, item);
                            }
                        } catch (Exception exception) {
                            Fih.LOGGER.warn("Could not load tackle definition {}", file, exception);
                        }
                    });
            loaded.put(type, Map.copyOf(byItem));
        }
        definitions = Map.copyOf(loaded);
        Fih.LOGGER.info("Loaded tackle definitions: {} hooks, {} bobbers, {} bait",
                all(Type.HOOK).size(), all(Type.BOBBER).size(), all(Type.BAIT).size());
    }

    public static Definition find(Type type, ItemStack stack) {
        return definitions.getOrDefault(type, Map.of()).get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static List<Definition> all(Type type) {
        return List.copyOf(definitions.getOrDefault(type, Map.of()).values());
    }

    public static Map<Type, Set<ResourceLocation>> itemTypes() {
        Map<Type, Set<ResourceLocation>> result = new EnumMap<>(Type.class);
        for (Type type : Type.values()) result.put(type, Set.copyOf(definitions.getOrDefault(type, Map.of()).keySet()));
        return Map.copyOf(result);
    }

    public static void setClientItems(Map<Type, Set<ResourceLocation>> items) {
        Map<Type, Set<ResourceLocation>> result = new EnumMap<>(Type.class);
        for (Type type : Type.values()) result.put(type, Set.copyOf(items.getOrDefault(type, Set.of())));
        clientItems = Map.copyOf(result);
    }

    public static boolean allows(Type type, ItemStack stack, boolean client) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return client ? clientItems.getOrDefault(type, Set.of()).contains(id)
                : definitions.getOrDefault(type, Map.of()).containsKey(id);
    }
}
