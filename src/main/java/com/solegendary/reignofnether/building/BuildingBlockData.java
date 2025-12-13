package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.resources.BlockUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// a class for static functions related to reading building NBT data (as created by Structure Blocks)

public class BuildingBlockData {

    public static ArrayList<BuildingBlock> getBuildingBlocksFromNbt(String structureName, LevelAccessor level) {
        ResourceManager resourceManager;
        if (level.isClientSide())
            resourceManager = Minecraft.getInstance().getResourceManager();
        else
            resourceManager = level.getServer().getResourceManager();

        CompoundTag nbt = getBuildingNbt(structureName, resourceManager);

        return getBuildingBlocksFromNbt(nbt);
    }

    public static ArrayList<BuildingBlock> getBuildingBlocksFromNbt(CompoundTag nbt) {
        ArrayList<BuildingBlock> blocks = new ArrayList<>();

        // load in blocks (list of blockPos and their palette index)
        ListTag blocksNbt = nbt.getList("blocks", 10);

        ArrayList<BlockState> palette = getBuildingPalette(nbt);

        for(int i = 0; i < blocksNbt.size(); i++) {
            CompoundTag blockNbt = blocksNbt.getCompound(i);
            ListTag blockPosNbt = blockNbt.getList("pos", 3);

            BlockPos bp = new BlockPos(
                    blockPosNbt.getInt(0),
                    blockPosNbt.getInt(1),
                    blockPosNbt.getInt(2)
            );
            BlockState bs = palette.get(blockNbt.getInt("state"));
            CompoundTag bNbt = null;
            if (blockNbt.contains("nbt")) {
                bNbt = blockNbt.getCompound("nbt");
            }
            if (BlockUtils.isFallingLogBlock(bs))
                bs = BlockUtils.getNonFallingLog(bs);

            if (bs.getBlock() != Blocks.WATER || bs.getFluidState().isSource())
                blocks.add(new BuildingBlock(bp, bs, bNbt));
        }
        return blocks;
    }

    public static CompoundTag getBuildingNbt(String structureName, ResourceManager resManager) {

        try {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("reignofnether", "structures/" + structureName + ".nbt");
            Optional<Resource> rs = resManager.getResource(rl);
            if (rs.isEmpty()) return null;
            return NbtIo.readCompressed(rs.get().open());
        } catch (IOException e) {
            ReignOfNether.LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    public static ArrayList<BlockState> getBuildingPalette(CompoundTag nbt) {
        ArrayList<BlockState> palette = new ArrayList<>();
        // load in palette (list of unique block states)
        ListTag paletteNbt = nbt.getList("palette", 10);
        for(int i = 0; i < paletteNbt.size(); i++) {
            palette.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), paletteNbt.getCompound(i)));
        }
        return palette;
    }

    public static BuildingBlock getBuildingBlockByPos(ArrayList<BuildingBlock> blocks, BlockPos bp) {
        for (BuildingBlock block : blocks) {
            if (block.getBlockPos().equals(bp)) return block;
        }
        return null;
    }
}
