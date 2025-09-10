package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.registrars.SoundRegistrar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(MusicManager.class)
public class MusicManagerMixin {

    @Shadow private int nextSongDelay = 100;
    @Shadow @Final private Minecraft minecraft;
    @Nullable @Shadow private SoundInstance currentMusic;
    @Shadow @Final private RandomSource random = RandomSource.create();
    @Shadow public void startPlaying(Music pSelector) {}

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tick(CallbackInfo ci) {
        Music music = this.minecraft.getSituationalMusic();
        if (music == Musics.MENU) {
            ci.cancel();
            music = new Music(Holder.direct(SoundRegistrar.MAIN_MENU.get()), 0, 0, true);
            this.nextSongDelay = 0;
        } else {
            return;
        }
        if (this.currentMusic != null) {
            if (!music.getEvent().value().getLocation().equals(this.currentMusic.getLocation()) && music.replaceCurrentMusic()) {
                this.minecraft.getSoundManager().stop(this.currentMusic);
            }
            if (!this.minecraft.getSoundManager().isActive(this.currentMusic)) {
                this.currentMusic = null;
            }
        }
        if (this.currentMusic == null && this.nextSongDelay-- <= 0)
            this.startPlaying(music);
    }
}