package net.syrupstudios.fih.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.syrupstudios.fih.ai.FishMeasurementAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFish.class)
public abstract class BucketableMixin {
    @Unique private static final ResourceLocation FIH_SCALE_MODIFIER = ResourceLocation.fromNamespaceAndPath("fih", "fish_size");

    @Inject(method = "saveToBucketTag", at = @At("TAIL"))
    private void fih$saveMeasurementsToBucket(ItemStack stack, CallbackInfo ci) {
        if (!((Object) this instanceof FishMeasurementAccess measurements)
                || measurements.fih$lengthCm() <= 0 || measurements.fih$weightKg() <= 0) return;

        CompoundTag bucketTag = stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
        CompoundTag fishTag = bucketTag.getCompound("fih").copy();
        fishTag.putFloat("length_cm", measurements.fih$lengthCm());
        fishTag.putFloat("weight_kg", measurements.fih$weightKg());
        if ((Object) this instanceof LivingEntity fish && fish.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) != null) {
            var modifier = fish.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE)
                    .getModifier(FIH_SCALE_MODIFIER);
            if (modifier != null) fishTag.putDouble("scale_modifier", modifier.amount());
        }
        bucketTag.put("fih", fishTag);
        stack.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(bucketTag));
    }

    @Inject(method = "loadFromBucketTag", at = @At("TAIL"))
    private void fih$loadMeasurementsFromBucket(CompoundTag bucketTag, CallbackInfo ci) {
        if (!((Object) this instanceof FishMeasurementAccess measurements)) return;
        CompoundTag fishTag = bucketTag.getCompound("fih");
        float length = fishTag.getFloat("length_cm");
        float weight = fishTag.getFloat("weight_kg");
        if (!Float.isFinite(length) || length <= 0 || !Float.isFinite(weight) || weight <= 0) return;

        measurements.fih$setMeasurements(length, weight);
        if ((Object) this instanceof LivingEntity fish && fishTag.contains("scale_modifier", net.minecraft.nbt.Tag.TAG_DOUBLE)) {
            double amount = fishTag.getDouble("scale_modifier");
            var attribute = fish.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
            if (attribute != null && Double.isFinite(amount)) {
                attribute.removeModifier(FIH_SCALE_MODIFIER);
                attribute.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        FIH_SCALE_MODIFIER, amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }
}
