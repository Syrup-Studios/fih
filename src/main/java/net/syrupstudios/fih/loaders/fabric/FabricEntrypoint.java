package net.syrupstudios.fih.loaders.fabric;

//? if fabric {
/*import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.syrupstudios.fih.FishDefinitions;
import net.syrupstudios.fih.FishDefinitionsPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.syrupstudios.fih.Fih;
import net.syrupstudios.fih.FihSounds;
import net.syrupstudios.fih.FihItems;
import net.syrupstudios.fih.TackleDefinitions;
import net.syrupstudios.fih.FihMenus;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class FabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        Fih.initialize("Fabric");
        Registry.register(BuiltInRegistries.ITEM, FihItems.TACKLE_BOX_ID, FihItems.TACKLE_BOX);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("fih", "bone_hook"), FihItems.BONE_HOOK);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("fih", "iron_hook"), FihItems.IRON_HOOK);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("fih", "bobber"), FihItems.BOBBER);
        Registry.register(BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath("fih", "tackle_storage"), FihMenus.TACKLE);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> {
                    entries.accept(FihItems.TACKLE_BOX);
                    entries.accept(FihItems.BONE_HOOK);
                    entries.accept(FihItems.IRON_HOOK);
                    entries.accept(FihItems.BOBBER);
                });
        Registry.register(BuiltInRegistries.SOUND_EVENT, FihSounds.REEL_IN_ID, FihSounds.REEL_IN);
        Registry.register(BuiltInRegistries.SOUND_EVENT, FihSounds.LINE_OUT_ID, FihSounds.LINE_OUT);
        PayloadTypeRegistry.playS2C().register(FishDefinitionsPayload.TYPE, FishDefinitionsPayload.CODEC);
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public ResourceLocation getFabricId() { return ResourceLocation.fromNamespaceAndPath("fih", "fish_definitions"); }
            @Override public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
                FishDefinitions.reload(manager);
                TackleDefinitions.reload(manager);
            }
        });
    }
}
*///?}
