package net.syrupstudios.fih.ai;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;

public final class HeldFish {
    private static final TagKey<Item> DISPLAY_FISH = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("fih", "display_fish"));
    private HeldFish() {}

    public static boolean isFish(ItemStack stack) {
        return fishType(stack) != null;
    }

    public static ResourceLocation fishType(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag data = customData.copyTag().getCompound("fih");
            if (data.contains("display_fish")) {
                return data.getBoolean("display_fish") ? ResourceLocation.tryParse(data.getString("entity_type")) : null;
            }
        }
        return stack.is(DISPLAY_FISH) ? BuiltInRegistries.ITEM.getKey(stack.getItem()) : null;
    }

    public static void holdCatch(Player player, ItemStack catchStack) {
        if (!isFish(catchStack)) {
            putInInventoryOrDrop(player, catchStack);
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            insertIntoInventory(player.getInventory(), offhand);
            if (!offhand.isEmpty()) {
                player.setItemInHand(InteractionHand.OFF_HAND, offhand);
                putInInventoryOrDrop(player, catchStack);
                return;
            }
        }
        player.setItemInHand(InteractionHand.OFF_HAND, catchStack);
    }

    public static void stow(Player player) {
        ItemStack fish = player.getOffhandItem();
        if (!isFish(fish)) return;
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        insertIntoInventory(player.getInventory(), fish);
        if (!fish.isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, fish);
            player.displayClientMessage(Component.literal("Inventory is full"), true);
        }
    }

    private static void putInInventoryOrDrop(Player player, ItemStack stack) {
        insertIntoInventory(player.getInventory(), stack);
        if (!stack.isEmpty()) player.drop(stack, false);
    }

    private static void insertIntoInventory(Inventory inventory, ItemStack stack) {
        for (int slot = 0; slot < inventory.items.size() && !stack.isEmpty(); slot++) {
            ItemStack existing = inventory.items.get(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) continue;
            int space = Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize()) - existing.getCount();
            int moved = Math.clamp(space, 0, stack.getCount());
            existing.grow(moved);
            stack.shrink(moved);
        }
        for (int slot = 0; slot < inventory.items.size() && !stack.isEmpty(); slot++) {
            if (inventory.items.get(slot).isEmpty()) {
                inventory.setItem(slot, stack.split(Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize())));
            }
        }
        inventory.setChanged();
    }
}
