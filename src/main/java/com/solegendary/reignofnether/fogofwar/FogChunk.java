package com.solegendary.reignofnether.fogofwar;

import net.minecraft.client.renderer.LevelRenderer;

public class FogChunk {
    private final static float TRANSITION_TICKS = 20;

    public static float BRIGHT = 1.0f;
    public static float SEMI = 0.15f;
    public static float DARK = 0f;

    public LevelRenderer.RenderChunkInfo chunkInfo;
    public Boolean shouldBeRendered; // only set false for explored chunks - rendered only once to retain its freeze-frame effect
    public FogTransitionBrightness fogTB;
    public float brightness;

    public FogChunk(LevelRenderer.RenderChunkInfo chunkInfo, FogTransitionBrightness fogTB) {
        this.chunkInfo = chunkInfo;
        this.shouldBeRendered = true;
        this.fogTB = fogTB;
        switch(this.fogTB) {
            case BRIGHT_TO_SEMI, BRIGHT_TO_DARK -> this.brightness = BRIGHT;
            case SEMI_TO_DARK, SEMI_TO_BRIGHT -> this.brightness = SEMI;
            case DARK_TO_SEMI, DARK_TO_BRIGHT -> this.brightness = DARK;
        }
    }

    public void tickBrightness() {
        float originalBrightness = this.brightness;
        switch(this.fogTB) {
            case BRIGHT_TO_SEMI -> {
                if (this.brightness > SEMI)
                    this.brightness -= ((BRIGHT - SEMI) / TRANSITION_TICKS);
                else
                    this.brightness = SEMI;
            }
            case SEMI_TO_DARK -> {
                if (this.brightness > DARK)
                    this.brightness -= ((SEMI - DARK) / TRANSITION_TICKS);
                else
                    this.brightness = DARK;
            }
            case BRIGHT_TO_DARK -> {
                if (this.brightness > DARK)
                    this.brightness -= ((BRIGHT - DARK) / TRANSITION_TICKS);
                else
                    this.brightness = DARK;
            }
            case SEMI_TO_BRIGHT -> {
                if (this.brightness < BRIGHT)
                    this.brightness += ((BRIGHT - SEMI) / TRANSITION_TICKS);
                else
                    this.brightness = BRIGHT;
            }
            case DARK_TO_SEMI -> {
                if (this.brightness < SEMI)
                    this.brightness += ((SEMI - DARK) / TRANSITION_TICKS);
                else
                    this.brightness = SEMI;
            }
            case DARK_TO_BRIGHT -> {
                if (this.brightness < BRIGHT)
                    this.brightness += ((BRIGHT - DARK) / TRANSITION_TICKS);
                else
                    this.brightness = BRIGHT;
            }
        }
        if (this.brightness != originalBrightness)
            this.chunkInfo.chunk.setDirty(true);
    }
}
