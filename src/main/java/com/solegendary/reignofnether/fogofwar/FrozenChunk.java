package com.solegendary.reignofnether.fogofwar;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;

public class FrozenChunk {
    public BlockPos origin;
    public LevelRenderer.RenderChunkInfo chunkInfo = null;

    public FrozenChunk(BlockPos origin) {
        this.origin = origin;
    }
}
