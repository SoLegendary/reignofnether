package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class GarrisonGoal extends MoveToTargetBlockGoal {

    private BuildingPlacement buildingTarget;

    public GarrisonGoal(Mob mob) {
        super(mob, true, 0);
    }

    public void tick() {
        if (buildingTarget instanceof GarrisonableBuilding garr && garr.getCapacity() > 0) {
            calcMoveTarget();
            if (buildingTarget.getBlocksPlaced() <= 0) {
                stopGarrisoning();
            }
            if (moveTarget != null && this.mob.distanceToSqr(new Vec3(moveTarget.getX() + 0.5f,
                moveTarget.getY() + 0.5f,
                moveTarget.getZ() + 0.5f
            )) <= 3f) {

                // teleport to garrison entry pos
                if (!garr.isFull() && buildingTarget.isBuilt && garr.getEntryPosition() != null) {
                    BlockPos bp = garr.getEntryPosition();
                    this.mob.teleportTo(bp.getX() + 0.5f, bp.getY() + 0.5f, bp.getZ() + 0.5f);
                }
                this.stopGarrisoning();
            }
        } else {
            this.moveTarget = null;
        }
    }

    private void calcMoveTarget() {
        if (this.buildingTarget instanceof GarrisonableBuilding garr && garr.getCapacity() > 0) {
            this.moveTarget = this.buildingTarget.getClosestGroundPos(mob.getOnPos(), 1);
        }
    }

    public void setBuildingTarget(BlockPos blockPos) {
        if (blockPos != null) {
            boolean clientside = this.mob.level().isClientSide();
            BuildingPlacement building = BuildingUtils.findBuilding(clientside, blockPos);

            if (building == null)
                return;

            boolean isAllied;
            if (clientside)
                isAllied = AlliancesClient.isAllied(building.ownerName, ((Unit) mob).getOwnerName());
            else
                isAllied = AlliancesServerEvents.isAllied(building.ownerName, ((Unit) mob).getOwnerName());

            if (!(building instanceof GarrisonableBuilding garr && garr.getCapacity() > 0) ||
                (!building.ownerName.equals(((Unit) mob).getOwnerName()) && !isAllied)) {
                if (clientside)
                    HudClientEvents.showTemporaryMessage(I18n.get("hud.reignofnether.not_garrisonable"));
            }
            else if (garr.isFull()) {
                if (clientside)
                    HudClientEvents.showTemporaryMessage(I18n.get("hud.reignofnether.building_full"));
            } else {
                if (clientside) {
                    MiscUtil.addUnitCheckpoint(((Unit) mob), new BlockPos(
                            building.centrePos.getX(),
                            building.originPos.getY() + 1,
                            building.centrePos.getZ()
                    ), true);
                }
                this.buildingTarget = building;
                calcMoveTarget();
                this.start();
            }
        }
    }

    public BuildingPlacement getBuildingTarget() {
        return buildingTarget;
    }

    // if we override stop() it for some reason is called after start() and we can never begin this goal...
    public void stopGarrisoning() {
        buildingTarget = null;
        super.stopMoving();
    }
}
