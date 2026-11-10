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
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ModIdentity.MOD_ID)
public final class NeoForgeEntrypoint {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ModIdentity.MOD_ID);
    static {
        SOUNDS.register("reel_in", () -> FihSounds.REEL_IN);
        SOUNDS.register("line_out", () -> FihSounds.LINE_OUT);
    }
    public NeoForgeEntrypoint(IEventBus modEventBus) {
        Fih.initialize("NeoForge");
        SOUNDS.register(modEventBus);
        modEventBus.addListener((RegisterPayloadHandlersEvent event) ->
                event.registrar("1").playToClient(FishDefinitionsPayload.TYPE, FishDefinitionsPayload.CODEC,
                        (payload, context) -> FishDefinitions.setDisplayItems(payload.items())));
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override protected Void prepare(ResourceManager manager, ProfilerFiller profiler) { return null; }
            @Override protected void apply(Void prepared, ResourceManager manager, ProfilerFiller profiler) {
                FishDefinitions.reload(manager);
            }
        }));
    }
}
//?}
