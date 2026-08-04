package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.buildings.piglins.CentralPortal;
import com.solegendary.reignofnether.building.buildings.piglins.PortalBasic;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.nether.NetherBlocks;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.isBridge;

public class BuildingValidators {

    // minimum % of blocks below a building that need to be supported by a solid block for it to be placeable
    // 1 means you can't have any gaps at all, 0 means you can place buildings in mid-air
    private static final float MIN_SUPPORTED_BLOCKS_PERCENT = 0.6f;
    private static final float MIN_NETHER_BLOCKS_PERCENT = 0.8f; // piglin buildings must be build on at least 80%
    private static final int MIN_BRIDGE_SIZE = 10; // a bridge must have at least 10 blocks to be placeable
    private static final float MIN_BRIDGE_LIQUID_BLOCKS_PERCENT = 0.20f; // at least 20% of covered blocks must be liquid
    private static final float MAX_BRIDGE_LIQUID_BLOCKS_PERCENT = 0.95f; // at least 5% of covered blocks must be solid

    public static boolean isPlacementValid(Level level, Building building, BlockPos placementPos, String ownerName,
                                           boolean isDiagonalBridge, boolean isSandbox, boolean ignoreFog) {
        return getPlacementValidityError(level, building, placementPos, ownerName, isDiagonalBridge, isSandbox, ignoreFog) == null;
    }

    @Nullable
    public static String getPlacementValidityError(Level level, Building building, BlockPos originPos, String ownerName,
                                                   boolean isDiagonalBridge, boolean isSandbox, boolean ignoreFog) {
        if (level == null || building == null)
            return "Unknown error";

        ArrayList<BuildingBlock> blocks;
        if (building instanceof AbstractBridge bridge)
            blocks = bridge.getRelativeBlockData(level, isDiagonalBridge);
        else
            blocks = building.getRelativeBlockData(level); // TODO: is lack of rotation here a problem?

        if (isBuildingPlacementInAirOrOnIllegalBlocks(level, building, originPos, blocks)) {
            return "building.reignofnether.ground_not_flat";
        } else if (isBuildingPlacementClipping(level, building, originPos, blocks)) {
            return "building.reignofnether.ground_not_flat";
        } else if (isOverlappingAnyOtherBuilding(level, building, originPos, blocks) && !isSandbox) {
            return "building.reignofnether.too_close";
        } else if (!isNonPiglinOrOnNetherBlocks(level, building, originPos, blocks)) {
            return "building.reignofnether.must_be_nether";
        } else if (!isNonBridgeOrValidBridge(level, building, originPos, blocks)) {
            return "building.reignofnether.must_be_liquid";
        } else if (!isInBrightChunk(level, originPos, blocks, ownerName) && !isSandbox && !ignoreFog) {
            return "building.reignofnether.unexplored";
        } else if (!isBuildingPlacementWithinWorldBorder(level, building, originPos, blocks)) {
            return "building.reignofnether.outside_map";
        } else if (!isNotTutorialOrNearValidCapitolPosition(level, building, originPos)) {
            return "building.reignofnether.build_centre_here";
        }
        return null;
    }

    // disallow any building block from clipping into any other existing blocks
    private static boolean isBuildingPlacementClipping(Level level, Building building, BlockPos originPos, List<BuildingBlock> blocks) {
        if (level == null) {
            return false;
        }
        if (isBridge(building) || level.getGameRules().getRule(GameRuleRegistrar.SLANTED_BUILDING).get()) {
            return false;
        }

        for (BuildingBlock block : blocks) {
            BlockPos bp = block.getBlockPos().offset(originPos).offset(0, 1, 0);
            if ((level.getBlockState(bp).isSolid() || !level.getBlockState(bp).getFluidState().isEmpty()) && (block.getBlockState().isSolid() || !block.getBlockState().getFluidState().isEmpty())) {
                return true;
            }
        }
        return false;
    }

