package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.Building;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

// FrozenChunks are (16x16x16) chunks created when any serverside block placement happens in
// a dark chunk and should not be seen on clients - eg. building placement by an opponent.
// When this is reversed, the FrozenChunk is destroyed - eg. building is destroyed
//
// Upon creation, a snapshot of the clientside blocks in the chunk are recorded and whenever the chunk is loaded
// clientside (RenderChunk loading, not ChunkEvent.Load) these blocks are checked and synced to always match this initial state (loadBlocks)
//
// When the chunk is explored: syncServerBlocks()
// When the chunk is unexplored: saveBlocks()
//
public class FrozenChunk {

    public BlockPos origin;
    public ArrayList<Pair<BlockPos, BlockState>> blocks = new ArrayList<>();
    public Building building;

    private static final Minecraft MC = Minecraft.getInstance();

    public FrozenChunk(BlockPos origin, Building building) {
        this.origin = origin;
        this.building = building;
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
        for (int x = 0; x <= 16; x++) {
            for (int y = 0; y <= 16; y++) {
                for (int z = 0; z <= 16; z++) {
                    BlockPos bp = origin.offset(x,y,z);
                    BlockState bs = MC.level.getBlockState(bp);
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
