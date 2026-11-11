package net.syrupstudios.fih.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.syrupstudios.fih.menu.TackleMenu;

public final class TackleScreen extends AbstractContainerScreen<TackleMenu> {
    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath("fih", "textures/gui/tackle_box.png");
    private static final ResourceLocation WITH_ROD = ResourceLocation.fromNamespaceAndPath("fih", "textures/gui/tackle_box_rod.png");
    private static final int LINE_X = 195, LINE_TOP = 20, LINE_BOTTOM = 75;
    private boolean draggingBobber;
    private int previewDepth = -1;
    private double dragOffsetY;

    public TackleScreen(TackleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 206;
        imageHeight = 166;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(menu.hasRod() ? WITH_ROD : EMPTY, leftPos, topPos, 0, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, playerInventoryTitle, 8, 72, 0x404040, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!menu.bobber().isEmpty()) {
            if (!draggingBobber && previewDepth == menu.depth()) previewDepth = -1;
            int depth = displayedDepth();
            int y = bobberY(depth);
            graphics.renderItem(menu.bobber(), leftPos + LINE_X - 8, topPos + y - 8);
            if (draggingBobber || isHovering(LINE_X - 8, y - 8, 16, 16, mouseX, mouseY)) {
                graphics.renderTooltip(font, Component.translatable("screen.fih.hook_depth", depth), mouseX, mouseY);
                return;
            }
        } else {
            previewDepth = -1;
            draggingBobber = false;
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private int bobberY(int depth) {
        return menu.maxDepth() == 0 ? LINE_BOTTOM : LINE_BOTTOM - Math.round((float) depth / menu.maxDepth() * (LINE_BOTTOM - LINE_TOP));
    }

    private int displayedDepth() {
        return previewDepth < 0 ? menu.depth() : Math.min(previewDepth, menu.maxDepth());
    }

    private void updateDepth(double mouseY) {
        double position = Math.clamp((LINE_BOTTOM - (mouseY - topPos - dragOffsetY)) / (LINE_BOTTOM - LINE_TOP), 0.0, 1.0);
        int depth = (int) Math.round(position * menu.maxDepth());
        if (depth != previewDepth) {
            previewDepth = depth;
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, depth);
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int depth = displayedDepth();
        if (button == 0 && !menu.bobber().isEmpty()
                && isHovering(LINE_X - 8, bobberY(depth) - 8, 16, 16, mouseX, mouseY)) {
            draggingBobber = true;
            dragOffsetY = mouseY - topPos - bobberY(depth);
            updateDepth(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBobber && button == 0) {
            updateDepth(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingBobber && button == 0) {
            updateDepth(mouseY);
            draggingBobber = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
