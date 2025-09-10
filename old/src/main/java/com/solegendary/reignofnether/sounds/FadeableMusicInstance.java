package com.solegendary.reignofnether.sounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class FadeableMusicInstance extends AbstractTickableSoundInstance {
    private boolean fadingOut = false;

    public FadeableMusicInstance(SoundEvent soundEvent) {
        super(soundEvent, SoundSource.MUSIC, RandomSource.create());
        this.volume = 1.0f;
        this.looping = true;
        this.delay = 0;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    @Override
    public void tick() {
        if (fadingOut) {
            volume -= 0.01f;
            if (volume <= 0f) {
                volume = 0f;
                this.stop();
            }
        }
    }

    public void startFadeOut() {
        fadingOut = true;
    }
}
