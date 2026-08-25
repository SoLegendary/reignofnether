package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.UUID;

public class ItemServerEvents {

    public static void swapItems(
            int unitId, // unit performing the action
            int invIndex1,
            int invIndex2
    ) {
        for (LivingEntity le : UnitServerEvents.getAllUnits())
            if (le.getId() == unitId && le instanceof UnitInventory inv)
                inv.swapSlots(invIndex1, invIndex2);
    }
    
    public static void doAction(
            ItemAction action,
            int unitId, // unit performing the action
            UUID itemUuid, // uuid of the item in the unit's inventory (unused for PICKUP/NONE)
            int targetId, // GIVE/USE_ON_ENTITY: target unit, PICKUP: target ItemEntity (-1 if unused)
            BlockPos blockTarget // DROP/USE_ON_BLOCK: block, SELL/USE_ON_BUILDING: building pos (null if unused)) {
    ) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = null;
        if (server != null) level = server.getLevel(Level.OVERWORLD);

        for (LivingEntity le : UnitServerEvents.getAllUnits()) {
            if (le.getId() == unitId && le instanceof Unit unit &&
                le instanceof UnitInventory inv &&
                unit.getItemGoal() != null && level != null) {

                Entity entity = level.getEntity(targetId);

                ItemStack itemInHand = inv.get(itemUuid);
                ItemEntity itemTarget = (entity instanceof ItemEntity ie) ? ie : null;
                LivingEntity leTarget = (entity instanceof LivingEntity le2) ? le2 : null;
                BuildingPlacement buildingTarget = blockTarget != null ? BuildingUtils.findBuilding(false, blockTarget) : null;
                boolean useItem = List.of(ItemAction.USE_ON_BUILDING, ItemAction.USE_ON_BLOCK, ItemAction.USE_ON_ENTITY, ItemAction.USE).contains(action);

                Unit.fullResetBehaviours(unit);
                unit.getItemGoal().start(itemInHand, itemTarget, leTarget, blockTarget, buildingTarget, useItem);
                break;
            }
        }
    }
}
