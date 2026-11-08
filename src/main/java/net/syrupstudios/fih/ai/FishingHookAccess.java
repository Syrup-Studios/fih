package net.syrupstudios.fih.ai;

import net.minecraft.world.entity.animal.AbstractFish;

public interface FishingHookAccess {
    boolean fih$targets(AbstractFish fish);
    String fih$diagnostics(AbstractFish fish);
    boolean fih$reserve(AbstractFish fish);
    void fih$release(AbstractFish fish);
    void fih$completeApproach(AbstractFish fish);
}
