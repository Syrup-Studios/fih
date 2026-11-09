package net.syrupstudios.fih.mixin;

import net.minecraft.world.entity.player.Player;
import net.syrupstudios.fih.ai.CastingAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public abstract class PlayerCastingMixin implements CastingAccess {
    @Unique private boolean fih$casting;

    @Override
    public void fih$setCasting(boolean casting) { fih$casting = casting; }

    @Override
    public boolean fih$isCasting() { return fih$casting; }
}
