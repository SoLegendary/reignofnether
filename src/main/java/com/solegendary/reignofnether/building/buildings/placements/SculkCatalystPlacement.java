package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.Sacrifice;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.monsters.SculkCatalyst;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

import static com.solegendary.reignofnether.building.BuildingUtils.isPosInsideAnyBuilding;

public class SculkCatalystPlacement extends BuildingPlacement implements RangeIndicator, NightSource {
    private final static Random random = new Random();
    private final Set<BlockPos> nightBorderBps = new HashSet<>();

    private final static int SCULK_SEARCH_RANGE = 30;
    private final static float HP_PER_SCULK = 0.5f;
    private final static float RANGE_PER_SCULK = 0.25f;

    public final ArrayList<BlockPos> sculkBps = new ArrayList<>();

    // for some reason, destroy() does not restore sculk unless restoreRandomSculk was run at least once before
    private final boolean didSculkFix = false;
    private final BlockPos sculkFixBp = null;
    public SculkCatalystPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
        Ability sacrifice = new Sacrifice(level);
        abilities.add(sacrifice);
    }

    public int getUncappedNightRange() {
        if (isBuilt || isBuiltServerside) {
            return (int) (sculkBps.size() * RANGE_PER_SCULK) + SculkCatalyst.nightRangeMin;
        }
        return 0;
    }

    public int getRange() {
        if (isBuilt || isBuiltServerside) {
            return (int) Math.min(SculkCatalyst.nightRangeMin + (sculkBps.size() * RANGE_PER_SCULK), SculkCatalyst.nightRangeMax);
        }
        return 0;
    }

    @Override
    public int getNightRange() {
        return getRange();
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        updateBorderBps();
        updateSculkBps();
    }

    @Override
    public void updateBorderBps() {
        if (!level.isClientSide()) {
            return;
        }
        updateSculkBps();
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

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        if (tickAgeAfterBuilt > 0 && tickAgeAfterBuilt % 100 == 0) {
            if (tickLevel.isClientSide()) {
                updateBorderBps();
            } else {
                updateSculkBps();
            }
        }
    }

    @Override
    public int getHealth() {
        return (int) (getBlocksPlaced() / MIN_BLOCKS_PERCENT) - getHighestBlockCountReached() + (int) (
                sculkBps.size() * HP_PER_SCULK
        );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    private void updateSculkBps() {
        sculkBps.clear();
        for (int x = centrePos.getX() - SCULK_SEARCH_RANGE / 2; x < centrePos.getX() + SCULK_SEARCH_RANGE / 2; x++) {
            for (int z = centrePos.getZ() - SCULK_SEARCH_RANGE / 2;
                 z < centrePos.getZ() + SCULK_SEARCH_RANGE / 2; z++) {
                BlockPos topBp = new BlockPos(x, maxCorner.getY(), z);
                if (isPosInsideAnyBuilding(level.isClientSide(), topBp)) {
                    continue;
                }

                int y = 0;
                BlockState bs;
                BlockPos bp;
                do {
                    y += 1;
                    bp = topBp.offset(0, -y, 0);
                    bs = level.getBlockState(bp);
                } while (bs.isAir() && y < 10);

                if (isSculk(bs.getBlock())) {
                    sculkBps.add(bp);
                }
            }
        }
        Collections.shuffle(sculkBps);
    }

    public static boolean isSculk(Block block) {
        return block == Blocks.SCULK || block == Blocks.SCULK_VEIN || block == Blocks.SCULK_CATALYST || block == Blocks.SCULK_SENSOR || block == Blocks.SCULK_SHRIEKER || block == Blocks.CALIBRATED_SCULK_SENSOR;
    }

    private static final int destroys = 0;

    @Override
    public void destroy(ServerLevel serverLevel) {
        super.destroy(serverLevel);

        if (isBuilt) {
            updateSculkBps();
            int i = 0;
            while (sculkBps.size() > 0 && i < 10) {
                restoreRandomSculk(100);
                i += 1;
            }
        }
    }

    // returns the number of blocks converted
    private int restoreRandomSculk(int amount) {
        if (getLevel().isClientSide()) {
            return 0;
        }
        int restoredSculk = 0;
        updateSculkBps();

        for (int i = 0; i < amount; i++) {
            BlockPos bp;
            BlockState bs;

            if (i >= sculkBps.size()) {
                return restoredSculk;
            }

            bp = sculkBps.get(i);
            bs = level.getBlockState(bp);

            if (bs.getBlock() == Blocks.SCULK) {
                for (BlockPos bpAdj : List.of(bp.below(), bp.north(), bp.south(), bp.east(), bp.west())) {
                    BlockState bsAdj = level.getBlockState(bpAdj);
                    if (!bsAdj.isAir() && !isSculk(bsAdj.getBlock())) {
                        level.setBlockAndUpdate(bp, bsAdj);
                        restoredSculk += 1;
                        break;
                    }
                }
            } else if (bs.getBlock() == Blocks.SCULK_VEIN || bs.getBlock() == Blocks.SCULK_SENSOR) {
                level.destroyBlock(bp, false);
                restoredSculk += 1;
            }
        }
        return restoredSculk;
    }

    public void destroyRandomBlocks(int amount) {
        if (getLevel().isClientSide() || amount <= 0) {
            return;
        }

        int restoredSculk = restoreRandomSculk((int) (amount / HP_PER_SCULK));
        if (restoredSculk < amount) {
            super.destroyRandomBlocks(amount - restoredSculk);
        }
    }
}
