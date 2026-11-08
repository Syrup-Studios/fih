package net.syrupstudios.fih.mixin;

import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRules.IntegerValue.class)
public interface IntegerGameRuleAccess {
    @Invoker("create")
    static GameRules.Type<GameRules.IntegerValue> fih$create(int defaultValue) {
        throw new AssertionError();
    }
}
