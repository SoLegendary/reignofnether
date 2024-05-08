package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
import com.solegendary.reignofnether.nether.NetherBlocks;
import com.solegendary.reignofnether.sounds.SoundClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

import java.util.ArrayList;
import java.util.List;

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
    public boolean hasFakeBlocks = false;

    private static final Minecraft MC = Minecraft.getInstance();

    public FrozenChunk(BlockPos origin, Building building) {
        this.origin = origin;
        this.building = building;
        if (isFullyLoaded())
            saveBlocks();
    }

    // Match ClientLevel blocks with ServerLevel blocks
    // need to mute any plant or portal locations as they will be broken and replaced
    public void syncServerBlocks(BlockPos renderChunkOrigin) {
        if (MC.level != null) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos bp = renderChunkOrigin.offset(x,y,z);
                        BlockState bs = MC.level.getBlockState(bp);
                        if (bs.getMaterial() == Material.PORTAL ||
                            bs.getMaterial() == Material.PLANT ||
                            bs.getMaterial() == Material.REPLACEABLE_PLANT) {
                            SoundClientEvents.mutedBps.add(bp);
                        }
                    }
                }
            }
        }
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
        hasFakeBlocks = false;
    }

    // Like saveBlocks() but replace certain blocks to obscure the real state:
    // 1. Scaffolding -> Air
    // 2. Magma/Cobble -> Coal Ore
    // 3. Nether Blocks -> Overworld equivalent
    // 4. Blocks that are a part of the parent building -> Air
    public void saveFakeBlocks() {
        if (MC.level == null)
            return;

        ArrayList<BuildingBlock> bbs = new ArrayList<>();
        for (BuildingBlock bb : building.getBlocks())
            if (bb.getBlockPos().getX() >= origin.getX() && bb.getBlockPos().getX() < origin.getX() + 16 &&
                bb.getBlockPos().getY() >= origin.getY() && bb.getBlockPos().getY() < origin.getY() + 16 &&
                bb.getBlockPos().getZ() >= origin.getZ() && bb.getBlockPos().getZ() < origin.getZ() + 16 &&
                !bb.getBlockState().isAir())
                bbs.add(bb);

        for (int x = 0; x <= 16; x++) {
            for (int y = 0; y <= 16; y++) {
                outerloop:
                for (int z = 0; z <= 16; z++) {
                    BlockPos bp = origin.offset(x,y,z);
                    BlockState bs = MC.level.getBlockState(bp);

                    for (BuildingBlock bb : bbs) {
                        if (bb.getBlockPos().equals(bp)) {
                            blocks.add(new Pair<>(bb.getBlockPos(), Blocks.AIR.defaultBlockState()));
                            continue outerloop;
                        }
                    }
                    String blockName = bs.getBlock().getName().getString().toLowerCase();
                    if (blockName.equals("scaffolding")) {
                        blocks.add(new Pair<>(bp, Blocks.AIR.defaultBlockState()));
                    } else if (blockName.equals("magma_block") ||
                            blockName.equals("cobblestone")) {
                        blocks.add(new Pair<>(bp, Blocks.COAL_ORE.defaultBlockState()));
                    } else if (blockName.equals("dirt") ||
                            blockName.equals("netherrack")) {
                        blocks.add(new Pair<>(bp, Blocks.GRASS_BLOCK.defaultBlockState()));
                    } else if (blockName.equals("obsidian")) {
                        blocks.add(new Pair<>(bp, Blocks.WATER.defaultBlockState()));
                    } else if (NetherBlocks.isNetherBlock(MC.level, bp) ||
                            NetherBlocks.isNetherPlantBlock(MC.level, bp)) {
                        BlockState overworldBs = NetherBlocks.getOverworldBlock(MC.level, bp);
                        if (overworldBs != null)
                            blocks.add(new Pair<>(bp, overworldBs));
                    }
                }
            }
        }
        hasFakeBlocks = true;
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
