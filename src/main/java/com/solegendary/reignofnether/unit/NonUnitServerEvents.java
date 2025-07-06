package com.solegendary.reignofnether.unit;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.units.monsters.PhantomSummon;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
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

        if (evt.level.getServer() != null && evt.level.getServer().getGameRules().getRule(GameRuleRegistrar.NEUTRAL_AGGRO).get()) {
            Set<PathfinderMob> pfMobs = new HashSet<>();
            for (LivingEntity unit : UnitServerEvents.getAllUnits()) {
                if (unit.tickCount % 20 != 0)
                    continue;
                AABB aabb = new AABB(unit.blockPosition().offset(-10, -10, -10), unit.blockPosition().offset(10, 10, 10));
                pfMobs.addAll(evt.level.getNearbyEntities(PathfinderMob.class, TargetingConditions.forCombat(), unit, aabb));
            }
            for (PathfinderMob pfMob : pfMobs) {
                if (!(shouldMobBeAggressive(pfMob)))
                    continue;

                boolean hasAttackGoal = false;
                for (WrappedGoal wrappedGoal : pfMob.targetSelector.getAvailableGoals())
                    if (wrappedGoal.getGoal() instanceof NearestAttackableTargetGoal)
                        hasAttackGoal = true;

                if (pfMob.getTarget() == null && hasAttackGoal && !attackSuppressedNonUnits.contains(pfMob)) {
                    LivingEntity target = MiscUtil.findClosestAttackableEntity(pfMob, 10, (ServerLevel) evt.level);
                    if (target != null)
                        pfMob.setTarget(target);
                }
            }
        }
    }

    private static boolean shouldMobBeAggressive(Mob mob) {
        return !(mob instanceof Vex) &&
                !(mob instanceof PhantomSummon);
    }
}
