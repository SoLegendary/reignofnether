package com.solegendary.reignofnether.unit;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.targeting.EntityTargetIndex;
import com.solegendary.reignofnether.unit.units.monsters.PhantomSummon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        if (evt.getLevel().isClientSide() ||
                !(evt.getEntity() instanceof PathfinderMob mob) || mob instanceof Unit ||
                !shouldMobBeAggressive(mob)) {
            return;
        }

        boolean hasAttackGoal = false;
        for (WrappedGoal wrappedGoal : mob.targetSelector.getAvailableGoals()) {
            if (wrappedGoal.getGoal() instanceof NeutralAggroGoal)
                return;
            if (wrappedGoal.getGoal() instanceof NearestAttackableTargetGoal)
                hasAttackGoal = true;
        }
        if (hasAttackGoal)
            mob.targetSelector.addGoal(3, new NeutralAggroGoal(mob));
    }

    private static final class NeutralAggroGoal extends Goal {
        private static final int SEARCH_INTERVAL = 20;
        private static final int SEARCH_RANGE = 10;
        private final PathfinderMob mob;
        private final TargetingConditions candidateConditions = TargetingConditions.forCombat()
                .range(SEARCH_RANGE).ignoreLineOfSight();
        private long nextSearchTick;
        private LivingEntity target;

        private NeutralAggroGoal(PathfinderMob mob) {
            this.mob = mob;
            nextSearchTick = mob.level().getGameTime() + Math.floorMod(mob.getId(), SEARCH_INTERVAL);
            // One-shot target acquisition: leave TARGET control and ongoing combat to the existing AI.
        }

        private boolean enabled() {
            return mob.level() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD &&
                    level.getGameRules().getRule(GameRuleRegistrar.NEUTRAL_AGGRO).get() &&
                    mob.getTarget() == null && !attackSuppressedNonUnits.contains(mob);
        }

        @Override
        public boolean canUse() {
            long tick = mob.level().getGameTime();
            if (tick < nextSearchTick)
                return false;
            nextSearchTick = tick + SEARCH_INTERVAL;
            target = null;
            if (!enabled())
                return false;

            target = findTarget((ServerLevel) mob.level());
            return target != null;
        }

        private LivingEntity findTarget(ServerLevel level) {
            Faction faction = getNonUnitFaction(mob);
            return EntityTargetIndex.get(level).findClosest(mob.position(), SEARCH_RANGE,
                    group -> group.unit() && !group.ownerName().isBlank() && !faction.equals(group.faction()),
                    null, entity -> 0,
                    entity -> entity.isAlive() &&
                            level.getWorldBorder().isWithinBounds(entity.blockPosition()) &&
                            candidateConditions.test(mob, entity) &&
                            !((Unit) entity).isGarrisoned() && mob.getSensing().hasLineOfSight(entity));
        }

        @Override
        public void start() {
            if (target != null && target.isAlive() && enabled())
                mob.setTarget(target);
            target = null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
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

    private static boolean shouldMobBeAggressive(Mob mob) {
        return !(mob instanceof Vex) &&
                !(mob instanceof PhantomSummon);
    }
}
