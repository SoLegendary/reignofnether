package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.ability.heroAbilities.enchanter.CivilEnchantment;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FarmPlacement extends BuildingPlacement {
    private class FarmCropBlock {
        public int tickAge = 0;
        public BlockPos bp;
        public FarmCropBlock(BlockPos bp) {
            this.bp = bp;
        }
    }
    private final ArrayList<FarmCropBlock> farmBlocks = new ArrayList<>();
    private static final int TICK_CROPS_INTERVAL = 10;
    private static final int TPS = ResourceCost.TICKS_PER_SECOND;
    private static final int STEM_GROW_TICKS = 13 * TPS; // (91s maturity) ticks to grow 1/8 stages (includes pumpkins and melons)
    private static final int GOURD_GROW_TICKS = 45 * TPS; // ticks to grow a gourd once we have a grown stem
    private static final int CROP_GROW_TICKS = 13 * TPS; // (91s maturity) ticks to grow 1/8 stages (includes wheat, potato, beetroot, etc.)
    private static final int NETHER_WART_GROW_TICKS = 35 * TPS; // (105s maturity) ticks to grow 1/4 stages

    private static final int ICE_CHECK_TICKS_MAX = 100;
    private int ticksToNextIceCheck = ICE_CHECK_TICKS_MAX;
    public FarmPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);

        for (BuildingBlock bb : blocks) {
            if (List.of(Blocks.FARMLAND, Blocks.SOUL_SAND)
                    .contains(bb.getBlockState().getBlock())) {
                farmBlocks.add(new FarmCropBlock(bb.getBlockPos().above()));
            }
        }
    }

    @Nullable
    private WorkerUnit getWorker() {
        for (LivingEntity le : UnitServerEvents.getAllUnits()) {
            if (le instanceof WorkerUnit workerUnit && workerUnit.getGatherResourceGoal().getTargetFarm() == this) {
                return workerUnit;
            }
        }
        return null;
    }

    // tick crop growth here to have precise control over growth speed with no RNG
    private void tickCrops() {
        for (FarmCropBlock farmBlock : farmBlocks) {

            BlockState bs = level.getBlockState(farmBlock.bp);
            Block block = bs.getBlock();

            int ticksToIncrement = TICK_CROPS_INTERVAL;
            if (getWorker() != null) {
                ticksToIncrement = (int) (TICK_CROPS_INTERVAL * CivilEnchantment.getEfficiencyMultiplier(getWorker()));
            }

            if (block instanceof CropBlock cropBlock && (cropBlock == Blocks.WHEAT || cropBlock == Blocks.CARROTS || cropBlock == Blocks.POTATOES)) {
                farmBlock.tickAge += ticksToIncrement;
                int age = bs.getValue(BlockStateProperties.AGE_7);
                if (farmBlock.tickAge >= CROP_GROW_TICKS) {
                    int newAge = Math.min(7, age + 1);
                    BlockState grownState = block.defaultBlockState().setValue(BlockStateProperties.AGE_7, newAge);
                    level.setBlock(farmBlock.bp, grownState, 2);
                    farmBlock.tickAge = 0;
                }
            }
            else if (block instanceof StemBlock stemBlock) {
                farmBlock.tickAge += ticksToIncrement;
                int age = bs.getValue(BlockStateProperties.AGE_7);
                if (age >= 7 && farmBlock.tickAge >= GOURD_GROW_TICKS) {
                    growGourd(stemBlock, farmBlock.bp);
                    farmBlock.tickAge = 0;
                } else if (age < 7 && farmBlock.tickAge >= STEM_GROW_TICKS) {
                    int newAge = Math.min(7, age + 1);
                    BlockState grownState = block.defaultBlockState().setValue(BlockStateProperties.AGE_7, newAge);
                    level.setBlock(farmBlock.bp, grownState, 2);
                    farmBlock.tickAge = 0;
                }
            }
            else if (block instanceof NetherWartBlock) {
                farmBlock.tickAge += ticksToIncrement;
                int age = bs.getValue(BlockStateProperties.AGE_3);
                if (farmBlock.tickAge >= NETHER_WART_GROW_TICKS) {
                    int newAge = Math.min(3, age + 1);
                    BlockState grownState = block.defaultBlockState().setValue(BlockStateProperties.AGE_3, newAge);
                    level.setBlock(farmBlock.bp, grownState, 2);
                    farmBlock.tickAge = 0;
                }
            }
        }
    }

    private void growGourd(StemBlock stemBlock, BlockPos bp) {
        ArrayList<Direction> dirs = new ArrayList<>(List.of(
                Direction.NORTH,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST
        ));
        Collections.shuffle(dirs);
        for (Direction dir : dirs) {
            BlockPos bpAdj = bp.relative(dir);
            BlockState bs = level.getBlockState(bpAdj.below());
            if (level.isEmptyBlock(bpAdj) && (bs.canSustainPlant(level, bpAdj.below(), Direction.UP, stemBlock.getFruit()) || bs.is(Blocks.FARMLAND) || bs.is(BlockTags.DIRT))) {
                level.setBlockAndUpdate(bpAdj, stemBlock.getFruit().defaultBlockState());
                level.setBlockAndUpdate(bp, stemBlock.getFruit().getAttachedStem().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, dir));
                return;
            }
        }
    }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);
        if (!tickLevel.isClientSide()) {
            ticksToNextIceCheck -= 1;
            if (ticksToNextIceCheck <= 0) {
                for (BuildingBlock bb : blocks)
                    if (tickLevel.getBlockState(bb.getBlockPos()).getBlock() == Blocks.ICE)
                        tickLevel.setBlockAndUpdate(bb.getBlockPos(), Blocks.WATER.defaultBlockState());
                ticksToNextIceCheck = ICE_CHECK_TICKS_MAX;
            }
        }
        if (!level.isClientSide() && tickAge % TICK_CROPS_INTERVAL == 0)
            tickCrops();
    }

    @Override
    public void destroy(ServerLevel serverLevel) {
        super.destroy(serverLevel);
        for (int x = minCorner.getX(); x <= maxCorner.getX(); x++) {
            for (int y = minCorner.getY(); y <= maxCorner.getY(); y++) {
                for (int z = minCorner.getZ(); z <= maxCorner.getZ(); z++) {
                    BlockPos bp = new BlockPos(x,y,z);
                    BlockState bs = serverLevel.getBlockState(bp);
                    Block block = bs.getBlock();
                    if (bs.getTags().toList().contains(BlockTags.CROPS) || block == Blocks.MELON || block == Blocks.PUMPKIN)
                        serverLevel.destroyBlock(bp, false);
                }
            }
        }
    }
}
