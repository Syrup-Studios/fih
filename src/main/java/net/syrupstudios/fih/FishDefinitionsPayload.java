package net.syrupstudios.fih;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FishDefinitionsPayload(Map<ResourceLocation, ResourceLocation> items) implements CustomPacketPayload {
    public static final Type<FishDefinitionsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fih", "fish_definitions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FishDefinitionsPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.items.size());
                payload.items.forEach((item, entity) -> {
                    buffer.writeResourceLocation(item);
                    buffer.writeResourceLocation(entity);
                });
            }, buffer -> {
                int size = buffer.readVarInt();
                if (size < 0 || size > 32768) throw new IllegalArgumentException("Invalid fish definition count: " + size);
                Map<ResourceLocation, ResourceLocation> items = new HashMap<>();
                for (int i = 0; i < size; i++) items.put(buffer.readResourceLocation(), buffer.readResourceLocation());
                return new FishDefinitionsPayload(Map.copyOf(items));
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
