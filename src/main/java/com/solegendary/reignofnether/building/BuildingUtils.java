package com.solegendary.reignofnether.building;

// class for static building functions

import com.solegendary.reignofnether.building.buildings.monsters.*;
import com.solegendary.reignofnether.building.buildings.shared.Stockpile;
import com.solegendary.reignofnether.building.buildings.villagers.*;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class BuildingUtils {

    public static boolean isInRangeOfNightSource(Vec3 pos, boolean clientSide) {
        List<Building> buildings = clientSide ? BuildingClientEvents.getBuildings() : BuildingServerEvents.getBuildings();
        for (Building building : buildings) {
            if (building instanceof Mausoleum mausoleum) {
                if (BuildingUtils.getCentrePos(mausoleum.getBlocks()).distToCenterSqr(pos.x, pos.y, pos.z) < Math.pow(Mausoleum.nightRange, 2))
                    return true;
            }
        }
        return false;
    }

    public static boolean doesPlayerOwnTownCentre(String playerName) {
        for (Building building : BuildingClientEvents.getBuildings())
            if (building.isTownCentre && building.ownerName.equals(playerName))
                return true;
        return false;
    }

    // returns a list of BPs that may reside in unique chunks for fog of war calcs
    public static ArrayList<BlockPos> getUniqueChunkBps(Building building) {
        AABB aabb = new AABB(
                building.minCorner,
                building.maxCorner.offset(1,1,1)
        );

        ArrayList<BlockPos> bps = new ArrayList<>();
        double x = aabb.minX;
        double z = aabb.minZ;
        do {
            do {
                bps.add(new BlockPos(x, aabb.minY, z));
                x += 16;
            }
            while (x <= aabb.maxX);
            z += 16;
            x = aabb.minX;
        }
        while (z <= aabb.maxZ);

        // include far corners
        bps.add(new BlockPos(aabb.maxX, aabb.minY, aabb.minZ));
        bps.add(new BlockPos(aabb.minX, aabb.minY, aabb.maxZ));
        bps.add(new BlockPos(aabb.maxX, aabb.minY, aabb.maxZ));

        return bps;
    }

    // given a string name return a new instance of that building
    public static Building getNewBuilding(String buildingName, Level level, BlockPos pos, Rotation rotation, String ownerName) {
        Building building = null;
        switch(buildingName) {
            case Stockpile.buildingName -> building = new Stockpile(level, pos, rotation, ownerName);
            case VillagerHouse.buildingName -> building = new VillagerHouse(level, pos, rotation, ownerName);
            case Graveyard.buildingName -> building = new Graveyard(level, pos, rotation, ownerName);
            case WheatFarm.buildingName -> building = new WheatFarm(level, pos, rotation, ownerName);
            case Laboratory.buildingName -> building = new Laboratory(level, pos, rotation, ownerName);
            case Barracks.buildingName -> building = new Barracks(level, pos, rotation, ownerName);
            case PumpkinFarm.buildingName -> building = new PumpkinFarm(level, pos, rotation, ownerName);
            case HauntedHouse.buildingName -> building = new HauntedHouse(level, pos, rotation, ownerName);
            case Blacksmith.buildingName -> building = new Blacksmith(level, pos, rotation, ownerName);
            case TownCentre.buildingName -> building = new TownCentre(level, pos, rotation, ownerName);
            case Mausoleum.buildingName -> building = new Mausoleum(level, pos, rotation, ownerName);
        }
        if (building != null)
            building.setLevel(level);
        return building;
    }

    // note originPos may not actually be a part of the building itself
    public static Building findBuilding(List<Building> buildings, BlockPos pos) {
        for (Building building : buildings)
            if (building.originPos.equals(pos) || building.isPosInsideBuilding(pos))
                return building;
        return null;
    }

    // functions for corners/centrePos given only blocks
    // if you have access to the Building itself, you should use .minCorner, .maxCorner and .centrePos
    public static BlockPos getMinCorner(ArrayList<BuildingBlock> blocks) {
        return new BlockPos(
                blocks.stream().min(Comparator.comparing(block -> block.getBlockPos().getX())).get().getBlockPos().getX(),
                blocks.stream().min(Comparator.comparing(block -> block.getBlockPos().getY())).get().getBlockPos().getY(),
                blocks.stream().min(Comparator.comparing(block -> block.getBlockPos().getZ())).get().getBlockPos().getZ()
        );
    }
    public static BlockPos getMaxCorner(ArrayList<BuildingBlock> blocks) {
        return new BlockPos(
                blocks.stream().max(Comparator.comparing(block -> block.getBlockPos().getX())).get().getBlockPos().getX(),
                blocks.stream().max(Comparator.comparing(block -> block.getBlockPos().getY())).get().getBlockPos().getY(),
                blocks.stream().max(Comparator.comparing(block -> block.getBlockPos().getZ())).get().getBlockPos().getZ()
        );
    }
    public static BlockPos getCentrePos(ArrayList<BuildingBlock> blocks) {
        BlockPos min = getMinCorner(blocks);
        BlockPos max = getMaxCorner(blocks);
        return new BlockPos(
                (float) (min.getX() + max.getX()) / 2,
                (float) (min.getY() + max.getY()) / 2,
                (float) (min.getZ() + max.getZ()) / 2
        );
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

    // get BlockPos values with absolute world positions
    public static ArrayList<BuildingBlock> getAbsoluteBlockData(ArrayList<BuildingBlock> staticBlocks, LevelAccessor level, BlockPos originPos, Rotation rotation) {
        ArrayList<BuildingBlock> blocks = new ArrayList<>();

        for (BuildingBlock block : staticBlocks) {
            block = block.rotate(level, rotation);
            BlockPos bp = block.getBlockPos();

            block.setBlockPos(new BlockPos(
                    bp.getX() + originPos.getX(),
                    bp.getY() + originPos.getY() + 1,
                    bp.getZ() + originPos.getZ()
            ));
            blocks.add(block);
        }
        return blocks;
    }

    // returns whether the given pos is part of ANY building in the level
    public static boolean isPosPartOfAnyBuilding(Level level, BlockPos bp, boolean onlyPlacedBlocks) {
        List<Building> buildings;
        if (level.isClientSide())
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();

        for (Building building : buildings)
            if (building.isPosPartOfBuilding(bp, onlyPlacedBlocks))
                return true;
        return false;
    }

    // returns whether the given pos is part of ANY building in the level
    public static boolean isPosInsideAnyBuilding(Level level, BlockPos bp) {
        List<Building> buildings;
        if (level.isClientSide())
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();

        for (Building building : buildings)
            if (building.isPosInsideBuilding(bp))
                return true;
        return false;
    }

    public static Building findClosestBuilding(ArrayList<Building> buildings, Vec3 pos, Predicate<Building> condition) {
        double closestDist = 9999;
        Building closestBuilding = null;
        for (Building building : buildings) {
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
}
