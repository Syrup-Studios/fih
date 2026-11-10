package net.syrupstudios.fih;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class FihSounds {
    public static final ResourceLocation REEL_IN_ID = ResourceLocation.fromNamespaceAndPath("fih", "reel_in");
    public static final ResourceLocation LINE_OUT_ID = ResourceLocation.fromNamespaceAndPath("fih", "line_out");
    public static final SoundEvent REEL_IN = SoundEvent.createVariableRangeEvent(REEL_IN_ID);
    public static final SoundEvent LINE_OUT = SoundEvent.createVariableRangeEvent(LINE_OUT_ID);

    private FihSounds() {}
}
