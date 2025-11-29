package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CustomBuildingPlacement extends BuildingPlacement implements RangeIndicator, NightSource, NetherConvertingBuilding {

    public NetherZone netherConversionZone = null;
    private final Set<BlockPos> nightBorderBps = new HashSet<>();

    public CustomBuildingPlacement(CustomBuilding customBuilding, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(customBuilding, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    public CustomBuilding getCustomBuilding() {
        return (CustomBuilding) this.getBuilding();
    }

    @Override public double getMaxNetherRange() { return this.getCustomBuilding().netherRadius; }
    @Override public double getStartingNetherRange() { return 3; }

    @Override
    public void onBuilt() {
        super.onBuilt();
        updateBorderBps();
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

    @Override
    public int getNightRange() {
        return this.getCustomBuilding().nightRadius;
    }

    @Override
    public void updateBorderBps() {
        if (!level.isClientSide() || this.getNightRange() <= 0) {
            return;
        }
        this.nightBorderBps.clear();
        this.nightBorderBps.addAll(MiscUtil.getRangeIndicatorCircleBlocks(centrePos,
                getNightRange() - TimeClientEvents.VISIBLE_BORDER_ADJ,
                level
        ));
    }

    @Override
    public Set<BlockPos> getBorderBps() {
        return nightBorderBps;
    }

    @Override
    public boolean showOnlyWhenSelected() {
        return false;
    }
}
