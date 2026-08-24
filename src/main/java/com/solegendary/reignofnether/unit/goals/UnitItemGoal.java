package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.shared.AbstractMarket;
import com.solegendary.reignofnether.items.ItemAction;
import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

// allows a unit to be able to mount any target as long as their ability allows them to
public class UnitItemGoal extends MoveToTargetBlockGoal {



    public static final float RANGE = 2;
    private ItemEntity itemTarget = null;
    private ItemStack itemInHand = null;
    private Unit unitTarget = null;
    private BlockPos blockTarget = null;
    private BuildingPlacement buildingTarget = null;
    private boolean useItem = false;

    public UnitItemGoal(Mob mob) {
        super(mob, false, 0);
    }

    private ItemAction getAction() {
        if (!(mob instanceof UnitInventory)) {
            return ItemAction.NONE;
        } else if (ItemUtil.isUnitItem(itemInHand) && blockTarget != null && mob.level().getWorldBorder().isWithinBounds(blockTarget)) {
            return useItem ? ItemAction.USE_ON_BLOCK : ItemAction.DROP;
        } else if (ItemUtil.isUnitItem(itemInHand) && buildingTarget != null) {
            return useItem ? ItemAction.USE_ON_BUILDING : ItemAction.SELL;
        } else if (ItemUtil.isUnitItem(itemInHand) && unitTarget instanceof UnitInventory) {
            return useItem ? ItemAction.USE_ON_ENTITY : ItemAction.GIVE;
        } else if (ItemUtil.isUnitItem(itemTarget)) {
            return ItemAction.PICKUP;
        }
        return ItemAction.NONE;
    }

    @Nullable
    private BlockPos getMoveTargetForAction(ItemAction action) {
        return switch (action) {
            case DROP, USE_ON_BLOCK -> blockTarget;
            case SELL, USE_ON_BUILDING -> buildingTarget.getClosestGroundPos(mob.getOnPos(), 0);
            case GIVE, USE_ON_ENTITY -> ((Entity) unitTarget).getOnPos();
            case PICKUP -> itemTarget.getOnPos();
            case USE -> mob.getOnPos();
            case NONE -> null;
        };
    }

    @Override
    public void tick() {
        ItemAction action = getAction();
        this.setMoveTarget(getMoveTargetForAction(action));

        if (getMoveTarget() != null && this.mob instanceof UnitInventory inv) {
            double distSqr;
            if (unitTarget != null)
                distSqr = this.mob.distanceToSqr((Entity) unitTarget);
            else
                distSqr = this.mob.distanceToSqr(getMoveTarget().getCenter());

            if (distSqr < RANGE * RANGE) {
                if (!this.mob.level().isClientSide()) {
                    switch (action) {
                        case DROP -> inv.dropUUID(ItemUtil.getUUID(itemInHand), blockTarget);
                        case SELL -> {
                            BuildingPlacement bpl = BuildingUtils.findBuilding(false, blockTarget);
                            if (bpl != null && bpl.getBuilding() instanceof AbstractMarket) {
                                inv.deleteUUID(ItemUtil.getUUID(itemInHand));
                                UnitItem unitItem = ItemUtil.getUnitItem(itemInHand.getItem());
                                if (mob instanceof Unit unit) {
                                    RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(unit.getOwnerName());
                                    if (rtsPlayer != null && unitItem != null) {
                                        ResourcesServerEvents.addSubtractResources(new Resources(unit.getOwnerName(), 0, 0, 0, unitItem.sellValue));
                                        // TODO: play money sound
                                    }
                                }
                            }
                        }
                        case GIVE -> {
                            if (unitTarget instanceof UnitInventory inv2)
                                inv.giveTo(ItemUtil.getUUID(itemInHand), inv2);
                        }
                        case PICKUP -> {
                            if (itemTarget.isAlive() && inv.tryAdding(itemTarget.getItem()))
                                itemTarget.discard(); // todo: actually pickup physically
                        }
                        case USE_ON_BLOCK -> inv.useOnGround(ItemUtil.getUUID(itemInHand), blockTarget);
                        case USE_ON_ENTITY -> inv.useOnEntity(ItemUtil.getUUID(itemInHand), (LivingEntity) unitTarget);
                        case USE_ON_BUILDING -> inv.useOnBuilding(ItemUtil.getUUID(itemInHand), buildingTarget);
                        case USE -> inv.use(ItemUtil.getUUID(itemInHand));
                        case NONE -> { }
                    }
                }
                this.stop();
            }
        }
    }

    @Override
    public void stop() {
        this.stopMoving();
    }
}
