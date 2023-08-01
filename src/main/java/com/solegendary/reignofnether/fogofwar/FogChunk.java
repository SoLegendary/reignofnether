package com.solegendary.reignofnether.fogofwar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;

public class FogChunk {
    private final static float TRANSITION_TICKS = 10;

    public static final float BRIGHT = 1.0f;
    public static final float SEMI = 0.15f;
    public static final float DARK = 0f;

    public static Minecraft MC = Minecraft.getInstance();

    public LevelRenderer.RenderChunkInfo chunkInfo; // IMPORTANT to note that chunkInfos are 3D so there may be vertical chunks too

    //public Set<LevelRenderer.RenderChunkInfo> chunkInfos;
    //public ChunkAccess chunk; // whole chunk column
    public Boolean shouldBeRendered; // only set false for explored chunks - rendered only once to retain its freeze-frame effect
    public FogTransitionBrightness fogTB;
    public float brightness;

    public boolean needsLightUpdate = true; // set for semi-dark chunks to refresh which allows lighting updates

    public FogChunk(LevelRenderer.RenderChunkInfo chunkInfo, FogTransitionBrightness fogTB) {
        this.chunkInfo = chunkInfo;
        this.shouldBeRendered = true;
        this.setBrightness(fogTB);
    }

    public float getFinalBrightness() {
        switch(this.fogTB) {
            case DARK_TO_BRIGHT, SEMI_TO_BRIGHT -> {
                return BRIGHT;
            }
            case DARK_TO_SEMI, BRIGHT_TO_SEMI -> {
                return SEMI;
            }
            case SEMI_TO_DARK, BRIGHT_TO_DARK -> {
                return DARK;
            }
        }
        return BRIGHT;
    }

    public void setBrightness(FogTransitionBrightness tb) {
        FogTransitionBrightness originalBrightness = this.fogTB;
        this.fogTB = tb;
        switch(this.fogTB) {
            case BRIGHT_TO_SEMI, BRIGHT_TO_DARK -> this.brightness = BRIGHT;
            case SEMI_TO_DARK, SEMI_TO_BRIGHT -> {
                this.brightness = SEMI;
                this.shouldBeRendered = true;
            }
            case DARK_TO_SEMI, DARK_TO_BRIGHT -> this.brightness = DARK;
        }
    }

    public boolean isAtFinalBrightness() {
        if (!FogOfWarClientEvents.smoothBrightnessEnabled)
            return true;

        return (this.fogTB == FogTransitionBrightness.SEMI_TO_BRIGHT && this.brightness == BRIGHT) ||
                (this.fogTB == FogTransitionBrightness.DARK_TO_BRIGHT && this.brightness == BRIGHT) ||
                (this.fogTB == FogTransitionBrightness.BRIGHT_TO_SEMI && this.brightness == SEMI) ||
                (this.fogTB == FogTransitionBrightness.DARK_TO_SEMI && this.brightness == SEMI) ||
                (this.fogTB == FogTransitionBrightness.BRIGHT_TO_DARK && this.brightness == DARK) ||
                (this.fogTB == FogTransitionBrightness.SEMI_TO_DARK && this.brightness == DARK);
    }

    public void tickBrightness() {
        float originalBrightness = this.brightness;

        if (!FogOfWarClientEvents.smoothBrightnessEnabled) {
            this.brightness = getFinalBrightness();
        } else {
            switch(this.fogTB) {
                case BRIGHT_TO_SEMI -> {
                    if (this.brightness > SEMI)
                        this.brightness -= ((BRIGHT - SEMI) / TRANSITION_TICKS);
                    else
                        this.brightness = SEMI;
                    if (brightness < SEMI)
                        brightness = SEMI;
                }
                case SEMI_TO_DARK -> {
                    if (this.brightness > DARK)
                        this.brightness -= ((SEMI - DARK) / TRANSITION_TICKS);
                    else
                        this.brightness = DARK;
                    if (brightness < DARK)
                        brightness = DARK;
                }
                case BRIGHT_TO_DARK -> {
                    if (this.brightness > DARK)
                        this.brightness -= ((BRIGHT - DARK) / TRANSITION_TICKS);
                    else
                        this.brightness = DARK;
                    if (brightness < DARK)
                        brightness = DARK;
                }
                case SEMI_TO_BRIGHT -> {
                    if (this.brightness < BRIGHT)
                        this.brightness += ((BRIGHT - SEMI) / TRANSITION_TICKS);
                    else
                        this.brightness = BRIGHT;
                    if (brightness > BRIGHT)
                        brightness = BRIGHT;
                }
                case DARK_TO_SEMI -> {
                    if (this.brightness < SEMI)
                        this.brightness += ((SEMI - DARK) / TRANSITION_TICKS);
                    else
                        this.brightness = SEMI;
                    if (brightness > SEMI)
                        brightness = SEMI;
                }
                case DARK_TO_BRIGHT -> {
                    if (this.brightness < BRIGHT)
                        this.brightness += ((BRIGHT - DARK) / TRANSITION_TICKS);
                    else
                        this.brightness = BRIGHT;
                    if (brightness > BRIGHT)
                        brightness = BRIGHT;
                }
            }
        }
        if (this.brightness != originalBrightness) {
            this.chunkInfo.chunk.setDirty(true);
            this.chunkInfo.chunk.playerChanged = true;
            this.needsLightUpdate = true;
        }
    }
}
