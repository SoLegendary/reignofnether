package com.solegendary.reignofnether.unit;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class NonUnitServerEvents {

    // list of units to cancel specific goals for so they don't interfere with player commands
    public static final List<PathfinderMob> attackSuppressedNonUnits = Collections.synchronizedList(new ArrayList<>());
    public static final List<PathfinderMob> moveSuppressedNonUnits = Collections.synchronizedList(new ArrayList<>());

    public static final List<Pair<PathfinderMob, BlockPos>> nonUnitMoveTargets = Collections.synchronizedList(new ArrayList<>());

    public static boolean canControlAllMobs(Level level, String playerName) {
        return ResearchServerEvents.playerHasCheat(playerName, "wouldyoukindly");
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD) {
            return;
        }

        synchronized (nonUnitMoveTargets) {
            nonUnitMoveTargets.removeIf(pair -> {
                PathfinderMob mob = pair.getFirst();
                BlockPos finalTargetBp = pair.getSecond();
                Path navPath = mob.getNavigation().getPath();
                if (mob.distanceToSqr(finalTargetBp.getCenter()) < 4) {
                    return true;
                } else if (mob.tickCount % 20 == 0 && navPath == null ||
                    (navPath != null && navPath.getEndNode() != null && mob.distanceToSqr(navPath.getEndNode().asBlockPos().getCenter()) < 4)) {
                    Path path = mob.getNavigation().createPath(finalTargetBp.getX(), finalTargetBp.getY(), finalTargetBp.getZ(), 0);
                    mob.getNavigation().moveTo(path, 1);
                }
                return false;
            });
        }
        synchronized (attackSuppressedNonUnits) {
            attackSuppressedNonUnits.removeIf(mob -> (mob.isDeadOrDying() || mob.isRemoved() || (mob.getNavigation().isDone() && mob.getTarget() == null)));
        }
        synchronized (moveSuppressedNonUnits) {
            moveSuppressedNonUnits.removeIf(mob -> (mob.isDeadOrDying() || mob.isRemoved() || (mob.getNavigation().isDone() && mob.getTarget() == null)));
        }

        if (evt.level instanceof ServerLevel serverLevel && serverLevel.getGameTime() % 20 == 0)
            moveAnimalsOutOfWater(serverLevel);
    }

    private static final int WATER_ESCAPE_RANGE = 8;
    private static final int WATER_ESCAPE_RANGE_Y = 3;

    // vanilla only gives water a path penalty, so huntable animals still wade in whenever it's the shortest route
    @SubscribeEvent
    public static void onAnimalJoin(EntityJoinLevelEvent evt) {
        if (!evt.getLevel().isClientSide() && evt.getEntity() instanceof Mob mob && ResourceSources.isHuntableAnimal(mob))
            mob.setPathfindingMalus(BlockPathTypes.WATER, -1.0f);
    }

    private static void moveAnimalsOutOfWater(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Animal animal) || !animal.isInWater() || animal.isPassenger() ||
                !ResourceSources.isHuntableAnimal(animal))
                continue;
            if (moveSuppressedNonUnits.contains(animal))
                continue;
            if (animal.getNavigation().isInProgress() && !animal.getNavigation().isStuck())
                continue;

            BlockPos origin = animal.blockPosition();
            BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
            BlockPos closest = null;
            double closestDistSqr = Double.MAX_VALUE;
            for (int x = -WATER_ESCAPE_RANGE; x <= WATER_ESCAPE_RANGE; x++) {
                for (int z = -WATER_ESCAPE_RANGE; z <= WATER_ESCAPE_RANGE; z++) {
                    for (int y = -WATER_ESCAPE_RANGE_Y; y <= WATER_ESCAPE_RANGE_Y; y++) {
                        bp.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                        double distSqr = bp.distSqr(origin);
                        if (distSqr >= closestDistSqr || !isDryStandable(level, bp))
                            continue;
                        closest = bp.immutable();
                        closestDistSqr = distSqr;
                    }
                }
            }
            if (closest != null) {
                Path path = animal.getNavigation().createPath(closest.getX() + 0.5, closest.getY() + 0.5, closest.getZ() + 0.5, 0);
                animal.getNavigation().moveTo(path, 1);
            }
        }
    }

    private static boolean isDryStandable(Level level, BlockPos bp) {
        BlockState feet = level.getBlockState(bp);
        BlockState head = level.getBlockState(bp.above());
        BlockPos below = bp.below();
        return feet.getFluidState().isEmpty() && feet.getCollisionShape(level, bp).isEmpty() &&
            head.getFluidState().isEmpty() && head.getCollisionShape(level, bp.above()).isEmpty() &&
            level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) &&
            level.getFluidState(below).isEmpty();
    }

    public static Faction getNonUnitFaction(LivingEntity le) {
        if (le instanceof IronGolem || le instanceof AbstractIllager || le instanceof AbstractVillager)
            return Factions.VILLAGERS;
        else if (le instanceof AbstractPiglin || le instanceof Hoglin || le instanceof Ghast || le instanceof Blaze || le instanceof WitherSkeleton)
            return Factions.PIGLINS;
        else if (le instanceof AbstractSkeleton || le instanceof Zombie || le instanceof Creeper || le instanceof Spider || le instanceof Slime || le instanceof Warden)
            return Factions.MONSTERS;
        return Factions.NONE;
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent evt) {
        LivingEntity le = evt.getEntity();

        // prevent vanilla mobs attacking their RoN faction equivalents
        if (!(le instanceof Unit) && evt.getNewTarget() instanceof Unit unit)
            if (Factions.getFaction(unit).equals(getNonUnitFaction(le)))
                evt.setCanceled(true);
    }
}
