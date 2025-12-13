package com.solegendary.reignofnether.survival.spawners;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.IPlantable;

import java.util.*;

import static com.solegendary.reignofnether.survival.SurvivalServerEvents.ENEMY_OWNER_NAME;

public class WaveSpawner {

    public static final int MAX_SPAWN_RANGE = 100;
    public static final int MIN_SPAWN_RANGE = 70;
    public static final int SAMPLE_POINTS_PER_BUILDING = 100;
    public static final int MAX_FAILED_BUILDINGS = 10;

    public static final int MIN_VALID_BUILDINGS = 5; // once we sort the buildings by distance to centroid, how many buildings do we pick to spawn around?
    public static final float PERCENT_VALID_BUILDINGS = 0.1f; // % of all buildings added to MIN_VALID_BUILDINGS

    private static final Random random = new Random();

    public static int getModifiedPopCost(Unit unit) {
        return Math.max(1, unit.getCost().population - 1);
    }

    // used to determine how flat an area is
    public static double getYVariance(Level level, BlockPos bp, int radius) {
        ArrayList<BlockPos> bps = new ArrayList<>();

        for (int x = -radius; x < radius; x++)
            for (int z = -radius; z < radius; z++)
                bps.add(MiscUtil.getHighestGroundBlock(level, bp.offset(x, 0, z)));

        int n = bps.size();
        if (n == 0)
            return 0;

        // Calculate mean Y value
        double sumY = 0.0;
        for (BlockPos bp1 : bps)
            sumY += bp1.getY();

        double meanY = sumY / n;

        // Calculate variance
        double variance = 0.0;
        for (BlockPos bp1 : bps)
            variance += Math.pow(bp1.getY() - meanY, 2);

        return variance / n;
    }

    public static void placeIceOrMagma(BlockPos bp, Level level) {

        BlockPos bpLiquid = null;
        for (int i = 0; i < 10; i++) {
            if (!level.getBlockState(bp.offset(0,5-i,0)).getFluidState().isEmpty()) {
                bpLiquid = bp.offset(0,5-i,0);
                break;
            }
        }
        if (bpLiquid == null)
            return;

        BlockState bs = level.getBlockState(bpLiquid);
        BlockState bsToPlace;

        if (bs.getFluidState().is(FluidTags.LAVA))
            bsToPlace = BlockRegistrar.WALKABLE_MAGMA_BLOCK.get().defaultBlockState();
        else if (bs.getFluidState().is(FluidTags.WATER))
            bsToPlace = Blocks.FROSTED_ICE.defaultBlockState();
        else
            return;

        level.setBlockAndUpdate(bpLiquid, bsToPlace);

        List<BlockPos> bps = List.of(bpLiquid.north(), bpLiquid.east(), bpLiquid.south(), bpLiquid.west(),
                bpLiquid.north().east(),
                bpLiquid.south().west(),
                bpLiquid.north().east(),
                bpLiquid.south().west());

        // Frostwalker effect provided in LivingEntityMixin, but it only happens on changing block positions on the ground
        for (BlockPos pos : bps) {
            BlockState bsAdj = level.getBlockState(pos);
            if (!bsAdj.getFluidState().isEmpty() ||
                    (bsAdj instanceof IPlantable plantable && plantable instanceof LiquidBlockContainer))
                level.setBlockAndUpdate(pos, bsToPlace);
        }
    }

