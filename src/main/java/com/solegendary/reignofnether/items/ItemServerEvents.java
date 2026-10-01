package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.addon.ItemShopAddon;
import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.UUID;

public class ItemServerEvents {

    public static final boolean ENABLED = UnitItem.ENABLED;

    public static void buyItem(
            Unit unit,
            String descId,
            BlockPos shopPos
    ) {
        if (!ENABLED) return;

        for (BuildingPlacement bpl : BuildingServerEvents.getBuildings()) {
            if (bpl.originPos.equals(shopPos) && bpl instanceof ItemShopPlacement itemShopPlacement && bpl.isBuilt) {
                ItemShopAddon itemShopAddon = bpl.getBuilding().getActiveAddon(ItemShopAddon.class);
                if (itemShopAddon != null && unit instanceof UnitInventory) {
                    itemShopAddon.buyItem(unit, itemShopPlacement, ItemUtil.getUnitItem(descId));
                    break;
                }
            }
        }
    }

    public static void swapItems(
            Unit unit, // unit performing the action
            int invIndex1,
            int invIndex2
    ) {
        if (!ENABLED) return;

        if (unit instanceof UnitInventory inv)
            inv.swapSlots(invIndex1, invIndex2);
    }
    
    public static void doAction(
            ItemAction action,
            Unit unit, // unit performing the action
            UUID itemUuid, // uuid of the item in the unit's inventory (unused for PICKUP/NONE)
            int targetId, // GIVE/USE_ON_ENTITY: target unit, PICKUP: target ItemEntity (-1 if unused)
            BlockPos blockTarget // DROP/USE_ON_BLOCK: block, SELL/USE_ON_BUILDING: building pos (null if unused)) {
    ) {
        if (!ENABLED) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = null;
        if (server != null) level = server.getLevel(Level.OVERWORLD);

        if (unit instanceof UnitInventory inv &&
            unit.getItemGoal() != null && level != null) {

            Entity entity = level.getEntity(targetId);
            ItemStack itemInHand = inv.get(itemUuid);
            UnitItem unitItem = ItemUtil.getUnitItem(itemInHand);
            if (action == ItemAction.USE) {
                if (inv.use(ItemUtil.getUUID(itemInHand)) && unitItem != null && unitItem.resetBehaviours)
                    Unit.fullResetBehaviours(unit);
            } else {
                ItemEntity itemTarget = (entity instanceof ItemEntity ie) ? ie : null;
                LivingEntity leTarget = (entity instanceof LivingEntity le2) ? le2 : null;
                BuildingPlacement buildingTarget = blockTarget != null ? BuildingUtils.findBuilding(false, blockTarget) : null;
                boolean useItem = List.of(ItemAction.USE_ON_BUILDING, ItemAction.USE_ON_BLOCK, ItemAction.USE_ON_ENTITY).contains(action);
                Unit.fullResetBehaviours(unit);
                unit.getItemGoal().start(itemInHand, itemTarget, leTarget, blockTarget, buildingTarget, useItem);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent evt) {
        if (evt.getEntity() instanceof Unit unitKilled) {
            String killerName = "";
            Entity sourceEntity = evt.getSource().getEntity();
            LivingEntity lastHurtBy = evt.getEntity().getLastHurtByMob();
            if (evt.getSource().getEntity() instanceof Unit unit)
                killerName = unit.getOwnerName();
            else if (lastHurtBy instanceof Unit unit)
                killerName = unit.getOwnerName();
            else if (sourceEntity != null && sourceEntity.getPersistentData().contains("ownerName"))
                killerName = sourceEntity.getPersistentData().getString("ownerName");
            else if (lastHurtBy != null && lastHurtBy.getPersistentData().contains("ownerName"))
                killerName = lastHurtBy.getPersistentData().getString("ownerName");

            boolean isNeutral = unitKilled.getOwnerName().isBlank();
            boolean isWaveSurvivalEnemy = SurvivalServerEvents.isEnabled() && unitKilled.getOwnerName().equals(SurvivalServerEvents.ENEMY_OWNER_NAME);
            if (!killerName.isBlank() && (isNeutral || isWaveSurvivalEnemy)) {
                RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(killerName);
                if (rtsPlayer != null) {
                    int creepScore = unitKilled.getCost().population + 2;
                    rtsPlayer.creepScore += creepScore;
                    System.out.println("+" + creepScore + " creepScore for: " + rtsPlayer.name);
                }
            }
        }
    }
}
