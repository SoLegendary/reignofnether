package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;

public class CentralPortalPlacement extends ProductionPlacement implements NetherConvertingBuilding {
    public NetherZone netherConversionZone = null;

    public CentralPortalPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    @Override public double getMaxNetherRange() { return 30; }
    @Override public double getStartingNetherRange() { return 6; }
    @Override public NetherZone getNetherZone() { return netherConversionZone; }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        if (!this.getLevel().isClientSide() && this.getBlocksPlaced() >= getBlocksTotal()) {
            BlockPos bp;
            if (this.rotation == Rotation.CLOCKWISE_90 ||
                    this.rotation == Rotation.COUNTERCLOCKWISE_90) {
                bp = this.centrePos.offset(0,-1,0);
            } else {
                bp = this.centrePos.offset(-1,0,0);
            }
            if (this.getLevel().getBlockState(bp).isAir())
                this.getLevel().setBlockAndUpdate(bp, Blocks.FIRE.defaultBlockState());
        }
    }

    @Override
    public void setNetherZone(NetherZone nz, boolean save) {
        if (netherConversionZone == null) {
            netherConversionZone = nz;
            if (!level.isClientSide()) {
                BuildingServerEvents.netherZones.add(netherConversionZone);
                if (save)
                    BuildingServerEvents.saveNetherZones((ServerLevel) level);
            }
        }
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        if (getMaxNetherRange() > 0)
            setNetherZone(new NetherZone(centrePos.offset(0,-6,0), getMaxNetherRange(), getStartingNetherRange()), true);
    }

    @Override
    public boolean canDestroyBlock(BlockPos relativeBp) {
        BlockPos worldBp = relativeBp.offset(this.originPos);
        Block block = this.getLevel().getBlockState(worldBp).getBlock();
        return block != Blocks.OBSIDIAN && block != Blocks.NETHER_PORTAL;
    }
}
