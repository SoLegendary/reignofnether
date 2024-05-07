package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.nether.NetherBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
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
    public boolean removeOnExplore = false;
    public boolean attemptedUnloadedSave = false;
    public boolean hasFakeBlocks = false;

    private boolean saveFakeBuildingBlocks = false;

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

                    // if we tried to save this previously but it was unloaded, so manually replace certain blocks:
                    // 1. Scaffolding -> Air
                    // 2. Magma/Cobble -> Coal Ore
                    // 3. Nether Blocks -> Overworld equivalent
                    // 4. Blocks that are a part of the parent building -> Air
                    if (attemptedUnloadedSave) {
                        saveFakeBuildingBlocks = true;
                        BlockState bs = MC.level.getBlockState(bp);
                        String blockName = bs.getBlock().getName().getString().toLowerCase();
                        if (blockName.equals("scaffolding")) {
                            blocks.add(new Pair<>(bp, Blocks.AIR.defaultBlockState()));
                        } else if (blockName.equals("magma_block") ||
                                blockName.equals("cobblestone") ) {
                            blocks.add(new Pair<>(bp, Blocks.COAL_ORE.defaultBlockState()));
                        } else if (blockName.equals("dirt")) {
                            blocks.add(new Pair<>(bp, Blocks.GRASS_BLOCK.defaultBlockState()));
                        } else if (blockName.equals("obsidian")) {
                            blocks.add(new Pair<>(bp, Blocks.WATER.defaultBlockState()));
                        } else if (NetherBlocks.isNetherBlock(MC.level, bp)) {
                            BlockState overworldBs = NetherBlocks.getOverworldBlock(MC.level, bp);
                            if (overworldBs != null)
                                blocks.add(new Pair<>(bp, overworldBs));
                        }
                        hasFakeBlocks = true;
                    } else {
                        BlockState bs = MC.level.getBlockState(bp);
                        blocks.add(new Pair<>(bp, MC.level.getBlockState(bp)));
                        hasFakeBlocks = false;
                    }
                }
            }
        }
        // do this outside the loop to avoid nesting loops
        if (saveFakeBuildingBlocks) {
            for (BuildingBlock bb : building.getBlocks())
                blocks.add(new Pair<>(bb.getBlockPos(), Blocks.AIR.defaultBlockState()));
            saveFakeBuildingBlocks = false;
        }
        if (this.blocks.isEmpty()) {
            attemptedUnloadedSave = true;
            System.out.println("attempted unloaded save at: " + origin);
        } else {
            attemptedUnloadedSave = false;
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

    private boolean isFullyLoaded() {
        if (MC.level == null)
            return false;
        for (int x = 0; x <= 16; x++)
            for (int y = 0; y <= 16; y++)
                for (int z = 0; z <= 16; z++)
                    if (!MC.level.isLoaded(origin.offset(x,y,z)))
                        return false;
        return true;
    }
}
