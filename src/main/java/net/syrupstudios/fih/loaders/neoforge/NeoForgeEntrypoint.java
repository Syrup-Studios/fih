package net.syrupstudios.fih.loaders.neoforge;

//? if neoforge {
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.syrupstudios.fih.ModIdentity;
import net.syrupstudios.fih.Fih;

@Mod(ModIdentity.MOD_ID)
public final class NeoForgeEntrypoint {
    public NeoForgeEntrypoint(IEventBus modEventBus) {
        Fih.initialize("NeoForge");
    }
}
//?}
