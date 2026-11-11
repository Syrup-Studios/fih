package net.syrupstudios.fih;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.EnumMap;
import java.util.HashSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FishDefinitionsPayload(Map<ResourceLocation, ResourceLocation> items,
        Map<TackleDefinitions.Type, Set<ResourceLocation>> tackleItems) implements CustomPacketPayload {
    public static final Type<FishDefinitionsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fih", "fish_definitions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FishDefinitionsPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.items.size());
                payload.items.forEach((item, entity) -> {
                    buffer.writeResourceLocation(item);
                    buffer.writeResourceLocation(entity);
                });
                for (TackleDefinitions.Type type : TackleDefinitions.Type.values()) {
                    Set<ResourceLocation> entries = payload.tackleItems.getOrDefault(type, Set.of());
                    buffer.writeVarInt(entries.size());
                    entries.forEach(buffer::writeResourceLocation);
                }
            }, buffer -> {
                int size = buffer.readVarInt();
                if (size < 0 || size > 32768) throw new IllegalArgumentException("Invalid fish definition count: " + size);
                Map<ResourceLocation, ResourceLocation> items = new HashMap<>();
                for (int i = 0; i < size; i++) items.put(buffer.readResourceLocation(), buffer.readResourceLocation());
                Map<TackleDefinitions.Type, Set<ResourceLocation>> tackle = new EnumMap<>(TackleDefinitions.Type.class);
                for (TackleDefinitions.Type type : TackleDefinitions.Type.values()) {
                    int count = buffer.readVarInt();
                    if (count < 0 || count > 32768) throw new IllegalArgumentException("Invalid tackle count: " + count);
                    Set<ResourceLocation> entries = new HashSet<>();
                    for (int i = 0; i < count; i++) entries.add(buffer.readResourceLocation());
                    tackle.put(type, Set.copyOf(entries));
                }
                return new FishDefinitionsPayload(Map.copyOf(items), Map.copyOf(tackle));
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
