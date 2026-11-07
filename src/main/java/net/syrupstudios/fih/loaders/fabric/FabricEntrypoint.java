package net.syrupstudios.fih.loaders.fabric;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.syrupstudios.fih.Fih;

public final class FabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        Fih.initialize("Fabric");
    }
}
//?}
