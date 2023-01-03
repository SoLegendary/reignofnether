package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.BuildRepairGoal;
import com.solegendary.reignofnether.unit.goals.GatherResourcesGoal;
import com.solegendary.reignofnether.unit.goals.ReturnResourcesGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public interface WorkerUnit {
    public BuildRepairGoal getBuildRepairGoal();
    public GatherResourcesGoal getGatherResourceGoal();
    public BlockState getReplantBlockState();

    public static void tick(WorkerUnit unit) {
        BuildRepairGoal buildRepairGoal = unit.getBuildRepairGoal();
        if (buildRepairGoal != null)
            buildRepairGoal.tick();
        GatherResourcesGoal gatherResourcesGoal = unit.getGatherResourceGoal();
        if (gatherResourcesGoal != null)
            gatherResourcesGoal.tick();
    }

    public static void resetBehaviours(WorkerUnit unit) {
        unit.getBuildRepairGoal().stopBuilding();
        unit.getGatherResourceGoal().stopGathering();
    }

    public static boolean isIdle(WorkerUnit unit) {
        GatherResourcesGoal resGoal = unit.getGatherResourceGoal();

        BlockPos bp = ((Unit) unit).getMoveGoal().getMoveTarget();
        BlockPos onBp = ((LivingEntity) unit).getOnPos();
        double dist = bp == null ? 0 : bp.distToCenterSqr(onBp.getX() + 0.5f, onBp.getY() + 0.5f, onBp.getZ() + 0.5f);
        boolean isStationary = dist < 1.5f && resGoal.getMoveTarget() == null;

        boolean isGathering = resGoal.getGatherTarget() != null;
        boolean isReturning = ((Unit) unit).getReturnResourcesGoal().getBuildingTarget() != null;
        boolean isBuilding = unit.getBuildRepairGoal().getBuildingTarget() != null;

        return isStationary && !isGathering && !isReturning && !isBuilding;
    }
}
