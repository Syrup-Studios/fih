package net.syrupstudios.fih.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.syrupstudios.fih.FishDefinitions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public abstract class SpawnPlacementsMixin {
    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static <T extends net.minecraft.world.entity.Entity> void fih$checkFishSpawn(
            EntityType<T> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos,
            RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        if (!FishDefinitions.allowsSpawn(BuiltInRegistries.ENTITY_TYPE.getKey(type), spawnType, level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
