package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.Sacrifice;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.NightSourceAddon;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.building.buildings.monsters.SculkCatalyst;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

import static com.solegendary.reignofnether.building.BuildingUtils.isPosInsideAnyBuilding;

public class SculkCatalystPlacement extends BuildingPlacement {

    //private final Set<BlockPos> nightBorderBps = new HashSet<>();

    private final static int SCULK_SEARCH_RANGE = 30;
    private final static double HP_PER_SCULK = 1.0f;
    public final static float RANGE_PER_SCULK = 0.30f;

    public String autoSacrificeUnitType = "";

    public final ArrayList<BlockPos> sculkBps = new ArrayList<>();

    // for some reason, destroy() does not restore sculk unless restoreRandomSculk was run at least once before
    public SculkCatalystPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    public int getUncappedNightRange() {
        if (isBuilt || isBuiltServerside) {
            return (int) (getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE).size() * RANGE_PER_SCULK) + SculkCatalyst.MIN_NIGHT_RANGE;
        }
        return 0;
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        RangeIndicatorAddon ria;
        if ((ria = getBuilding().getActiveAddon(RangeIndicatorAddon.class)) != null) {
            ria.updateHighlightBps(this);
        }
        updateSculkBps();
    }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        if (tickAgeAfterBuilt > 0) {
            if (tickAgeAfterBuilt % 100 == 0) {
                if (tickLevel.isClientSide()) {
                    RangeIndicatorAddon ria;
                    if ((ria = getBuilding().getActiveAddon(RangeIndicatorAddon.class)) != null) {
                        ria.updateHighlightBps(this);
                    }
                } else {
                    updateSculkBps();
                }
            }
            if (tickAgeAfterBuilt % 30 == 0) {
                for (Ability ability : abilities) {
                    NightSourceAddon nsa;
                    if (ability instanceof Sacrifice sacrifice &&
                        sacrifice.isAutocasting(this) &&
                        (nsa = getBuilding().getActiveAddon(NightSourceAddon.class)) != null && nsa.getNightRange(this) < SculkCatalyst.MAX_NIGHT_RANGE) {
                        sacrifice.autoSacrifice(this);
                    }
                }
            }
        }
        if (tickAgeAfterBuilt % 10 == 0)
            updateButtons();
    }

    @Override
    public int getHealth() {
        return (int) ((((getBlocksPlaced() - partialBlocksDestroyed) / MIN_BLOCKS_PERCENT) - getHighestBlockCountReached() +
                (sculkBps.size() * HP_PER_SCULK)) * (getHealthPerBlock() / 2));
    }

    public void updateSculkBps() {
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

    // returns the number of blocks restored
    private double restoreRandomSculk(double amount) {
        if (getLevel().isClientSide()) {
            return 0;
        }
        int restoredSculk = 0;
        updateSculkBps();

        amount /= (HP_PER_SCULK / 2d);
        double floorAmount = Math.floor(amount);
        partialBlocksDestroyed += (amount - floorAmount);

        int intAmount = (int) floorAmount;
        if (partialBlocksDestroyed >= 1d) {
            partialBlocksDestroyed -= 1d;
            intAmount += 1;
        }

        for (int i = 0; i < intAmount; i++) {
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

    @Override
    public void destroyRandomBlocks(double amount) {
        if (!getLevel().isClientSide() && amount > 0) {
            double absorbedHp = 0;
            if (!sculkBps.isEmpty()) {
                double restoredSculk = restoreRandomSculk(amount / HP_PER_SCULK);
                absorbedHp = restoredSculk * HP_PER_SCULK;
                updateSculkBps();
            }
            double newAmount = amount - absorbedHp;
            if (newAmount > 0 && sculkBps.isEmpty()) {
                super.destroyRandomBlocks(newAmount);
            }
        }
    }
}
