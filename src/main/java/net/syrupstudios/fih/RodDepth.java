package net.syrupstudios.fih;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameRules;

public final class RodDepth {
    public static final int DEFAULT = 2;
    private static final int MAX_SUPPORTED = Short.MAX_VALUE;

    private RodDepth() {}

    public static int maximum(GameRules rules) {
        return Math.clamp(rules.getInt(FihGameRules.MAX_HOOK_DEPTH), 0, MAX_SUPPORTED);
    }

    public static int get(ItemStack rod) {
        CompoundTag data = rod.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("fih");
        return data.contains("hook_depth") ? Math.clamp(data.getInt("hook_depth"), 0, MAX_SUPPORTED) : DEFAULT;
    }

    public static void set(ItemStack rod, int depth) {
        CustomData.update(DataComponents.CUSTOM_DATA, rod, data -> {
            CompoundTag fih = data.getCompound("fih");
            fih.putInt("hook_depth", depth);
            data.put("fih", fih);
        });
    }
}
