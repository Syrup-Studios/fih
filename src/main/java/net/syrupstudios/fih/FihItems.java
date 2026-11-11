package net.syrupstudios.fih;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class FihItems {
    public static final ResourceLocation TACKLE_BOX_ID = ResourceLocation.fromNamespaceAndPath(ModIdentity.MOD_ID, "tackle_box");
    public static final Item TACKLE_BOX = new TackleBoxItem(new Item.Properties().stacksTo(1));
    public static final Item BONE_HOOK = new Item(new Item.Properties().stacksTo(16));
    public static final Item IRON_HOOK = new Item(new Item.Properties().stacksTo(16));
    public static final Item BOBBER = new Item(new Item.Properties().stacksTo(16));

    private FihItems() {}
}
