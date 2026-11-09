package net.syrupstudios.fih.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.syrupstudios.fih.FishMeasurements;
import net.syrupstudios.fih.ai.FishMeasurementAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class AbstractFishMeasurementMixin implements FishMeasurementAccess {
    @Unique private static final String fih$LENGTH = "fihLengthCm";
    @Unique private static final String fih$WEIGHT = "fihWeightKg";
    @Unique private static final ResourceLocation fih$SCALE_MODIFIER = ResourceLocation.fromNamespaceAndPath("fih", "fish_size");
    @Unique private float fih$lengthCm;
    @Unique private float fih$weightKg;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void fih$initializeMeasurements(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof AbstractFish fish) || fish.level().isClientSide) return;
        if (fih$lengthCm <= 0 || fih$weightKg <= 0) {
            var range = FishMeasurements.range(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType()));
            float percentile = fish.getRandom().nextFloat();
            fih$lengthCm = range.minLength() + (range.maxLength() - range.minLength()) * percentile;
            fih$weightKg = range.minWeight() + (range.maxWeight() - range.minWeight()) * percentile;
        }
        var instance = fish.getAttribute(Attributes.SCALE);
        if (instance != null && instance.getModifier(fih$SCALE_MODIFIER) == null) {
            var range = FishMeasurements.range(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType()));
            double scale = 0.65 + 0.7 * (fih$lengthCm / Math.max(0.01, range.maxLength()));
            instance.addPermanentModifier(new AttributeModifier(fih$SCALE_MODIFIER, scale - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void fih$saveMeasurements(CompoundTag tag, CallbackInfo ci) {
        if ((Object) this instanceof AbstractFish && fih$lengthCm > 0 && fih$weightKg > 0) {
            tag.putFloat(fih$LENGTH, fih$lengthCm);
            tag.putFloat(fih$WEIGHT, fih$weightKg);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void fih$loadMeasurements(CompoundTag tag, CallbackInfo ci) {
        if (!((Object) this instanceof AbstractFish)) return;
        float length = tag.getFloat(fih$LENGTH);
        float weight = tag.getFloat(fih$WEIGHT);
        fih$lengthCm = Float.isFinite(length) && length > 0 ? length : 0;
        fih$weightKg = Float.isFinite(weight) && weight > 0 ? weight : 0;
    }

    @Override public float fih$lengthCm() { return fih$lengthCm; }
    @Override public float fih$weightKg() { return fih$weightKg; }
    @Override public void fih$setMeasurements(float lengthCm, float weightKg) {
        fih$lengthCm = lengthCm;
        fih$weightKg = weightKg;
    }

}
