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
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class UnitItemGoal extends MoveToTargetBlockGoal {

    public static final float RANGE = 2;
    private ItemStack itemInHand = null;
    private ItemEntity itemTarget = null;
    private LivingEntity leTarget = null;
    private BlockPos blockTarget = null;
    private BuildingPlacement buildingTarget = null;
    private boolean useItem = false;

    public UnitItemGoal(Mob mob) {
        super(mob, false, 0);
    }

    public void start(ItemStack itemInHand, ItemEntity itemTarget, LivingEntity leTarget,
                      BlockPos blockTarget, BuildingPlacement buildingTarget, boolean useItem) {
        this.itemInHand = itemInHand;
        this.itemTarget = itemTarget;
        this.leTarget = leTarget;
        this.blockTarget = blockTarget;
        this.buildingTarget = buildingTarget;
        this.useItem = useItem;
    }

    private ItemAction getAction() {
        if (!(mob instanceof UnitInventory)) {
            return ItemAction.NONE;
        } else if (ItemUtil.isUnitItem(itemInHand) && buildingTarget != null) {
            return useItem ? ItemAction.USE_ON_BUILDING : ItemAction.SELL;
        } else if (ItemUtil.isUnitItem(itemInHand) && blockTarget != null && mob.level().getWorldBorder().isWithinBounds(blockTarget)) {
            return useItem ? ItemAction.USE_ON_BLOCK : ItemAction.DROP;
        } else if (ItemUtil.isUnitItem(itemInHand) && leTarget != null && useItem) {
            return ItemAction.USE_ON_ENTITY;
        } else if (ItemUtil.isUnitItem(itemInHand) && leTarget instanceof UnitInventory) {
            return ItemAction.GIVE;
        } else if (ItemUtil.isUnitItem(itemTarget)) {
            return ItemAction.PICKUP;
        }
        return ItemAction.NONE;
    }

    @Nullable
    private BlockPos getMoveTargetForAction(ItemAction action) {
        return switch (action) {
            case DROP, USE_ON_BLOCK -> blockTarget;
            case SELL, USE_ON_BUILDING -> buildingTarget.getClosestGroundPos(mob.getOnPos(), 1);
            case GIVE, USE_ON_ENTITY -> leTarget.getOnPos();
            case PICKUP -> itemTarget.getOnPos();
            case USE, NONE, SWAP -> null;
        };
    }

    @Override
    public void tick() {
        ItemAction action = getAction();
        this.setMoveTarget(getMoveTargetForAction(action));

        if (getMoveTarget() != null && this.mob instanceof UnitInventory inv) {
            double distSqr = 0;
            if (leTarget != null)
                distSqr = this.mob.distanceToSqr(leTarget);
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
                                UnitItem unitItem = ItemUtil.getUnitItem(itemInHand);
                                if (mob instanceof Unit unit) {
                                    RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(unit.getOwnerName());
                                    if (rtsPlayer != null && unitItem != null) {
                                        Resources res = new Resources(unit.getOwnerName(), 0, 0, 0, unitItem.sellValue);
                                        ResourcesServerEvents.addSubtractResources(res);
                                        ResourcesClientboundPacket.showFloatingText(res, this.mob.getOnPos());
                                        SoundClientboundPacket.playSoundAtPos(SoundAction.SELL_ITEM, this.mob.getOnPos());
                                    }
                                }
                            }
                        }
                        case GIVE -> {
                            if (leTarget instanceof UnitInventory inv2) {
                                inv.giveTo(ItemUtil.getUUID(itemInHand), inv2);
                            }
                        }
                        case PICKUP -> {
                            if (itemTarget.isAlive() && inv.tryAdding(itemTarget.getItem())) {
                                this.mob.take(itemTarget, itemTarget.getItem().getCount());
                                itemTarget.discard();
                            }
                        }
                        case USE_ON_BLOCK -> inv.useOnGround(ItemUtil.getUUID(itemInHand), blockTarget);
                        case USE_ON_ENTITY -> inv.useOnEntity(ItemUtil.getUUID(itemInHand), leTarget);
                        case USE_ON_BUILDING -> inv.useOnBuilding(ItemUtil.getUUID(itemInHand), buildingTarget);
                        case NONE, SWAP, USE -> { }
                    }
                }
                this.stop();
            }
        }
    }

    public boolean isIdle() {
        return moveTarget == null;
    }

    @Override
    public void stop() {
        this.stopMoving();
        this.itemInHand = null;
        this.itemTarget = null;
        this.leTarget = null;
        this.blockTarget = null;
        this.buildingTarget = null;
        this.useItem = false;
    }
}
