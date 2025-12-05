package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CustomBuildingPlacement extends BuildingPlacement implements RangeIndicator, NightSource, NetherConvertingBuilding, GarrisonableBuilding {

    public NetherZone netherConversionZone = null;
    private final Set<BlockPos> nightBorderBps = new HashSet<>();
    private final ArrayList<BlockPos> garrisonEntries = new ArrayList<>();
    private final ArrayList<BlockPos> garrisonExits = new ArrayList<>();
    private final Random random = new Random();

    public static final List<Block> INVULNERABLE_BLOCKS = List.of(
            BlockRegistrar.GARRISON_EXIT_BLOCK.get(),
            BlockRegistrar.GARRISON_ENTRY_BLOCK.get(),
            BlockRegistrar.GARRISON_ZONE_BLOCK.get(),
            Blocks.NETHER_PORTAL,
            Blocks.LIGHT,
            Blocks.COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK
    );

    public static final List<Block> INVULNERABLE_ABOVE_BLOCKS = List.of(
            BlockRegistrar.GARRISON_ENTRY_BLOCK.get(),
            BlockRegistrar.GARRISON_ZONE_BLOCK.get()
    );

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

    // NightSource
    @Override
    public int getNightRange() {
        return this.getCustomBuilding().nightRadius;
    }

    // RangeIndicator
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

    // GarrisonableBuilding
    @Override
    public int getAttackRange() { return getCustomBuilding().garrisonRange; }

    @Override
    public int getExternalAttackRangeBonus() { return Math.min(15, getCustomBuilding().garrisonRange / 2); }

    @Override
    public int getCapacity() { return getCustomBuilding().garrisonCapacity; }

    @Override
    public BlockPos getEntryPosition() {
        if (!garrisonEntries.isEmpty()) {
            return garrisonEntries.get(random.nextInt(garrisonEntries.size())).above();
        }
        return null;
    }

    @Override
    public BlockPos getExitPosition() {
        if (!garrisonExits.isEmpty()) {
            return garrisonExits.get(random.nextInt(garrisonExits.size())).above();
        }
        return null;
    }

    @Override
    public boolean canDestroyBlock(BlockPos relativeBp) {
        if (getCapacity() <= 0)
            return true;
        BlockPos worldBp = relativeBp.offset(this.originPos);
        Block block = this.getLevel().getBlockState(worldBp).getBlock();
        BlockPos worldBpAbove = relativeBp.offset(this.originPos.above());
        Block blockAbove = this.getLevel().getBlockState(worldBpAbove).getBlock();
        return !INVULNERABLE_BLOCKS.contains(block) && !INVULNERABLE_ABOVE_BLOCKS.contains(blockAbove);
    }
}
