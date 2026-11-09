package net.syrupstudios.fih.loaders.neoforge;

//? if neoforge {
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.syrupstudios.fih.FishMeasurements;
import net.syrupstudios.fih.ModIdentity;
import net.syrupstudios.fih.Fih;

@Mod(ModIdentity.MOD_ID)
public final class NeoForgeEntrypoint {
    public NeoForgeEntrypoint(IEventBus modEventBus) {
        Fih.initialize("NeoForge");
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override protected Void prepare(ResourceManager manager, ProfilerFiller profiler) { return null; }
            @Override protected void apply(Void prepared, ResourceManager manager, ProfilerFiller profiler) {
                FishMeasurements.reload(manager);
            }
        }));
    }
}
//?}
