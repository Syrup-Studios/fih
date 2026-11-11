package net.syrupstudios.fih.loaders.neoforge;

//? if neoforge {
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.syrupstudios.fih.FishDefinitions;
import net.syrupstudios.fih.FishDefinitionsPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.syrupstudios.fih.ModIdentity;
import net.syrupstudios.fih.Fih;
import net.syrupstudios.fih.FihSounds;
import net.syrupstudios.fih.FihItems;
import net.syrupstudios.fih.TackleDefinitions;
import net.syrupstudios.fih.FihMenus;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ModIdentity.MOD_ID)
public final class NeoForgeEntrypoint {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ModIdentity.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ModIdentity.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ModIdentity.MOD_ID);
    static {
        ITEMS.register("tackle_box", () -> FihItems.TACKLE_BOX);
        ITEMS.register("bone_hook", () -> FihItems.BONE_HOOK);
        ITEMS.register("iron_hook", () -> FihItems.IRON_HOOK);
        ITEMS.register("bobber", () -> FihItems.BOBBER);
        MENUS.register("tackle_storage", () -> FihMenus.TACKLE);
        SOUNDS.register("reel_in", () -> FihSounds.REEL_IN);
        SOUNDS.register("line_out", () -> FihSounds.LINE_OUT);
    }
    public NeoForgeEntrypoint(IEventBus modEventBus) {
        Fih.initialize("NeoForge");
        SOUNDS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        modEventBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                event.accept(FihItems.TACKLE_BOX);
                event.accept(FihItems.BONE_HOOK);
                event.accept(FihItems.IRON_HOOK);
                event.accept(FihItems.BOBBER);
            }
        });
        modEventBus.addListener((RegisterPayloadHandlersEvent event) ->
                event.registrar("1").playToClient(FishDefinitionsPayload.TYPE, FishDefinitionsPayload.CODEC,
                        (payload, context) -> {
                            FishDefinitions.setDisplayItems(payload.items());
                            TackleDefinitions.setClientItems(payload.tackleItems());
                        }));
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override protected Void prepare(ResourceManager manager, ProfilerFiller profiler) { return null; }
            @Override protected void apply(Void prepared, ResourceManager manager, ProfilerFiller profiler) {
                FishDefinitions.reload(manager);
                TackleDefinitions.reload(manager);
            }
        }));
    }
}
//?}
