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

public class CustomBuildingPlacement extends BuildingPlacement implements RangeIndicator, NightSource, NetherConvertingBuilding {

    public NetherZone netherConversionZone = null;
    private final Set<BlockPos> nightBorderBps = new HashSet<>();
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

    public CustomBuilding getCustomBuilding() {
        return (CustomBuilding) this.getBuilding();
    }

    // NetherConvertingBuilding
    @Override public double getMaxNetherRange() { return this.getCustomBuilding().netherRadius; }
    @Override public double getStartingNetherRange() { return 3; }

    @Override
    public void onBuilt() {
        super.onBuilt();
        updateHighlightBps();
        if (getMaxNetherRange() > 0)
            setNetherZone(new NetherZone(new BlockPos(centrePos.getX(), originPos.getY() + 1, centrePos.getZ()), getMaxNetherRange(), getStartingNetherRange()), true);
    }

    @Nullable
    @Override
    public NetherZone getNetherZone() {
        if (this.getCustomBuilding().netherRadius > 0)
            return netherConversionZone;
        return null;
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

    // NightSource
    @Override
    public int getNightRange() {
        return this.isBuilt ? this.getCustomBuilding().nightRadius : 0;
    }

    // RangeIndicator
    @Override
    public void updateHighlightBps() {
        if (!level.isClientSide() || this.getNightRange() <= 0) {
            return;
        }
        this.nightBorderBps.clear();
        this.nightBorderBps.addAll(MiscUtil.getRangeIndicatorCircleBlocks(centrePos,
                getNightRange() - BlockClientEvents.VISIBLE_BORDER_ADJ,
                level, true
        ));
    }

    @Override
    public Set<BlockPos> getHighlightBps() {
        return nightBorderBps;
    }

    @Override
    public boolean showOnlyWhenSelected() {
        return false;
    }
}
