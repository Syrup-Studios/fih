package net.syrupstudios.fih.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.fih.FihMenus;
import net.syrupstudios.fih.TackleDefinitions;
import net.syrupstudios.fih.RodDepth;

public final class TackleMenu extends AbstractContainerMenu {
    private static final TackleDefinitions.Type[] TYPES = {
            TackleDefinitions.Type.BOBBER, TackleDefinitions.Type.HOOK, TackleDefinitions.Type.BAIT
    };
    private static final int ROD_SLOT = 6;
    private static final int EQUIPPED_START = 7;
    private static final int PLAYER_START = 10;
    private final Inventory inventory;
    private final int boxIndex, originalRodIndex;
    private final ItemStack box;
    private final SimpleContainer rodInput = new SimpleContainer(1);
    private final SimpleContainer equipped = new SimpleContainer(3);
    private final SimpleContainerData depthData = new SimpleContainerData(2);
    private ItemStack equippedOwner = ItemStack.EMPTY;
    private boolean loadingEquipped;

    public static TackleMenu client(int id, Inventory inventory) {
        return new TackleMenu(id, inventory, -1, -1);
    }

    public TackleMenu(int id, Inventory inventory, int boxIndex, int rodIndex) {
        super(FihMenus.TACKLE, id);
        this.inventory = inventory;
        this.boxIndex = boxIndex;
        this.originalRodIndex = rodIndex;
        this.box = boxIndex >= 0 ? inventory.getItem(boxIndex) : ItemStack.EMPTY;
        SimpleContainer storage = contents(box, 6);
        if (!inventory.player.level().isClientSide && rodIndex >= 0) {
            rodInput.setItem(0, inventory.removeItem(rodIndex, 1));
            loadEquipped();
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new TackleSlot(storage, row * 3 + col, 20 + col * 18, 24 + row * 18, TYPES[col], true, false));
            }
        }
        addSlot(new Slot(rodInput, 0, 116, 14) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof FishingRodItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new TackleSlot(equipped, 0, 170, 43, TYPES[0], false, true));
        addSlot(new TackleSlot(equipped, 1, 188, 84, TYPES[1], false, true));
        addSlot(new TackleSlot(equipped, 2, 188, 102, TYPES[2], false, true));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new InventorySlot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new InventorySlot(inventory, col, 8 + col * 18, 142));
        addDataSlots(depthData);
        if (!inventory.player.level().isClientSide) {
            storage.addListener(container -> save(box, storage));
            rodInput.addListener(container -> loadEquipped());
            equipped.addListener(container -> {
                if (!loadingEquipped && !rodInput.getItem(0).isEmpty()) {
                    save(rodInput.getItem(0), equipped);
                    rodInput.setChanged();
                }
            });
        }
    }

    public boolean hasRod() { return !rodInput.getItem(0).isEmpty(); }
    public ItemStack bobber() { return equipped.getItem(0); }
    public int depth() { return depthData.get(0); }
    public int maxDepth() { return depthData.get(1); }

    @Override public void broadcastChanges() {
        if (!inventory.player.level().isClientSide) {
            int max = RodDepth.maximum(inventory.player.level().getGameRules());
            depthData.set(1, max);
            depthData.set(0, Math.min(RodDepth.get(rodInput.getItem(0)), max));
        }
        super.broadcastChanges();
    }

    @Override public boolean clickMenuButton(Player player, int depth) {
        if (player.level().isClientSide || !stillValid(player) || !hasRod() || bobber().isEmpty()
                || depth < 0 || depth > RodDepth.maximum(player.level().getGameRules())) return false;
        RodDepth.set(rodInput.getItem(0), depth);
        rodInput.setChanged();
        return true;
    }

    private SimpleContainer contents(ItemStack owner, int count) {
        SimpleContainer container = new SimpleContainer(count);
        NonNullList<ItemStack> items = NonNullList.withSize(count, ItemStack.EMPTY);
        owner.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        for (int i = 0; i < count; i++) container.setItem(i, items.get(i));
        return container;
    }

    private void loadEquipped() {
        ItemStack current = rodInput.getItem(0);
        if (current == equippedOwner) return;
        equippedOwner = current;
        NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
        current.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        loadingEquipped = true;
        try {
            for (int i = 0; i < 3; i++) equipped.setItem(i, items.get(i));
        } finally {
            loadingEquipped = false;
        }
    }

    private void save(ItemStack owner, SimpleContainer contents) {
        if (!owner.isEmpty()) {
            owner.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents.getItems()));
            inventory.setChanged();
        }
    }

    private boolean locked(int index) { return index == boxIndex; }

    private final class InventorySlot extends Slot {
        InventorySlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
        @Override public boolean mayPickup(Player player) { return !locked(getContainerSlot()); }
        @Override public boolean mayPlace(ItemStack stack) { return !locked(getContainerSlot()); }
    }

    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot.container == inventory && locked(slot.getContainerSlot())) return;
        }
        if (type == ClickType.SWAP && locked(button)) return;
        super.clicked(slotId, button, type, player);
    }

    @Override public boolean stillValid(Player player) {
        return player.level().isClientSide || (boxIndex >= 0 && inventory.getItem(boxIndex) == box);
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            ItemStack remaining = rodInput.removeItemNoUpdate(0);
            if (!remaining.isEmpty()) {
                if (!player.isAlive() || (player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()))
                    player.drop(remaining, false);
                else if (originalRodIndex >= 0 && inventory.getItem(originalRodIndex).isEmpty())
                    inventory.setItem(originalRodIndex, remaining);
                else inventory.placeItemBackInInventory(remaining);
            }
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || (slot.container == inventory && locked(slot.getContainerSlot()))) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < ROD_SLOT) {
            if (!moveItemStackTo(stack, EQUIPPED_START, PLAYER_START, false)
                    && !moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index == ROD_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, 0, ROD_SLOT, false)
                    && !moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, ROD_SLOT, ROD_SLOT + 1, false)
                    && !moveItemStackTo(stack, 0, ROD_SLOT, false)
                    && !moveItemStackTo(stack, EQUIPPED_START, PLAYER_START, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    private final class TackleSlot extends Slot {
        private final TackleDefinitions.Type type;
        private final boolean unique, requiresRod;
        TackleSlot(SimpleContainer container, int index, int x, int y, TackleDefinitions.Type type, boolean unique, boolean requiresRod) {
            super(container, index, x, y);
            this.type = type;
            this.unique = unique;
            this.requiresRod = requiresRod;
        }
        @Override public boolean isActive() { return !requiresRod || hasRod(); }
        @Override public int getMaxStackSize() { return requiresRod ? 1 : super.getMaxStackSize(); }
        @Override public boolean mayPlace(ItemStack stack) {
            if (!isActive() || !TackleDefinitions.allows(type, stack, inventory.player.level().isClientSide)) return false;
            if (unique) {
                int other = getContainerSlot() < 3 ? getContainerSlot() + 3 : getContainerSlot() - 3;
                if (ItemStack.isSameItem(container.getItem(other), stack)) return false;
            }
            return true;
        }
    }
}
