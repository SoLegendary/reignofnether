package com.solegendary.reignofnether.sounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class FadeableSoundInstance extends AbstractTickableSoundInstance {

    private static final float FADE_SPEED = 0.025f;

    private boolean fadingIn;
    private boolean fadingOut = false;
    public final int id;
    private float volumeMult;
    private int ticksLeft;

    public FadeableSoundInstance(SoundEvent soundEvent, BlockPos pos, int id, boolean fadeIn, float volumeMult, int tickDuration) {
        super(soundEvent, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01f;
        this.fadingIn = fadeIn;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.id = id;
        this.volumeMult = volumeMult;
        this.ticksLeft = tickDuration;
    }

    @Override
    public void tick() {
        if (ticksLeft > 0) {
            ticksLeft -= 1;
            if (ticksLeft <= 0)
                fadeOut();
        }
        if (fadingIn) {
            if (volume < volumeMult)
                volume += FADE_SPEED * volumeMult;
            else
                fadingIn = false;
        }
        if (fadingOut) {
            volume -= FADE_SPEED * volumeMult;
            if (volume <= 0.0f) {
                stop();
            }
        }
    }

    public void fadeOut() {
        fadingOut = true;
    }
}