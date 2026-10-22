package com.solegendary.reignofnether.soldier;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.ping.PingType;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.taskscheduler.TaskSchedulerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// first-person soldiers: one fixed kit (giveKit), death into spectator, timed respawn at their commander's
// buildings. Who is a soldier lives in PlayerRoles
public class SoldierServerEvents {

    static class State {
        boolean dead = false;
        long respawnAtTick = 0;
        String killer = "";
        String respawnBuilding = "";
        PingType order = null; // the commander's last ATTACK/DEFEND ping
        BlockPos orderPos = BlockPos.ZERO;
        long orderTick = 0;
    }

    private static final Map<String, State> states = new ConcurrentHashMap<>();
    private static long ticks = 0;

    public static void bind(ServerPlayer player, String owner, BlockPos startPos) {
        String name = player.getName().getString();
        PlayerServerEvents.closeTopdownGui(player.getId()); // restores its saved game mode first, so ours sticks
        PlayerRoles.setSoldier(name, owner);
        states.put(name, new State());
        player.setGameMode(GameType.SURVIVAL);
        BlockPos spawn = MiscUtil.getHighestNonAirBlock(player.level(), startPos.offset(3, 0, 3)).above();
        player.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        giveKit(player);
        sync(player);
    }

    // the soldier's faction lost: they watch from here on
    public static void toSpectator(ServerPlayer player) {
        if (states.remove(player.getName().getString()) != null)
            player.getInventory().clearContent();
        player.setGameMode(GameType.SPECTATOR);
        SoldierStateClientboundPacket.clear(player);
        TaskSchedulerServerEvents.schedule(60, () -> FogOfWarServerEvents.resendAllTrackedChunks(player));
    }

    // the same kit for every soldier, about a mid-tier unit
    public static void giveKit(ServerPlayer player) {
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(Items.IRON_SWORD));
        player.getInventory().add(new ItemStack(Items.BOW));
        player.getInventory().add(new ItemStack(Items.ARROW, 16));
        player.getInventory().add(new ItemStack(Items.BREAD, 8));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
    }

    // a commander's ATTACK or DEFEND ping becomes their soldiers' current order
    public static void setOrder(String owner, PingType type, BlockPos pos) {
        if (type != PingType.ATTACK && type != PingType.DEFEND)
            return;
        for (String name : PlayerRoles.soldiersOf(owner)) {
            State state = states.get(name);
            ServerPlayer player = find(name);
            if (state != null && player != null) {
                state.order = type;
                state.orderPos = pos;
                state.orderTick = ticks;
                sync(player);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent evt) {
        if (!(evt.getEntity() instanceof ServerPlayer player))
            return;
        State state = states.get(player.getName().getString());
        if (state == null || state.dead)
            return;
        // no death screen and no dropped kit: the soldier just turns into a spectator until they respawn
        evt.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.clearFire();
        player.getInventory().clearContent();
        player.setGameMode(GameType.SPECTATOR);
        Entity killer = evt.getSource().getEntity();
        state.dead = true;
        state.killer = killer != null ? killer.getDisplayName().getString() : "";
        state.respawnAtTick = ticks + 20L * player.level().getGameRules().getInt(GameRuleRegistrar.SOLDIER_RESPAWN_SECONDS);
        BuildingPlacement building = respawnBuilding(PlayerRoles.commanderOf(player.getName().getString()), player.blockPosition());
        state.respawnBuilding = building != null ? building.getBuilding().name : "";
        sync(player);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        ticks++;
        if (ticks % 20 != 0)
            return;
        states.forEach((name, state) -> {
            ServerPlayer player = find(name);
            if (player == null || !state.dead)
                return;
            if (ticks >= state.respawnAtTick)
                tryRespawn(player, state);
            else
                sync(player); // keeps the countdown honest
        });
    }

    private static void tryRespawn(ServerPlayer player, State state) {
        BuildingPlacement building = respawnBuilding(PlayerRoles.commanderOf(player.getName().getString()), player.blockPosition());
        if (building == null) {
            state.respawnBuilding = "";
            sync(player); // no building to come back to yet; keep waiting
            return;
        }
        BlockPos spawn = building.getClosestGroundPos(player.blockPosition(), 1);
        player.teleportTo(spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5);
        player.setGameMode(GameType.SURVIVAL);
        giveKit(player);
        state.dead = false;
        state.killer = "";
        sync(player);
    }

    // the commander's capitol, else their built building nearest to `near`
    private static BuildingPlacement respawnBuilding(String owner, BlockPos near) {
        BuildingPlacement nearest = null;
        for (BuildingPlacement b : BuildingServerEvents.getBuildings()) {
            if (!b.ownerName.equals(owner) || !b.isBuilt)
                continue;
            if (b.isCapitol)
                return b;
            if (nearest == null || b.centrePos.distSqr(near) < nearest.centrePos.distSqr(near))
                nearest = b;
        }
        return nearest;
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent evt) {
        if (states.containsKey(evt.getEntity().getName().getString()))
            evt.setCanceled(true); // the kit is all a soldier carries
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent evt) {
        if (states.containsKey(evt.getPlayer().getName().getString())) {
            evt.getPlayer().getInventory().add(evt.getEntity().getItem());
            evt.setCanceled(true);
        }
    }

    // no friendly fire from soldiers
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent evt) {
        if (!(evt.getSource().getEntity() instanceof Player attacker) || !states.containsKey(attacker.getName().getString()))
            return;
        String side = PlayerRoles.commanderOf(attacker.getName().getString());
        String targetSide = null;
        if (evt.getEntity() instanceof Player target)
            targetSide = PlayerRoles.commanderOf(target.getName().getString());
        else if (evt.getEntity() instanceof Unit unit)
            targetSide = unit.getOwnerName();
        if (targetSide != null && !targetSide.isBlank() &&
                (targetSide.equals(side) || AlliancesServerEvents.isAllied(side, targetSide)))
            evt.setCanceled(true);
    }

    // reconnecting soldiers come back as they left
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (!(evt.getEntity() instanceof ServerPlayer player))
            return;
        State state = states.get(player.getName().getString());
        if (state == null || PlayerRoles.roleOf(player.getName().getString()) != PlayerRole.SOLDIER)
            return;
        player.setGameMode(state.dead ? GameType.SPECTATOR : GameType.SURVIVAL);
        sync(player);
    }

    // the match is over: take the kit back and clear their soldier HUD
    public static void reset() {
        for (String name : states.keySet()) {
            ServerPlayer player = find(name);
            if (player != null) {
                player.getInventory().clearContent();
                SoldierStateClientboundPacket.clear(player);
            }
        }
        states.clear();
    }

    private static ServerPlayer find(String name) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getPlayerList().getPlayerByName(name) : null;
    }

    private static void sync(ServerPlayer player) {
        State state = states.get(player.getName().getString());
        if (state == null)
            return;
        int secondsLeft = state.dead ? (int) Math.max(0, (state.respawnAtTick - ticks + 19) / 20) : 0;
        SoldierStateClientboundPacket.send(player, new SoldierStateClientboundPacket(true, state.dead,
                secondsLeft, state.killer, state.respawnBuilding, state.order, state.orderPos,
                (int) ((ticks - state.orderTick) / 20)));
    }
}
