package net.syrupstudios.fih;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.syrupstudios.fih.menu.TackleMenu;

public final class TackleBoxItem extends Item {
    public TackleBoxItem(Properties properties) { super(properties); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, ignored) -> new TackleMenu(id, inventory, slot, -1),
                    Component.translatable("item.fih.tackle_box")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
