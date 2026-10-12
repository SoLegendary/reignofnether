package com.solegendary.reignofnether.unit;

import net.neoforged.neoforge.event.tick.LevelTickEvent;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
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
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.bus.api.SubscribeEvent;

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
    public static void onWorldTick(LevelTickEvent.Post evt) {
        if (evt.getLevel().isClientSide() || evt.getLevel().dimension() != Level.OVERWORLD) {
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

        if (evt.getLevel().getServer() != null && evt.getLevel().getServer().getGameRules().getRule(GameRuleRegistrar.NEUTRAL_AGGRO).get()) {
            Set<PathfinderMob> pfMobs = new HashSet<>();
            for (LivingEntity unit : UnitServerEvents.getAllUnits()) {
                if (unit.tickCount % 20 != 0)
                    continue;
                AABB aabb = new AABB(unit.blockPosition().offset(-10, -10, -10).getCenter(), unit.blockPosition().offset(10, 10, 10).getCenter());
                pfMobs.addAll(evt.getLevel().getNearbyEntities(PathfinderMob.class, TargetingConditions.forCombat(), unit, aabb));
            }
            for (PathfinderMob pfMob : pfMobs) {
                if (!(shouldMobBeAggressive(pfMob)))
                    continue;

                boolean hasAttackGoal = false;
                for (WrappedGoal wrappedGoal : pfMob.targetSelector.getAvailableGoals())
                    if (wrappedGoal.getGoal() instanceof NearestAttackableTargetGoal)
                        hasAttackGoal = true;

                if (pfMob.getTarget() == null && hasAttackGoal && !attackSuppressedNonUnits.contains(pfMob)) {
                    LivingEntity target = MiscUtil.findClosestAttackableEntity(pfMob, 10, (ServerLevel) evt.getLevel());
                    if (target != null)
                        pfMob.setTarget(target);
                }
            }
        }
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
        if (!(le instanceof Unit) && evt.getNewAboutToBeSetTarget() instanceof Unit unit)
            if (Factions.getFaction(unit).equals(getNonUnitFaction(le)))
                evt.setCanceled(true);
    }

    private static boolean shouldMobBeAggressive(Mob mob) {
        return !(mob instanceof Vex) &&
                !(mob instanceof PhantomSummon);
    }
}
