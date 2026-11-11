package net.syrupstudios.fih.loaders.fabric;

//? if fabric {
/*import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.syrupstudios.fih.FishDefinitions;
import net.syrupstudios.fih.FishDefinitionsPayload;
import net.syrupstudios.fih.TackleDefinitions;

public final class FabricClientEntrypoint implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FishDefinitionsPayload.TYPE,
                (payload, context) -> {
                    FishDefinitions.setDisplayItems(payload.items());
                    TackleDefinitions.setClientItems(payload.tackleItems());
                });
    }
}
*///?}
