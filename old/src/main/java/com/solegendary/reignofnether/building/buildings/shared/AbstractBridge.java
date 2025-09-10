package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.BridgePlacement;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public abstract class AbstractBridge extends Building {
    public final float MELEE_DAMAGE_MULTIPLIER = 0.05f;

    public AbstractBridge(ResourceCost cost) {
        super("", cost, false);
    }

    @Override
    public float getMeleeDamageMult() { return MELEE_DAMAGE_MULTIPLIER; }

    public ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level, boolean diagonal) {
        return BuildingBlockData.getBuildingBlocksFromNbt(diagonal ? getDiagonalStructureName() : getOrthogonalStructureName(), level);
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return createBuildingPlacement(level, pos, rotation, ownerName, false);
    }

    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName, boolean diagonal) {
        return new BridgePlacement(this, level, pos, rotation, "", getCulledBlocks(getAbsoluteBlockData(getRelativeBlockData(level, diagonal), level, pos, rotation), level), isCapitol, diagonal);
    }

    public abstract String getDiagonalStructureName();
    public abstract String getOrthogonalStructureName();
}
