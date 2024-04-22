package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

// FrozenChunks are (16x16x16) chunks created when any serverside block placement happens in
// a dark chunk and should not be seen on clients - eg. building placement by an opponent.
// When this is reversed, the FrozenChunk is destroyed - eg. building is destroyed
//
// Upon creation, a snapshot of the clientside blocks in the chunk are recorded and whenever the chunk is loaded
// clientside (ChunkEvent.Load) these blocks are checked and synced to always match this initial state (loadBlocks)
// TODO: this needs to happen BEFORE the chunk is put into semifrozen state
//
// When the chunk is explored: syncServerBlocks()
// When the chunk is unexplored: saveBlocks()
//
public class FrozenChunk {
    public BlockPos origin;
    public ArrayList<Pair<BlockPos, BlockState>> blocks = new ArrayList<>();

    private static final Minecraft MC = Minecraft.getInstance();

    public FrozenChunk(BlockPos origin) {
        this.origin = origin;
        saveBlocks();
    }

    // Match ClientLevel blocks with ServerLevel blocks
    public void syncServerBlocks(BlockPos renderChunkOrigin) {
        FrozenChunkServerboundPacket.syncServerBlocks(renderChunkOrigin);
    }

    // saves the ClientLevel blocks into this.blocks
    public void saveBlocks() {
        if (MC.level == null)
            return;
        for (int x = origin.getX(); x <= origin.getX(); x += 16) {
            for (int y = origin.getY(); y <= origin.getY(); y += 16) {
                for (int z = origin.getZ(); z <= origin.getZ(); z += 16) {
                    BlockPos bp = origin.offset(x,y,z);
                    blocks.add(new Pair<>(bp, MC.level.getBlockState(bp)));
                }
            }
        }
    }

    // updates the ClientLevel with the blocks saved
    public void loadBlocks() {
        if (MC.level == null)
            return;
        for (Pair<BlockPos, BlockState> pair : blocks) {
            MC.level.setBlockAndUpdate(pair.getFirst(), pair.getSecond());
        }
    }
}
