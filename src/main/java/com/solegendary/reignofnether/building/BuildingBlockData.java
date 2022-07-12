package com.solegendary.reignofnether.building;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class BuildingBlockData {

    public static ArrayList<BlockState> VILLAGER_HOUSE_PALETTE = null;
    public static ArrayList<BuildingBlock> VILLAGER_HOUSE_BLOCKS = null;
    public static ArrayList<BlockState> VILLAGER_TOWER_PALETTE = null;
    public static ArrayList<BuildingBlock> VILLAGER_TOWER_BLOCKS = null;

    public static void initBlockData(Minecraft MC) {
        CompoundTag villagerHouseNbt = getBuildingNbt(MC, "villager_house");
        VILLAGER_HOUSE_PALETTE = getBuildingPalette(villagerHouseNbt);
        VILLAGER_HOUSE_BLOCKS = getBuildingBlocks(villagerHouseNbt);
        CompoundTag villagerTowerNbt = getBuildingNbt(MC, "villager_tower");
        VILLAGER_TOWER_PALETTE = getBuildingPalette(villagerTowerNbt);
        VILLAGER_TOWER_BLOCKS = getBuildingBlocks(villagerTowerNbt);
    }

    public static CompoundTag getBuildingNbt(Minecraft MC, String structureName) {
        try {
            ResourceLocation rl = new ResourceLocation("reignofnether", "structures/" + structureName + ".nbt");
            Resource rs = MC.resourceManager.getResource(rl);
            return NbtIo.readCompressed(rs.getInputStream());
        }
        catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public static ArrayList<BlockState> getBuildingPalette(CompoundTag nbt) {
        ArrayList<BlockState> palette = new ArrayList<>();
        // load in palette (list of unique blockstates)
        ListTag paletteNbt = nbt.getList("palette", 10);
        for(int i = 0; i < paletteNbt.size(); i++)
            palette.add(NbtUtils.readBlockState(paletteNbt.getCompound(i)));
        return palette;
    }

    public static ArrayList<BuildingBlock> getBuildingBlocks(CompoundTag nbt) {
        ArrayList<BuildingBlock> blocks = new ArrayList<>();

        // load in blocks (list of blockPos and their palette index)
        ListTag blocksNbt = nbt.getList("blocks", 10);

        for(int i = 0; i < blocksNbt.size(); i++) {
            CompoundTag blockNbt = blocksNbt.getCompound(i);
            ListTag blockPosNbt = blockNbt.getList("pos", 3);

            blocks.add(new BuildingBlock(
                    new BlockPos(
                            blockPosNbt.getInt(0),
                            blockPosNbt.getInt(1),
                            blockPosNbt.getInt(2)
                    ),
                    blockNbt.getInt("state")
            ));
        }
        return blocks;
    }

    public static BuildingBlock getBuildingBlockByPos(ArrayList<BuildingBlock> blocks, BlockPos bp) {
        List<BuildingBlock> results = blocks.stream().filter(b -> b.blockPos.equals(bp)).toList();
        if (results.size() > 0)
            return results.get(0);
        else
            return null;
    }
}
