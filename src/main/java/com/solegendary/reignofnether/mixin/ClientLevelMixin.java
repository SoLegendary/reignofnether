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
        if (!OrthoviewClientEvents.isEnabled() || SoundClientEvents.STATIC_SOUNDS.contains(pSound.get()))
            return;

        ci.cancel();
        if (pSound.get().equals(SoundEvents.WARDEN_HEARTBEAT))
            return;

        float volumeMult = 0.5f;
        if (isWardenSound(pSound.get()))
            volumeMult = 0.2f;
        else if (isGhastHurt(pSound.get()))
            volumeMult = 0.1f;

        this.playSoundActual(pX, pY, pZ, pSound.get(), pSource, pVolume * volumeMult, pPitch, false, pSeed);
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

    // when near a source of night distortion, speed up time towards midnight (in whichever direction is closest)
    @Inject(
            method = "tickTime",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tickTime(CallbackInfo ci) {
        if (minecraft.level == null)
            return;

        ci.cancel();

        long timeNow = minecraft.level.getDayTime();
        long targetTime = TimeClientEvents.targetClientTime;
        long targetTimePlusHalfDay = targetTime + 12000;

        // transition through day faster if in orthoview and we aren't near dawn/dusk since you can't see the sky anyway
        long timeDiff = 100L;
        if (OrthoviewClientEvents.isEnabled() &&
                ((timeNow > 2000 && timeNow <= 10000) || (timeNow > 14000 && timeNow <= 22000)))
            timeDiff = 500L;

        targetTime = normaliseTime(targetTime);
        targetTimePlusHalfDay = normaliseTime(targetTimePlusHalfDay);

        if (targetTime < 12000 && (timeNow > targetTime && timeNow <= targetTimePlusHalfDay))
            timeDiff *= -1;
        else if (targetTime >= 12000 && (timeNow > targetTime || timeNow <= targetTimePlusHalfDay))
            timeDiff *= -1;

        long timeSet;
        if (Math.abs(timeNow - targetTime) < Math.abs(timeDiff))
            timeSet = targetTime;
        else
            timeSet = minecraft.level.getLevelData().getGameTime() + timeDiff;

        timeSet = normaliseTime(timeSet);

        this.setGameTime(timeSet);
        this.setDayTime(timeSet);
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