    // disallow the building borders from overlapping any other's, even if they don't collide physical blocks
    // also allow for a 1 block gap between buildings so units can spawn and stairs don't have their blockstates
    // messed up
    private static boolean isOverlappingAnyOtherBuilding(Level level, Building building, BlockPos originPos, List<BuildingBlock> blocks) {
        List<BuildingPlacement> buildings = BuildingUtils.getBuildingsList(level.isClientSide());

        BlockPos minPos = BuildingUtils.getMinCorner(blocks).offset(originPos);//.offset(-1, -1, -1);
        BlockPos maxPos = BuildingUtils.getMaxCorner(blocks).offset(originPos);//.offset(1, 1, 1);

        for (BuildingPlacement bpl : buildings) {
            for (BuildingBlock block : bpl.blocks) {
                if (isBridge(building)) {
                    continue;
                }
                BlockPos bp = block.getBlockPos();
                if (bp.getX() >= minPos.getX() && bp.getX() <= maxPos.getX() && bp.getY() >= minPos.getY()
                        && bp.getY() <= maxPos.getY() && bp.getZ() >= minPos.getZ() && bp.getZ() <= maxPos.getZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNonPiglinOrOnNetherBlocks(Level level, Building building, BlockPos originPos, List<BuildingBlock> blocks) {
        if (isBridge(building)) {
            return true;
        }
        boolean netherTerrainCustomBuilding = building instanceof CustomBuilding cb && cb.netherTerrainOnly;
        if (!netherTerrainCustomBuilding && building.getFaction() != Faction.PIGLINS || building instanceof CentralPortal) {
            return true;
        }
        if (building instanceof PortalBasic) {
            return true;
        }
        return isOnNetherBlocks(level, blocks, originPos);
    }

    public static boolean isOnNetherBlocks(Level level, List<BuildingBlock> blocks, BlockPos originPos) {
        int netherBlocksBelow = 0;
        int blocksBelow = 0;
        for (BuildingBlock block : blocks) {
            if (block.getBlockPos().getY() == 0 && level != null) {
                BlockPos bp = block.getBlockPos().offset(originPos).offset(0, 1, 0);
                BlockState bs = block.getBlockState(); // building block
                if (bs.isSolid()) {
                    blocksBelow += 1;
                    if (NetherBlocks.isNetherBlock(level, bp.below())) {
                        netherBlocksBelow += 1;
                    }
                }
            }
        }
        if (blocksBelow <= 0) {
            return false; // avoid division by 0
        }
        return ((float) netherBlocksBelow / (float) blocksBelow) > MIN_NETHER_BLOCKS_PERCENT;
    }


    // 90% all solid blocks at the base of the building must be on top of solid non-barrier blocks to be placeable
    // excluding those under blocks which aren't solid anyway
    private static boolean isBuildingPlacementInAirOrOnIllegalBlocks(Level level, Building building, BlockPos originPos, List<BuildingBlock> blocks) {
        if (isBridge(building) || level.getGameRules().getRule(GameRuleRegistrar.SLANTED_BUILDING).get()) {
            return false;
        }
        int solidBlocksBelow = 0;
        int blocksBelow = 0;
        for (BuildingBlock block : blocks) {
            if (block.getBlockPos().getY() == 0 && level != null) {
                BlockPos bp = block.getBlockPos().offset(originPos).offset(0, 1, 0);
                BlockState bs = block.getBlockState(); // building block
                BlockState bsBelow = level.getBlockState(bp.below()); // world block

                if (bs.isSolid() && !(bsBelow.getBlock() instanceof IceBlock)) {
                    blocksBelow += 1;
                    if (bsBelow.isSolid() &&
                            !(bsBelow.getBlock() instanceof LeavesBlock) &&
                            !(bsBelow.getBlock() instanceof BarrierBlock) &&
                            !(bsBelow.getBlock() instanceof SlabBlock && bsBelow.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM)) {
                        solidBlocksBelow += 1;
                    }
                }
            }
        }
        if (blocksBelow <= 0) {
            return false; // avoid division by 0
        }
        return ((float) solidBlocksBelow / (float) blocksBelow) < MIN_SUPPORTED_BLOCKS_PERCENT;
    }

    private static boolean isBuildingPlacementWithinWorldBorder(Level level, Building building, BlockPos originPos, List<BuildingBlock> blocks) {
        if (level == null || building == null)
            return false;
        if (level.getGameRules().getRule(GameRuleRegistrar.BUILDINGS_OUTSIDE_BORDER).get())
            return true;

        int minX = 999999;
        int minZ = 999999;
        int maxX = -999999;
        int maxZ = -999999;
        for (BuildingBlock block : blocks) {
            var bp = block.getBlockPos();
            if (bp.getX() < minX) {
                minX = bp.getX();
            }
            if (bp.getZ() < minZ) {
                minZ = bp.getZ();
            }
            if (bp.getX() > maxX) {
                maxX = bp.getX();
            }
            if (bp.getZ() > maxZ) {
                maxZ = bp.getZ();
            }
        }
        BlockPos minPos = BuildingUtils.getMinCorner(blocks).offset(originPos);
        BlockPos maxPos = BuildingUtils.getMaxCorner(blocks).offset(originPos);

        return level.getWorldBorder().isWithinBounds(minPos.getX(), minPos.getZ()) &&
                level.getWorldBorder().isWithinBounds(maxPos.getX(), maxPos.getZ()) &&
                level.getWorldBorder().isWithinBounds(maxPos.getX(), minPos.getZ()) &&
                level.getWorldBorder().isWithinBounds(minPos.getX(), maxPos.getZ());
    }


    // bridges should be connected to land or another bridge and be touching water
    private static boolean isNonBridgeOrValidBridge(Level level, Building building, BlockPos originPos, List<BuildingBlock> blocks) {
        if (!isBridge(building)) {
            return true;
        }

        int placeableBlocks = 0;
        for (BuildingBlock block : blocks)
            if (!AbstractBridge.shouldCullBlock(originPos.offset(0, 1, 0), block, level) && !block.getBlockState()
                    .isAir()) {
                placeableBlocks += 1;
            }
        if (placeableBlocks < MIN_BRIDGE_SIZE) {
            return false;
        }

        int bridgeBlocks = 0;
        int waterBlocksClipping = 0;
        for (BuildingBlock block : blocks) {
            if (block.getBlockState().isAir()) {
                continue;
            }
            if (level != null) {
                BlockPos bp = block.getBlockPos().offset(originPos).offset(0, 1, 0);
                BlockState bs = block.getBlockState(); // building block
                BlockState bsWorld = level.getBlockState(bp); // world block

                // top y level should not be touching any water at all
                if (block.getBlockPos().getY() == 1) {
                    if ((bs.getBlock() instanceof FenceBlock) && !bsWorld.getFluidState().isEmpty()) {
                        return false;
                    }
                }

                if (block.getBlockPos().getY() == 0) {
                    bridgeBlocks += 1;
                    if (!bsWorld.getFluidState().isEmpty() || bsWorld.getBlock() instanceof SeagrassBlock
                            || bsWorld.getBlock() instanceof KelpBlock) {
                        waterBlocksClipping += 1;
                    }
                }
            }
        }
        if (bridgeBlocks <= 0) {
            return false; // avoid division by 0
        }
        float percentWater = (float) waterBlocksClipping / (float) bridgeBlocks;
        return percentWater > MIN_BRIDGE_LIQUID_BLOCKS_PERCENT && percentWater < MAX_BRIDGE_LIQUID_BLOCKS_PERCENT;
    }

    private static boolean isNotTutorialOrNearValidCapitolPosition(Level level, Building building, BlockPos originPos) {
        if (!level.isClientSide())
            return true;
        if (!TutorialClientEvents.isEnabled())
            return true;
        if (!(building instanceof TownCentre))
            return true;
        return TutorialClientEvents.BUILD_CAPITOL_POS.distSqr(originPos) < 625; // 25 block range
    }

    public static boolean isInBrightChunk(Level level, BlockPos originPos, List<BuildingBlock> blocks, String ownerName) {
        BlockPos minPos = BuildingUtils.getMinCorner(blocks).offset(originPos);
        BlockPos maxPos = BuildingUtils.getMaxCorner(blocks).offset(originPos);
        BlockPos centrePos = new BlockPos((minPos.getX() + maxPos.getX()) / 2, (minPos.getY() + maxPos.getY()) / 2, (minPos.getZ() + maxPos.getZ()) / 2);
        return isInBrightChunk(level, centrePos, ownerName);
    }

    public static boolean isInBrightChunk(Level level, BlockPos centrePos, String ownerName) {
        if (level.isClientSide()) {
            return FogOfWarClientEvents.isInBrightChunk(centrePos);
        } else {
            return FogOfWarServerEvents.isBlockVisibleFor(ownerName, centrePos.getX(), centrePos.getZ());
        }
    }
}
