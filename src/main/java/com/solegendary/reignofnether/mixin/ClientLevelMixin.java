package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.sounds.SoundClientEvents;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.solegendary.reignofnether.sounds.SoundClientEvents.getOrthoviewSoundPos;
import static com.solegendary.reignofnether.time.TimeUtils.normaliseTime;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Shadow @Final private Minecraft minecraft;

    private boolean isWardenSound(SoundEvent pSoundEvent) {
        return pSoundEvent.getLocation().getPath().contains("warden");
    }
    private boolean isGhastHurt(SoundEvent pSoundEvent) {
        return pSoundEvent.getLocation().getPath().contains("ghast.hurt");
    }

    @Inject(
            method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void playSeededSound(Player pPlayer, double pX, double pY, double pZ, Holder<SoundEvent> pSound, SoundSource pSource, float pVolume, float pPitch, long pSeed, CallbackInfo ci) {
        if (!OrthoviewClientEvents.isEnabled() || SoundClientEvents.STATIC_SOUNDS.contains(pSound.value()))
            return;

        ci.cancel();
        if (pSound.value().equals(SoundEvents.WARDEN_HEARTBEAT))
            return;

        float volumeMult = 0.5f;
        if (isWardenSound(pSound.value()))
            volumeMult = 0.2f;
        else if (isGhastHurt(pSound.value()))
            volumeMult = 0.1f;

        this.playSoundActual(pX, pY, pZ, pSound.value(), pSource, pVolume * volumeMult, pPitch, false, pSeed);
    }

    // plays sounds for orthoview players as though they were on the ground near their selected units/buildings
    @Inject(
            method = "playSound",
            at = @At("HEAD"),
            cancellable = true
    )
    private void playSound(double pX, double pY, double pZ, SoundEvent pSoundEvent, SoundSource pSoundSource,
                           float pVolume, float pPitch, boolean pDistanceDelay, long pSeed, CallbackInfo ci) {
        if (!OrthoviewClientEvents.isEnabled() || SoundClientEvents.STATIC_SOUNDS.contains(pSoundEvent))
            return;

        ci.cancel();
        if (pSoundEvent.equals(SoundEvents.WARDEN_HEARTBEAT))
            return;

        BlockPos bp = new BlockPos((int) pX, (int) pY, (int) pZ);
        if (SoundClientEvents.mutedBps.contains(bp)) {
            SoundClientEvents.mutedBps.remove(bp);
            return;
        }

        float volumeMult = 0.5f;
        if (isWardenSound(pSoundEvent))
            volumeMult = 0.2f;
        else if (isGhastHurt(pSoundEvent))
            volumeMult = 0.1f;

        this.playSoundActual(pX, pY, pZ, pSoundEvent, pSoundSource, pVolume * volumeMult, pPitch, false, pSeed);
    }

    // not a mixin, but called by them
    private void playSoundActual(double pX, double pY, double pZ, SoundEvent pSoundEvent, SoundSource pSource,
                           float pVolume, float pPitch, boolean pDistanceDelay, long pSeed) {
        if (!FogOfWarClientEvents.isInBrightChunk(new BlockPos((int) (pX + 0.5f), (int) (pY + 0.5f), (int) (pZ + 0.5f))) &&
                !pSoundEvent.getLocation().getPath().contains("ui.button.click") &&
                !pSoundEvent.getLocation().getNamespace().contains("reignofnether"))
            return;

        Vec3 soundPos = getOrthoviewSoundPos(new Vec3(pX, pY, pZ));

        double d0 = this.minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(soundPos.x(), soundPos.y(), soundPos.z());
        SimpleSoundInstance simplesoundinstance = new SimpleSoundInstance(
                pSoundEvent, pSource, pVolume, pPitch, RandomSource.create(pSeed), soundPos.x(), soundPos.y(), soundPos.z()
        );
        if (pDistanceDelay && d0 > 100.0) {
            double d1 = Math.sqrt(d0) / 40.0;
            this.minecraft.getSoundManager().playDelayed(simplesoundinstance, (int)(d1 * 20.0));
        } else {
            this.minecraft.getSoundManager().play(simplesoundinstance);
        }
    }

    @Shadow public void setGameTime(long pTime) { }
    @Shadow public void setDayTime(long pTime) { }

    // Every write the server makes to the client's day clock lands here - both the vanilla time
    // packet and NeoForge's ClientboundCustomSetTimePayload. Record it, and while the mod owns the
    // clock swallow it: letting it through would snap the sky back to the real time once a second and
    // undo the night distortion the player is standing in.
    @Inject(
            method = "setDayTime",
            at = @At("HEAD"),
            cancellable = true
    )
    private void reignofnether$observeServerTime(long pDayTime, CallbackInfo ci) {
        if (TimeClientEvents.writingWarpedTime)
            return;
        // vanilla sends a negative day time while doDaylightCycle is off
        TimeClientEvents.onServerTimeUpdate(Math.abs(pDayTime),
                this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime());
        if (TimeClientEvents.owningClock)
            ci.cancel();
    }

    // when near a source of night distortion, speed up time towards midnight (in whichever direction is closest)
    @Inject(
            method = "tickTime",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tickTime(CallbackInfo ci) {
        if (minecraft.level == null)
            return;

        // Where the clock should be is decided from the camera every tick, so stepping in or out of a
        // night source takes effect immediately instead of up to a second later (the next server time
        // update). While the mod owns the clock - inside a source, or still warping home from one - it
        // also swallows the server's writes; see owningClock below.
        TimeClientEvents.updateClockTarget();

        long timeNow = minecraft.level.getDayTime();
        long targetTime = normaliseTime(TimeClientEvents.targetClientTime);

        // Signed distance to the target on the 24000-tick day circle, so the shortest way round is
        // taken instead of wrapping through the start of the day.
        long diff = targetTime - timeNow;
        if (diff > 12000) diff -= 24000;
        else if (diff < -12000) diff += 24000;

        // Ownership is a latch, not a comparison: entering a night source takes the clock, and it is
        // only handed back once the warp home has arrived back on the server's time. Handing it back
        // the moment the last unit left the aura applied the server's time in one frame, which is
        // exactly what made daybreak look instantaneous.
        //
        // It must NOT be "own it whenever the clock differs from the server's": between two server
        // updates (one per second) the client clock always runs a few ticks ahead of the last value
        // received, so that test stays true everywhere on the map, the vanilla tick is cancelled every
        // tick and the day only ever advances in one-second steps.
        if (TimeClientEvents.steeringClock)
            TimeClientEvents.owningClock = true;
        else if (TimeClientEvents.owningClock && diff == 0L)
            TimeClientEvents.owningClock = false;

        if (!TimeClientEvents.owningClock)
            return;

        ci.cancel();

        // Approach the target monotonically and stop on arrival: clamping the step to the remaining
        // distance makes an overshoot impossible, so the motion is smooth in both directions and there
        // is nothing to jitter. The rate is the whole effect - at 1 tick per tick the distance to
        // midnight never closes and the sky just stands still, which is why the aura used to be visible
        // in the code and invisible in the game. 100 per tick (500 in orthoview through the hours when
        // the sky is not the subject) matches upstream.
        long timeOfDay = normaliseTime(timeNow);
        long rate = TimeClientEvents.steeringClock ? 100L : TimeClientEvents.RETURN_RATE;
        if (TimeClientEvents.steeringClock && OrthoviewClientEvents.isEnabled() &&
            ((timeOfDay > 2000 && timeOfDay <= 10000) || (timeOfDay > 14000 && timeOfDay <= 22000)))
            rate = 500L;

        long step = Math.min(Math.abs(diff), rate);
        long timeSet = normaliseTime(timeNow + (diff >= 0 ? step : -step));

        // The day clock is the only one steered. The sun and moon are drawn from it too
        // (LevelRenderer rotates the sky pose by level.getTimeOfDay(), which reads the day clock);
        // the game clock only feeds RenderSystem.setShaderGameTime for shader animation, and moving
        // that with the day's step - or reversing it - is what made the celestial bodies leap about
        // the sky even though /time query, which reads the day clock, looked correct.
        this.setGameTime(this.minecraft.level.getLevelData().getGameTime() + 1L);
        TimeClientEvents.writingWarpedTime = true;
        try {
            this.setDayTime(timeSet);
        } finally {
            TimeClientEvents.writingWarpedTime = false;
        }
    }

    @Inject(
            method = "addDestroyBlockEffect",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddDestroyBlockEffect(BlockPos pPos, BlockState pState, CallbackInfo ci) {
        if (!FogOfWarClientEvents.isInBrightChunk(pPos))
            ci.cancel();
    }

    @Inject(
            method = "getSkyColor",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getSkyColor(Vec3 pPos, float pPartialTick, CallbackInfoReturnable<Vec3> cir) {
        if (TimeClientEvents.isBloodMoonActive())
            cir.setReturnValue(new Vec3(0.25f, 0f, 0f));
    }

    @Inject(
            method = "setServerVerifiedBlockState",
            at = @At("HEAD"),
            cancellable = true
    )
    private void reignofnether$gateFoggedBlockUpdate(BlockPos pPos, BlockState pState, int pFlags, CallbackInfo ci) {
        if (FogOfWarClientEvents.isEnabled() && !FogOfWarClientEvents.isBlockVisible(pPos))
            ci.cancel();
    }
}
