package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.neutral.NeutralTransportPortal;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.LanguageUtil;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

// used to move a worker over to the location of a place to build in fog before transferring control over to BuildRepairGoal
public class ExploreBuildLocationGoal extends MoveToTargetBlockGoal {

    private WorkerUnit workerUnit;
    public ExploreBuildLocationGoal(WorkerUnit workerUnit) {
        super((Mob) workerUnit, true, 0);
        this.workerUnit = workerUnit;
    }

    private final ArrayList<BuildingPlacement> fogQueuedBuildings = new ArrayList<>();
    public ArrayList<BuildingPlacement> getFogQueuedBuildings() { return fogQueuedBuildings; }
    private final Map<BlockPos, List<BuildingBlock>> fogQueuedBlocksToDraw = new HashMap<>();
    public Map<BlockPos, List<BuildingBlock>> getFogQueuedBlocksToDraw() { return fogQueuedBlocksToDraw; }

    @Override
    public boolean canUse() {
        return super.canUse() || !fogQueuedBuildings.isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() || !fogQueuedBuildings.isEmpty();
    }

    public void tick() {
        boolean hasBuildingTarget = workerUnit.getBuildRepairGoal().getBuildingTarget() != null;
        int tickCount = mob.tickCount % 2 == 0 ? mob.tickCount : mob.tickCount + 1;
        if (hasBuildingTarget && !workerUnit.getBuildRepairGoal().isBuilding() && tickCount % 20 == 0) {
            workerUnit.getBuildRepairGoal().start();
            super.stopMoving();
        }
        else if (!this.mob.level().isClientSide() && !fogQueuedBuildings.isEmpty() && !hasBuildingTarget) {
            BuildingPlacement bpl = fogQueuedBuildings.get(0);
            boolean isPlaced = BuildingServerEvents.getBuildings().contains(bpl);

            // not placed and is explored -> place
            if (!isPlaced && FogOfWarServerEvents.isBlockVisibleFor(((Unit) mob).getOwnerName(), bpl.centrePos.getX(), bpl.centrePos.getZ())) {
                BuildingServerEvents.placeBuilding(bpl, bpl.originPos, bpl.rotation, bpl.ownerName, new int[]{}, false, bpl.isDiagonalBridge, false);
                super.stopMoving();
            } else if (isPlaced) { // is placed - >start building
                super.stopMoving();
                fogQueuedBuildings.remove(bpl);
                workerUnit.getBuildRepairGoal().queuedBuildings.add(0, bpl);
                workerUnit.getBuildRepairGoal().startNextQueuedBuilding();
            } else { // not explored -> go explore
                setMoveTarget(bpl.centrePos);
            }
        }
    }

    // don't override checkpoints
    @Override
    public void setMoveTarget(@Nullable BlockPos bp) {
        boolean changed = !Objects.equals(bp, this.moveTarget);
        if (changed) {
            resetRecalcBackoff();
            recalcCooldown = 0;
        }
        this.moveTarget = bp;

        if (changed && !this.mob.level().isClientSide())
            this.start();
    }

    // if we override stop() it for some reason is called after start() and we can never begin this goal...
    public void reset() {
        fogQueuedBuildings.clear();
        fogQueuedBlocksToDraw.clear();
        super.stopMoving();
    }
}
