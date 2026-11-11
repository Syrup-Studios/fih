package net.syrupstudios.fih.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.syrupstudios.fih.FishDefinitions;
import net.syrupstudios.fih.FishDefinitionsPayload;
import net.syrupstudios.fih.TackleDefinitions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class FishDefinitionsSyncMixin {
    @Shadow public abstract java.util.List<ServerPlayer> getPlayers();

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void fih$syncJoiningPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        player.connection.send(new ClientboundCustomPayloadPacket(new FishDefinitionsPayload(FishDefinitions.displayItems(), TackleDefinitions.itemTypes())));
    }

    @Inject(method = "reloadResources", at = @At("TAIL"))
    private void fih$syncReload(CallbackInfo ci) {
        var packet = new ClientboundCustomPayloadPacket(new FishDefinitionsPayload(FishDefinitions.displayItems(), TackleDefinitions.itemTypes()));
        for (ServerPlayer player : getPlayers()) player.connection.send(packet);
    }
}
