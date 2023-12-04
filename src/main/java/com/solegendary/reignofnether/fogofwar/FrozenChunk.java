package com.solegendary.reignofnether.fogofwar;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;

public class FrozenChunk {
    public BlockPos origin;
    public LevelRenderer.RenderChunkInfo chunkInfo = null;

    public FrozenChunk(BlockPos origin) {
        this.origin = origin;
    }

    public FrozenChunk(LevelRenderer.RenderChunkInfo chunkInfo) {
        this.chunkInfo = chunkInfo;
        this.origin = chunkInfo.chunk.getOrigin();
    }
}
