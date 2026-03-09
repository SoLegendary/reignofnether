package com.solegendary.reignofnether.building;

// class for static building functions

import com.solegendary.reignofnether.building.addon.NetherConvertingAddon;
import com.solegendary.reignofnether.building.buildings.monsters.SculkCatalyst;
import com.solegendary.reignofnether.building.buildings.monsters.Stronghold;
import com.solegendary.reignofnether.building.buildings.piglins.Fortress;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.building.buildings.placements.SculkCatalystPlacement;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
import com.solegendary.reignofnether.building.buildings.villagers.Castle;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class BuildingUtils {

    public static List<Keybinding> keybindings = Arrays.asList();

    public static int getTotalCompletedBuildingsOwned(boolean isClientSide, String ownerName) {
        List<BuildingPlacement> buildings;
        if (isClientSide)
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();

        List<BuildingPlacement> list = new ArrayList<>();
        for (BuildingPlacement b : buildings) {
            if (b.isBuilt && b.ownerName.equals(ownerName)) {
                list.add(b);
            }
        }
        return list.size();
    }

    public static boolean castleOwned(boolean isClientSide, String ownerName) {
        List<BuildingPlacement> buildings;
        if (isClientSide)
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();

        for (BuildingPlacement building : buildings) {
            if (ownerName.equals(building.ownerName) &&
                building.isBuilt &&
                (building.getBuilding() instanceof Castle ||
                building.getBuilding() instanceof Fortress ||
                building.getBuilding() instanceof Stronghold)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBuildingBuildable(boolean isClientSide, BuildingPlacement building) {
        List<BuildingPlacement> buildings;
        if (isClientSide)
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();
        var flag = false;
        for (BuildingPlacement buildingPlacement : buildings) {
            if (buildingPlacement.originPos == building.originPos) {
                flag = true;
                break;
            }
        }
        return flag && building.getBlocksPlaced() < building.getBlocksTotal() && (!building.isBuilt || building.getBuilding().repairable);
    }

    // returns a list of BPs that may reside in unique chunks for fog of war calcs
    public static ArrayList<BlockPos> getUniqueChunkBps(BuildingPlacement building) {
        AABB aabb = new AABB(
                building.minCorner,
                building.maxCorner.offset(1,1,1)
        );

        ArrayList<BlockPos> bps = new ArrayList<>();
        double x = aabb.minX;
        double z = aabb.minZ;
        do {
            do {
                bps.add(new BlockPos((int) x, (int) aabb.minY, (int) z));
                x += 16;
            }
            while (x <= aabb.maxX);
            z += 16;
            x = aabb.minX;
        }
        while (z <= aabb.maxZ);

        // include far corners
        bps.add(new BlockPos((int) aabb.maxX, (int) aabb.minY, (int) aabb.minZ));
        bps.add(new BlockPos((int) aabb.minX, (int) aabb.minY, (int) aabb.maxZ));
        bps.add(new BlockPos((int) aabb.maxX, (int) aabb.minY, (int) aabb.maxZ));

        return bps;
    }

    public static BuildingPlacement getNewBuildingPlacement(Building building, Level level, BlockPos pos, Rotation rotation, String ownerName, boolean isDiagonalBridge) {
        BuildingPlacement buildingPlacement = null;
        if (building instanceof AbstractBridge bridge) {
            buildingPlacement = bridge.createBuildingPlacement(level, pos, rotation, ownerName, isDiagonalBridge);
        } else {
            buildingPlacement = building.createBuildingPlacement(level, pos, rotation, ownerName);
        }

        if (buildingPlacement != null) {
            buildingPlacement.updateButtons();
            buildingPlacement.setLevel(level);
        }

        return buildingPlacement;
    }

    // note originPos may be an air block
    public static BuildingPlacement findBuilding(boolean isClientSide, BlockPos pos) {
        List<BuildingPlacement> buildings = isClientSide ? BuildingClientEvents.getBuildings() : BuildingServerEvents.getBuildings();

        for (BuildingPlacement building : buildings)
            if (building.originPos.equals(pos) || building.isPosInsideBuilding(pos))
                return building;
        return null;
    }

    // functions for corners/centrePos given only blocks
    // if you have access to the Building itself, you should use .minCorner, .maxCorner and .centrePos
    public static BlockPos getMinCorner(ArrayList<BuildingBlock> blocks) {
        MinMaxValues minMax = calculateMinMax(blocks);
        return new BlockPos(minMax.minX, minMax.minY, minMax.minZ);
    }

    public static BlockPos getMaxCorner(ArrayList<BuildingBlock> blocks) {
        MinMaxValues minMax = calculateMinMax(blocks);
        return new BlockPos(minMax.maxX, minMax.maxY, minMax.maxZ);
    }

    public static BlockPos getCentrePos(ArrayList<BuildingBlock> blocks) {
        MinMaxValues minMax = calculateMinMax(blocks);
        return new BlockPos(
                (minMax.minX + minMax.maxX) / 2,
                (minMax.minY + minMax.maxY) / 2,
                (minMax.minZ + minMax.maxZ) / 2
        );
    }

    private static MinMaxValues calculateMinMax(ArrayList<BuildingBlock> blocks) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BuildingBlock block : blocks) {
            BlockPos pos = block.getBlockPos();
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;

            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }

        return new MinMaxValues(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static class MinMaxValues {
        int minX, minY, minZ;
        int maxX, maxY, maxZ;

        public MinMaxValues(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }



    public static Vec3i getBuildingSize(ArrayList<BuildingBlock> blocks) {
        BlockPos min = getMinCorner(blocks);
        BlockPos max = getMaxCorner(blocks);
        return new Vec3i(
                max.getX() - min.getX(),
                max.getY() - min.getY(),
                max.getZ() - min.getZ()
        );
    }

    public static ArrayList<BuildingBlock> getAbsoluteBlockData(ArrayList<BuildingBlock> staticBlocks, LevelAccessor level, BlockPos originPos, Rotation rotation, Vec3i blockOffset) {
        ArrayList<BuildingBlock> blocks = new ArrayList<>();

        for (BuildingBlock block : staticBlocks) {
            block = block.rotate(level, rotation);
            BlockPos bp = block.getBlockPos();

            block.setBlockPos(new BlockPos(
                    bp.getX() + originPos.getX() + blockOffset.getX(),
                    bp.getY() + originPos.getY() + blockOffset.getY() + 1,
                    bp.getZ() + originPos.getZ() + blockOffset.getZ()
            ));
            blocks.add(block);
        }
        return blocks;
    }

    // get BlockPos values with absolute world positions
    public static ArrayList<BuildingBlock> getAbsoluteBlockData(ArrayList<BuildingBlock> staticBlocks, LevelAccessor level, BlockPos originPos, Rotation rotation) {
        return getAbsoluteBlockData(staticBlocks, level, originPos, rotation, new Vec3i(0,0,0));
    }

    // returns whether the given pos is part of ANY building in the level
    // WARNING: very processing expensive!
    public static boolean isPosPartOfAnyBuilding(boolean isClientSide, BlockPos bp, boolean onlyPlacedBlocks, int range) {
        List<BuildingPlacement> buildings = isClientSide
                ? BuildingClientEvents.getBuildings()
                : BuildingServerEvents.getBuildings();

        // Precompute range squared to avoid repeated calculation
        int rangeSquared = range * range;

        for (BuildingPlacement building : buildings) {
            if ((range == 0 || bp.distSqr(building.centrePos) < rangeSquared) &&
                building.isPosPartOfBuilding(bp, onlyPlacedBlocks)) {
                return true;
            }
        }
        return false;
    }


    // returns whether the given pos is part of ANY building in the level
    public static boolean isPosInsideAnyBuilding(boolean isClientSide, BlockPos bp) {
        List<BuildingPlacement> buildings;
        if (isClientSide)
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();

        for (BuildingPlacement building : buildings)
            if (building.isPosInsideBuilding(bp))
                return true;
        return false;
    }

    public static boolean isPosInsideAnyNonBridgeBuilding(boolean isClientSide, BlockPos bp) {
        List<BuildingPlacement> buildings;
        if (isClientSide)
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();

        for (BuildingPlacement building : buildings)
            if (!(building.getBuilding() instanceof AbstractBridge) && building.isPosInsideBuilding(bp))
                return true;
        return false;
    }

    @Nullable
    public static BuildingPlacement findClosestBuilding(boolean isClientSide, Vec3 pos, Predicate<BuildingPlacement> condition) {
        List<BuildingPlacement> buildings;
        if (isClientSide)
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();

        double closestDist = 9999;
        BuildingPlacement closestBuilding = null;
        for (BuildingPlacement building : buildings) {
            if (condition.test(building)) {
                BlockPos bp = building.centrePos;
                Vec3 bpVec3 = new Vec3(bp.getX(), bp.getY(), bp.getZ());
                double dist = bpVec3.distanceToSqr(pos);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestBuilding = building;
                }
            }
        }
        return closestBuilding;
    }

    public static boolean isInNetherRange(boolean isClientSide, BlockPos bp) {
        List<BuildingPlacement> buildings = getBuildingsList(isClientSide);

        for (BuildingPlacement building : buildings) {
            NetherConvertingAddon ncb;
            if ((ncb = building.getBuilding().getActiveAddon(NetherConvertingAddon.class)) != null && ncb.getMaxNetherRange(building) > 0) {
                double maxRangeSquared = Math.pow(ncb.getMaxNetherRange(building), 2);
                if (bp.distSqr(building.centrePos) <= maxRangeSquared) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isWithinRangeOfMaxedCatalyst(LivingEntity entity) {
        List<BuildingPlacement> buildings = getBuildingsList(entity.level().isClientSide());

        double maxCatalystRangeSquared = SculkCatalyst.ESTIMATED_RANGE * SculkCatalyst.ESTIMATED_RANGE;

        for (BuildingPlacement building : buildings) {
            if (building instanceof SculkCatalystPlacement sc) {
                if (entity.distanceToSqr(Vec3.atCenterOf(sc.centrePos)) < maxCatalystRangeSquared &&
                        sc.getUncappedNightRange() >= SculkCatalyst.nightRangeMax * 1.5f) {
                    return true;
                }
            }
        }
        return false;
    }

    // Helper method to get the buildings list based on client or server side.
    private static List<BuildingPlacement> getBuildingsList(boolean isClientSide) {
        return isClientSide
                ? BuildingClientEvents.getBuildings()
                : BuildingServerEvents.getBuildings();
    }

    public static BeaconPlacement getBeacon(boolean isClientSide) {
        List<BuildingPlacement> buildings = getBuildingsList(isClientSide);
        for (BuildingPlacement building : buildings)
            if (building instanceof BeaconPlacement beacon)
                return beacon;
        return null;
    }

    public static void clearBuildingArea(BuildingPlacement building) {
        if (building != null) {
            for (int x = building.minCorner.getX() - 1; x < building.maxCorner.getX() + 2; x++)
                for (int y = building.minCorner.getY(); y < building.maxCorner.getY() + 2; y++)
                    for (int z = building.minCorner.getZ() - 1; z < building.maxCorner.getZ() + 2; z++)
                        if (!isPosInsideAnyBuilding(building.level.isClientSide(), new BlockPos(x,y,z)))
                            building.getLevel().setBlockAndUpdate(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState());
        }
    }
}
