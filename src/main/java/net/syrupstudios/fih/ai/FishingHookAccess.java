package net.syrupstudios.fih.ai;

import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.phys.Vec3;

public interface FishingHookAccess {
    boolean fih$targets(AbstractFish fish);
    String fih$diagnostics(AbstractFish fish);
    boolean fih$reserve(AbstractFish fish);
    void fih$release(AbstractFish fish);
    void fih$completeApproach(AbstractFish fish);
    void fih$reelIn();
    float fih$visualTension(float partialTick);
    Vec3 fih$hookPosition();
    Vec3 fih$renderHookPosition(float partialTick);
}
