package net.syrupstudios.fih;

import net.minecraft.world.level.GameRules;
import net.syrupstudios.fih.mixin.GameRulesAccess;
import net.syrupstudios.fih.mixin.IntegerGameRuleAccess;

public final class FihGameRules {
    public static final GameRules.Key<GameRules.IntegerValue> APPROACH_CHANCE = register("fihApproachChance", 200);
    public static final GameRules.Key<GameRules.IntegerValue> RETREAT_CHANCE = register("fihRetreatChance", 20);
    public static final GameRules.Key<GameRules.IntegerValue> IGNORE_CHANCE = register("fihIgnoreChance", 1500);
    public static final GameRules.Key<GameRules.IntegerValue> DEBUG = register("fihDebug", 0);

    private FihGameRules() {}

    private static GameRules.Key<GameRules.IntegerValue> register(String name, int value) {
        return GameRulesAccess.fih$register(name, GameRules.Category.MOBS, IntegerGameRuleAccess.fih$create(value));
    }

    public static int chance(GameRules rules, GameRules.Key<GameRules.IntegerValue> key) {
        return Math.clamp(rules.getInt(key), 0, 10000);
    }

    public static void initialize() {}
}
