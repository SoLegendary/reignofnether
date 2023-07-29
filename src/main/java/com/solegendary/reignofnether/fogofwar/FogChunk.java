package com.solegendary.reignofnether.fogofwar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Set;

public class FogChunk {
    private final static float TRANSITION_TICKS = 10;

    public static float BRIGHT = 1.0f;
    public static float SEMI = 0.15f;
    public static float DARK = 0f;

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

    // bright chunks are in immediate view of a unit or building
    public boolean isBrightChunk() {
        return this.fogTB == FogTransitionBrightness.DARK_TO_BRIGHT ||
                this.fogTB == FogTransitionBrightness.SEMI_TO_BRIGHT;
    }
    // chunks that have been in range of a unit or building before
    // if out of immediate view will be rendered with semi brightness and at its past state
    public boolean isExploredChunk() {
        return this.fogTB == FogTransitionBrightness.DARK_TO_SEMI ||
                this.fogTB == FogTransitionBrightness.BRIGHT_TO_SEMI;
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
        return (this.fogTB == FogTransitionBrightness.SEMI_TO_BRIGHT && this.brightness == BRIGHT) ||
                (this.fogTB == FogTransitionBrightness.DARK_TO_BRIGHT && this.brightness == BRIGHT) ||
                (this.fogTB == FogTransitionBrightness.BRIGHT_TO_SEMI && this.brightness == SEMI) ||
                (this.fogTB == FogTransitionBrightness.DARK_TO_SEMI && this.brightness == SEMI) ||
                (this.fogTB == FogTransitionBrightness.BRIGHT_TO_DARK && this.brightness == DARK) ||
                (this.fogTB == FogTransitionBrightness.SEMI_TO_DARK && this.brightness == DARK);
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

        if (this.brightness != originalBrightness) {
            this.chunkInfo.chunk.setDirty(true);
            this.chunkInfo.chunk.playerChanged = true;
            this.needsLightUpdate = true;
        }
    }
}
