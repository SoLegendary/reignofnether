package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class FlyingUsePortalGoal extends FlyingMoveToTargetGoal {

    private BuildingPlacement buildingTarget;

    public FlyingUsePortalGoal(Mob mob) {
        super(mob, 0);
    }

    public void tick() {
        if (buildingTarget instanceof PortalPlacement portal && moveTarget != null) {
            calcMoveTarget();
            if (buildingTarget.getBlocksPlaced() <= 0) {
                stopUsingPortal();
            }
            if (this.mob.distanceToSqr(new Vec3(moveTarget.getX() + 0.5f,
                moveTarget.getY() + 0.5f,
                moveTarget.getZ() + 0.5f
            )) <= 9f) {
                // teleport to destination
                if (portal.hasDestination()) {
                    BlockPos bp = portal.destination.offset(0,4,0);
                    SoundClientboundPacket.playSoundAtPos(SoundAction.USE_PORTAL, bp);
                    mob.teleportTo(bp.getX() + 0.5f, bp.getY() + 0.5f, bp.getZ() + 0.5f);
                    SoundClientboundPacket.playSoundAtPos(SoundAction.USE_PORTAL, portal.destination);
                }
                this.stopUsingPortal();
            }
        } else {
            this.moveTarget = null;
        }
    }

    private void calcMoveTarget() {
        if (this.buildingTarget instanceof PortalPlacement) {
            this.moveTarget = this.buildingTarget.centrePos.offset(0,4,0);
        }
    }

    public void setBuildingTarget(BlockPos blockPos) {
        if (blockPos != null) {
            if (this.mob.level().isClientSide()) {
                this.buildingTarget = BuildingUtils.findBuilding(true, blockPos);
                if (this.buildingTarget instanceof PortalPlacement portal
                    && buildingTarget.ownerName.equals(((Unit) mob).getOwnerName())) {

                    if (portal.hasDestination()) {
                        MiscUtil.addUnitCheckpoint(((Unit) mob), new BlockPos(
                            buildingTarget.centrePos.getX(),
                            buildingTarget.originPos.getY() + 1,
                            buildingTarget.centrePos.getZ()
                        ), true);
                    } else {
                        HudClientEvents.showTemporaryMessage(I18n.get("hud.reignofnether.no_destination"));
                    }
                }
            } else {
                this.buildingTarget = BuildingUtils.findBuilding(false, blockPos);
            }
            calcMoveTarget();
            this.start();
        }
    }

    public BuildingPlacement getBuildingTarget() {
        return buildingTarget;
    }

    // if we override stop() it for some reason is called after start() and we can never begin this goal...
    public void stopUsingPortal() {
        buildingTarget = null;
        super.stopMoving();
    }
}
