package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.addon.ItemShopAddon;
import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.checkerframework.checker.units.qual.C;

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
            inv.canPickupUnitItems() &&
            unit.getItemGoal() != null && level != null) {
            Entity entity = level.getEntity(targetId);
            ItemStack itemInHand = inv.get(itemUuid);
            if (action == ItemAction.USE) {
                if (inv.canUseUnitItems() && inv.use(ItemUtil.getUUID(itemInHand)))
                    Unit.fullResetBehaviours(unit);
            } else {
                ItemEntity itemTarget = (entity instanceof ItemEntity ie) ? ie : null;
                LivingEntity leTarget = (entity instanceof LivingEntity le2) ? le2 : null;
                BuildingPlacement buildingTarget = blockTarget != null ? BuildingUtils.findBuilding(false, blockTarget) : null;
                boolean useItem = List.of(ItemAction.USE_ON_BUILDING, ItemAction.USE_ON_BLOCK, ItemAction.USE_ON_ENTITY).contains(action);
                if (inv.canUseUnitItems() || !useItem) {
                    Unit.fullResetBehaviours(unit);
                    unit.getItemGoal().start(itemInHand, itemTarget, leTarget, blockTarget, buildingTarget, useItem);
                }
            }
        } else if (unit instanceof UnitInventory inv && action == ItemAction.DROP && SandboxServer.isAnyoneASandboxPlayer()) {
            inv.deleteItem(itemUuid);
        }
    }

    // Points needed for each successive drop increase range from 15 to 45
    private static int getCreepScoreThreshold(int itemsDropped) {
        int total = 0;
        for (int i = 0; i <= itemsDropped; i++)
            total += Math.min(12 + (i * 4), 48);
        return total;
    }

    public static void dropNextItem(RTSPlayer rtsPlayer, LivingEntity dropper) {
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
            boolean willDropOwnItem = unitKilled instanceof UnitInventory inv && !inv.isEmpty() && !(unitKilled instanceof HeroUnit);
            if (!killerName.isBlank() && isNeutral && !willDropOwnItem) {
                RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(killerName);
                if (rtsPlayer != null) {
                    int creepScore = unitKilled.getCost().population + 3;
                    if (killedEntity.getPersistentData().contains("isFromSpawner") &&
                        killedEntity.getPersistentData().getBoolean("isFromSpawner")) {
                        creepScore = Math.max(1, unitKilled.getCost().population);
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

    // give items to regular units to hold and drop on death
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract evt) {
        if (evt.getLevel().isClientSide()) return;
        if (evt.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = evt.getEntity();
        ItemStack stack = evt.getItemStack();
        Entity target = evt.getTarget();
        String mobName = target.getName().getString();
        String itemName = stack.getHoverName().getString();
        UnitItem unitItem = ItemUtil.getUnitItem(stack);

        if (target instanceof Mob && target instanceof UnitInventory inv &&
            (inv.canPickupUnitItems() || player.isCreative()) &&
            !stack.isEmpty() && unitItem != null
        ) {
            evt.setCanceled(true);

            if (inv.isFull(unitItem)) {
                player.sendSystemMessage(Component.translatable("item.reignofnether.error.full_inventory", mobName));
                evt.setCancellationResult(InteractionResult.FAIL);
            } else if (inv.tryAdding(new ItemStack(stack.getItem(), 1))) {
                player.sendSystemMessage(Component.translatable("item.reignofnether.hud.give_to_unit", itemName, mobName));
                if (!player.isCreative()) {
                    stack.setCount(stack.getCount() - 1);
                }
                evt.setCancellationResult(InteractionResult.SUCCESS);
            }
            else {
                player.sendSystemMessage(Component.translatable("item.reignofnether.error.failed_other", mobName));
                evt.setCancellationResult(InteractionResult.FAIL);
            }
        }
    }
}
