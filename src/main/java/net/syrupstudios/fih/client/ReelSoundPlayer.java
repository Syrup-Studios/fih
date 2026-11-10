package net.syrupstudios.fih.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.syrupstudios.fih.FihSounds;
import net.syrupstudios.fih.ai.FishingHookAccess;

public final class ReelSoundPlayer {
    private static final Map<FishingHook, ReelSoundState> reelSounds = new WeakHashMap<>();
    private static final class ReelSoundState {
        double lastTime = Double.NaN;
        double clipRemaining;
        double gapProgress;
        boolean ready = true;
    }
    private ReelSoundPlayer() {}

    public static void render(DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            reelSounds.clear();
            return;
        }
        float partialTick = delta.getGameTimeDeltaPartialTick(false);
        for (Player player : minecraft.level.players()) {
            FishingHook hook = player.fishing;
            if (hook == null || hook.isRemoved()) continue;
            ReelSoundState state = reelSounds.computeIfAbsent(hook, ignored -> new ReelSoundState());
            double time = hook.tickCount + partialTick;
            double elapsed = Double.isNaN(state.lastTime) ? 0.0 : Math.clamp(time - state.lastTime, 0.0, 2.0);
            state.lastTime = time;
            double seconds = elapsed / 20.0;
            double clipTime = Math.min(seconds, state.clipRemaining);
            state.clipRemaining -= clipTime;
            seconds -= clipTime;
            float movement = ((FishingHookAccess) hook).fih$lineSoundSpeed(partialTick);
            double speed = Math.clamp(Math.abs(movement), 0.0F, 1.0F);
            if (speed < 0.001) {
                state.gapProgress = 0.0;
                state.ready = true;
                continue;
            }
            if (state.clipRemaining > 0.0) continue;
            double gap = movement > 0.0F
                    ? Math.min(5.0, 0.01 + 0.05 * (1.0 - speed) / speed)
                    : 0.25 - 0.23 * speed;
            if (!state.ready && gap > 0.0) state.gapProgress += seconds / gap;
            if (state.ready || gap <= 0.0 || state.gapProgress >= 1.0) {
                double remainingProgress = state.ready || gap <= 0.0 ? 0.0 : state.gapProgress - 1.0;
                minecraft.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        movement > 0.0F ? FihSounds.REEL_IN : FihSounds.LINE_OUT,
                        SoundSource.PLAYERS, 0.35F, 1.0F, false);
                state.clipRemaining = 0.057;
                state.gapProgress = Math.min(remainingProgress, 1.0);
                state.ready = false;
            }
        }
    }
}
