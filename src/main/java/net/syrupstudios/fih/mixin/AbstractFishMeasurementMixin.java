package net.syrupstudios.fih.mixin;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.syrupstudios.fih.FishDefinitions;
import net.syrupstudios.fih.ai.FishMeasurementAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class AbstractFishMeasurementMixin implements FishMeasurementAccess {
    @Unique private static final String fih$LENGTH = "fihLengthCm";
    @Unique private static final String fih$WEIGHT = "fihWeightKg";
    @Unique private static final ResourceLocation fih$SCALE_MODIFIER = ResourceLocation.fromNamespaceAndPath("fih", "fish_size");
    @Unique private float fih$lengthCm;
    @Unique private float fih$weightKg;

    @Redirect(method = "dropFromLootTable", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"))
    private void fih$copyMeasurementsToDrops(LootTable table, LootParams params, long seed, Consumer<ItemStack> output) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof AbstractFish fish)) {
            table.getRandomItems(params, seed, output);
            return;
        }
        table.getRandomItems(params, seed, stack -> {
            if (FishDefinitions.isFishDrop(fish, stack)) FishDefinitions.copyToItem(fish, stack);
            output.accept(stack);
        });
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void fih$initializeMeasurements(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof AbstractFish fish) || fish.level().isClientSide) return;
        if (fih$lengthCm <= 0 || fih$weightKg <= 0) {
            var range = FishDefinitions.range(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType()));
            float percentile = fish.getRandom().nextFloat();
            fih$lengthCm = range.minLength() + (range.maxLength() - range.minLength()) * percentile;
            fih$weightKg = range.minWeight() + (range.maxWeight() - range.minWeight()) * percentile;
        }
        var instance = fish.getAttribute(Attributes.SCALE);
        if (instance != null && instance.getModifier(fih$SCALE_MODIFIER) == null) {
            var range = FishDefinitions.range(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType()));
            double scale = FishDefinitions.scaleForLength(fih$lengthCm, range);
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
