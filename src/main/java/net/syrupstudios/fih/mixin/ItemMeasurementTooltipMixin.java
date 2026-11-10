package net.syrupstudios.fih.mixin;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemMeasurementTooltipMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void fih$appendMeasurements(Item.TooltipContext context, Player player, TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        var tag = customData.copyTag().getCompound("fih");
        if (!tag.contains("length_cm") || !tag.contains("weight_kg")) return;
        float length = tag.getFloat("length_cm");
        float weight = tag.getFloat("weight_kg");
        if (!Float.isFinite(length) || !Float.isFinite(weight) || length <= 0 || weight <= 0) return;
        List<Component> tooltip = new java.util.ArrayList<>(cir.getReturnValue());
        tooltip.add(Component.translatable("item.fih.measurements.length", Component.literal(String.format(
                java.util.Locale.ROOT, "%.1f", length)).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.fih.measurements.weight", Component.literal(String.format(
                java.util.Locale.ROOT, "%.2f", weight)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));
        cir.setReturnValue(tooltip);
    }
}
