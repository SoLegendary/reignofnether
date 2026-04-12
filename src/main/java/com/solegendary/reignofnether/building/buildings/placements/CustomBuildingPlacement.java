package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CustomBuildingPlacement extends BuildingPlacement {
    public final ArrayList<BlockPos> garrisonEntries = new ArrayList<>();
    public final ArrayList<BlockPos> garrisonExits = new ArrayList<>();

    public CustomBuildingPlacement(CustomBuilding customBuilding, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(customBuilding, level, originPos, rotation, ownerName, blocks, isCapitol);

        for (BuildingBlock bb : blocks) {
            if (bb.getBlockState().getBlock() == BlockRegistrar.GARRISON_ENTRY_BLOCK.get()) {
                garrisonEntries.add(bb.getBlockPos());
            } else if (bb.getBlockState().getBlock() == BlockRegistrar.GARRISON_EXIT_BLOCK.get()) {
                garrisonExits.add(bb.getBlockPos());
            }
        }
    }
}
