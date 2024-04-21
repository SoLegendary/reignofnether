package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

// FrozenChunks are (8x8x8) chunks created when any serverside block placement happens in a dark chunk and should not
// be seen on clients - eg. building placement by an opponent.
// When this is reversed, the FrozenChunk is destroyed - eg. building is destroyed
//
// Upon creation, a snapshot of the clientside blocks in the chunk are recorded
// whenever the chunk is loaded clientside (ChunkEvent.Load) these blocks are checked and synced to always match this initial state
// TODO: this needs to happen BEFORE the chunk is put into semifrozen state
//
// When the chunk is explored:
// 1. The periodic clientside syncing is paused
// 2. A request is made to instead sync the blocks with the server
//
// When the chunk is unexplored:
// 1. The snapshot of the chunk blocks is updated
// 2. The periodic clientside syncing is resumed
//
public class FrozenChunk {
    public BlockPos origin;
    public ArrayList<Pair<BlockPos, BlockState>> blocks;

    public FrozenChunk(BlockPos origin) {
        this.origin = origin;
    }
}