    public static List<BlockPos> getValidSpawnPoints(int amount, Level level, boolean allowLiquid, int flatnessRadius) {
        List<BuildingPlacement> buildings = new ArrayList<>();
        for (BuildingPlacement buildingPlacement : BuildingServerEvents.getBuildings()) {
            if (!ENEMY_OWNER_NAME.equals(buildingPlacement.ownerName) && !buildingPlacement.ownerName.isBlank()) {
                buildings.add(buildingPlacement);
            }
        }

        Random random = new Random();
        if (buildings.isEmpty())
            return List.of();

        Vec3 centroid = new Vec3(0,0,0);

        for (BuildingPlacement building : buildings) {
            centroid = centroid.add(Vec3.atCenterOf(building.centrePos));
        }
        double invBs = 1f / buildings.size();
        final Vec3 fCentroid = centroid.multiply(new Vec3(invBs, invBs, invBs));

        // calculate all valid buildings to spawn around based on distance from the centroid
        buildings.sort(
                Comparator.comparing((BuildingPlacement b) -> b.centrePos.distToCenterSqr(fCentroid.x, fCentroid.y, fCentroid.z)).reversed()
        );

        int numValidBuildings = (int) (MIN_VALID_BUILDINGS + (buildings.size() * PERCENT_VALID_BUILDINGS));
        List<BuildingPlacement> validBuildings = buildings.subList(0, Math.min(buildings.size(), numValidBuildings));

        int spawnAttemptsThisBuilding = 0;
        BlockState spawnBs;
        BlockPos spawnBp;
        double distSqrToNearestBuilding = 999999;
        double distSqrToNearestEnemyBuilding = 999999;
        int failedBuildings = 0;
        ArrayList<BlockPos> validSpawns = new ArrayList<>();

        outerloop:
        do {
            do {
                BuildingPlacement building = validBuildings.get(random.nextInt(validBuildings.size()));

                int x = building.centrePos.getX() + random.nextInt(-MAX_SPAWN_RANGE, MAX_SPAWN_RANGE);
                int z = building.centrePos.getZ() + random.nextInt(-MAX_SPAWN_RANGE, MAX_SPAWN_RANGE);
                int y = level.getChunkAt(new BlockPos(x, 0, z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

                spawnBp = MiscUtil.getHighestGroundBlock(level, new BlockPos(x, y, z));
                spawnBs = level.getBlockState(spawnBp);
                spawnAttemptsThisBuilding += 1;
                if (spawnAttemptsThisBuilding > 100) {
                    //ReignOfNether.LOGGER.warn("Gave up trying to find a suitable spawn!");
                    failedBuildings += 1;
                    if (failedBuildings > MAX_FAILED_BUILDINGS)
                        break outerloop;
                    else
                        continue;
                }
                Vec3 vec3 = new Vec3(x, y, z);
                BuildingPlacement b = BuildingUtils.findClosestBuilding(false, vec3, (b1) -> !b1.ownerName.equals(ENEMY_OWNER_NAME));
                BuildingPlacement eb = BuildingUtils.findClosestBuilding(false, vec3, (b1) -> b1.ownerName.equals(ENEMY_OWNER_NAME));

                if (b != null)
                    distSqrToNearestBuilding = b.centrePos.distToCenterSqr(vec3);
                if (eb != null)
                    distSqrToNearestEnemyBuilding = eb.centrePos.distToCenterSqr(vec3);

            } while (spawnBs.is(BlockTags.LEAVES)
                    || spawnBs.is(BlockTags.LOGS) || spawnBs.is(BlockTags.PLANKS)
                    || distSqrToNearestBuilding < (MIN_SPAWN_RANGE * MIN_SPAWN_RANGE)
                    || distSqrToNearestEnemyBuilding < (10 * 10)
                    || (!spawnBs.getFluidState().isEmpty() && !allowLiquid)
                    || BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), spawnBp)
                    || BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), spawnBp.above())
                    || (flatnessRadius > 0 && getYVariance(level, spawnBp, flatnessRadius) >= flatnessRadius / 2f));

            validSpawns.add(spawnBp);
            amount -= 1;

        } while(amount > 0);

        Collections.shuffle(validSpawns);

        if (validSpawns.isEmpty())
            PlayerServerEvents.sendMessageToAllPlayers("WARNING: could not find any valid spawn locations!");

        return validSpawns;
    }

    public static BuildingPlacement spawnBuilding(Building building, BlockPos bp) {
        BuildingPlacement placement = BuildingServerEvents.placeBuilding(
                building, bp,
                Rotation.NONE,
                ENEMY_OWNER_NAME,
                new int[] {},
                false,
                false
        );
        if (placement != null)
            placement.selfBuilding = true;
        BuildingUtils.clearBuildingArea(placement);
        return placement;
    }
}
