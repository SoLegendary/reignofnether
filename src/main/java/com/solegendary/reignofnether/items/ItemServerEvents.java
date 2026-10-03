package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.addon.ItemShopAddon;
import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class ItemServerEvents {

    public static final boolean ENABLED = UnitItem.ENABLED;

    // for making every RTS player have the same drops
    public static final Long RANDOM_UNIT_ITEM_DROPS_SEED = new Random().nextLong();

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

    // Points needed for each successive drop increase range from 15 to 45
    private static int getCreepScoreThreshold(int itemsDropped) {
        int total = 0;
        for (int i = 0; i <= itemsDropped; i++)
            total += Math.min(15 + (i * 3), 45);
        return total;
    }

    private static void dropNextItem(RTSPlayer rtsPlayer, LivingEntity dropper) {
        if (rtsPlayer.itemDropQueue.isEmpty())
            return;

        UnitItem unitItem = rtsPlayer.itemDropQueue.pollFirst();
        if (unitItem == null)
            return;

        dropper.level().addFreshEntity(new ItemEntity(
                dropper.level(), dropper.getX(), dropper.getY(), dropper.getZ(),
                new ItemStack(unitItem.item))); // adjust to however UnitItem exposes its Item

        SoundClientboundPacket.playSoundAtPos(getItemDropSound(unitItem.rarity), dropper.blockPosition(), 2.5f);

        ReignOfNether.LOGGER.info(rtsPlayer.name + " received item drop #" + rtsPlayer.itemsDropped);
    }


    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent evt) {
        LivingEntity killedEntity = evt.getEntity();
        if (killedEntity instanceof Unit unitKilled) {
            String killerName = "";
            Entity directEntity = evt.getSource().getDirectEntity();
            Entity sourceEntity = evt.getSource().getEntity();
            LivingEntity lastHurtBy = evt.getEntity().getLastHurtByMob();

            if (directEntity instanceof Unit unit)
                killerName = unit.getOwnerName();
            else if (directEntity != null && directEntity.getPersistentData().contains("ownerName"))
                killerName = directEntity.getPersistentData().getString("ownerName");
            else if (sourceEntity instanceof Unit unit)
                killerName = unit.getOwnerName();
            else if (sourceEntity != null && sourceEntity.getPersistentData().contains("ownerName"))
                killerName = sourceEntity.getPersistentData().getString("ownerName");
            else if (lastHurtBy instanceof Unit unit)
                killerName = unit.getOwnerName();
            else if (lastHurtBy != null && lastHurtBy.getPersistentData().contains("ownerName"))
                killerName = lastHurtBy.getPersistentData().getString("ownerName");

            boolean isNeutral = unitKilled.getOwnerName().isBlank();
            if (!killerName.isBlank() && isNeutral) {
                RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(killerName);
                if (rtsPlayer != null) {
                    int creepScore = unitKilled.getCost().population + 4;
                    if (killedEntity.getPersistentData().contains("isFromSpawner") &&
                        killedEntity.getPersistentData().getBoolean("isFromSpawner")) {
                        creepScore = unitKilled.getCost().population + 1;
                    }
                    rtsPlayer.creepScore += creepScore;
                    ReignOfNether.LOGGER.info("+" + creepScore + " creepScore for: " + rtsPlayer.name);

                    if (!killedEntity.level().isClientSide()) {
                        // while, not if: one big kill could cross more than one threshold
                        while (rtsPlayer.creepScore >= getCreepScoreThreshold(rtsPlayer.itemsDropped)) {
                            dropNextItem(rtsPlayer, killedEntity);
                            rtsPlayer.itemsDropped += 1;
                        }
                    }
                }
            }
        }
    }

    public static SoundAction getItemDropSound(Rarity rarity) {
        if (rarity == Rarity.RARE || rarity == Rarity.EPIC)
            return SoundAction.ITEM_DROP_RARE;
        if (rarity == UnitItemRarity.LEGENDARY || rarity == UnitItemRarity.MYTHIC)
            return SoundAction.ITEM_DROP_LEGENDARY;
        return SoundAction.ITEM_DROP_COMMON;
    }
}
