package net.syrupstudios.fih.ai;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

public final class HeldFish {
    private HeldFish() {}

    public static boolean isFish(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON)
                || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH);
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
