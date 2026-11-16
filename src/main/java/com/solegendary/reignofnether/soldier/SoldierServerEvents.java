package com.solegendary.reignofnether.soldier;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.ping.PingType;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.research.researchItems.ResearchSoldierGear;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.taskscheduler.TaskSchedulerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        boolean deployed = false; // false until the commander first pays to deploy them
        int foodNeeded = 0; // the deploy cost while the commander can't afford it
    }

    private static final int DEPLOY_FOOD = 100; // paid on every deploy, respawns included
    private static final int FOOD_PER_HUNGER = 2; // commander food per hunger point refilled near their buildings
    private static final int SUPPLY_RANGE = 16;
    private static final int ALONE_RANGE = 10;
    private static final float ALONE_DAMAGE_MULT = 0.6f; // damage dealt with no friendly unit nearby

    private static final Map<String, State> states = WorldState.server(new ConcurrentHashMap<>());
    private static final Set<String> kitToClear = WorldState.server(ConcurrentHashMap.newKeySet()); // soldiers offline when the match ended
    private static long ticks = 0;
    static {
        WorldState.onServerReset(() -> ticks = 0);
    }

    public static void bind(ServerPlayer player, String owner, BlockPos startPos) {
        String name = player.getName().getString();
        PlayerServerEvents.closeTopdownGui(player.getId()); // restores its saved game mode first, so ours sticks
        PlayerRoles.setSoldier(name, owner);
        State state = new State();
        state.dead = true; // they spectate until their commander deploys them, like a respawn
        state.respawnAtTick = ticks;
        states.put(name, state);
        player.getInventory().clearContent();
        player.setGameMode(GameType.SPECTATOR);
        BlockPos spawn = MiscUtil.getHighestNonAirBlock(player.level(), startPos.offset(3, 0, 3)).above();
        player.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
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

    public static void giveKit(ServerPlayer player) {
        equip(player);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
    }

    // about a mid-tier unit, the same stats for every faction. The capitol's soldier research adds
    // sharpness/power and protection per tier. Faction armor trim, and a helmet dyed in the team colour.
    // No food: their commander supplies it near their buildings
    private static void equip(ServerPlayer player) {
        String owner = PlayerRoles.commanderOf(player.getName().getString());
        int weaponTier = gearTier(owner, false);
        int armorTier = gearTier(owner, true);
        player.getInventory().clearContent();
        player.getInventory().add(enchanted(new ItemStack(Items.IRON_SWORD), Enchantments.SHARPNESS, weaponTier));
        player.getInventory().add(enchanted(new ItemStack(Items.BOW), Enchantments.POWER_ARROWS, weaponTier));
        player.getInventory().add(new ItemStack(Items.ARROW, 16));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

        RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(owner);
        ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
        if (rtsPlayer != null && rtsPlayer.startPosColorId > 0)
            ((DyeableLeatherItem) Items.LEATHER_HELMET).setColor(helmet, MapColor.byId(rtsPlayer.startPosColorId).col);
        player.setItemSlot(EquipmentSlot.HEAD, enchanted(helmet, Enchantments.ALL_DAMAGE_PROTECTION, armorTier));

        ArmorTrim trim = factionTrim(player.level().registryAccess(), rtsPlayer != null ? rtsPlayer.faction : null);
        Map<EquipmentSlot, Item> pieces = Map.of(EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE,
                EquipmentSlot.LEGS, Items.CHAINMAIL_LEGGINGS, EquipmentSlot.FEET, Items.CHAINMAIL_BOOTS);
        pieces.forEach((slot, item) -> {
            ItemStack stack = enchanted(new ItemStack(item), Enchantments.ALL_DAMAGE_PROTECTION, armorTier);
            if (trim != null)
                ArmorTrim.setTrim(player.level().registryAccess(), stack, trim);
            player.setItemSlot(slot, stack);
        });
    }

    private static ItemStack enchanted(ItemStack stack, Enchantment enchantment, int level) {
        if (level > 0)
            stack.enchant(enchantment, level);
        return stack;
    }

    @Nullable
    private static ArmorTrim factionTrim(RegistryAccess access, @Nullable Faction faction) {
        ResourceKey<TrimMaterial> material;
        ResourceKey<TrimPattern> pattern;
        if (faction == Factions.VILLAGERS) {
            material = TrimMaterials.EMERALD;
            pattern = TrimPatterns.COAST;
        } else if (faction == Factions.MONSTERS) {
            material = TrimMaterials.AMETHYST;
            pattern = TrimPatterns.WARD;
        } else if (faction == Factions.PIGLINS) {
            material = TrimMaterials.GOLD;
            pattern = TrimPatterns.SNOUT;
        } else {
            return null;
        }
        return new ArmorTrim(access.registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(material),
                access.registryOrThrow(Registries.TRIM_PATTERN).getHolderOrThrow(pattern));
    }

    // the highest soldier armor or weapons research tier this commander has
    private static int gearTier(String owner, boolean armor) {
        List<ResearchSoldierGear> tiers = armor
                ? List.of(ProductionItems.RESEARCH_SOLDIER_ARMOR_3, ProductionItems.RESEARCH_SOLDIER_ARMOR_2, ProductionItems.RESEARCH_SOLDIER_ARMOR_1)
                : List.of(ProductionItems.RESEARCH_SOLDIER_WEAPONS_3, ProductionItems.RESEARCH_SOLDIER_WEAPONS_2, ProductionItems.RESEARCH_SOLDIER_WEAPONS_1);
        for (ResearchSoldierGear research : tiers)
            if (ResearchServerEvents.playerHasResearch(owner, research))
                return research.tier;
        return 0;
    }

    // new soldier research is done: the commander's living soldiers get the better gear straight away
    public static void reequip(String owner) {
        for (String name : PlayerRoles.soldiersOf(owner)) {
            State state = states.get(name);
            ServerPlayer player = find(name);
            if (state != null && !state.dead && player != null)
                equip(player);
        }
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

    // statics outlive the world: clear them so the next world opened in this game starts clean
    // (not reset(): that needs the live players)

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
            if (player != null && state.dead && ticks >= state.respawnAtTick)
                tryRespawn(player, state); // the client counts down on its own until then
            else if (player != null && !state.dead)
                tickHunger(player);
        });
    }

    // hunger drains fast above 3 bars: no sprinting or healing for long unless their commander supplies food,
    // which happens near the commander's buildings for FOOD_PER_HUNGER food per point
    private static void tickHunger(ServerPlayer player) {
        FoodData food = player.getFoodData();
        if (food.getFoodLevel() > 6)
            player.causeFoodExhaustion(1.0f); // ~1 min from full to 3 bars
        String owner = PlayerRoles.commanderOf(player.getName().getString());
        if (food.getFoodLevel() < 20 && ResourcesServerEvents.canAfford(owner, ResourceName.FOOD, FOOD_PER_HUNGER) &&
                BuildingServerEvents.getBuildings().stream().anyMatch(b -> b.ownerName.equals(owner) && b.isBuilt &&
                        b.centrePos.closerThan(player.blockPosition(), SUPPLY_RANGE))) {
            food.eat(1, 0);
            ResourcesServerEvents.addSubtractResources(new Resources(owner, -FOOD_PER_HUNGER, 0, 0));
        }
    }

    private static void tryRespawn(ServerPlayer player, State state) {
        BuildingPlacement building = respawnBuilding(PlayerRoles.commanderOf(player.getName().getString()), player.blockPosition());
        if (building == null) {
            if (!state.respawnBuilding.isEmpty()) {
                state.respawnBuilding = "";
                sync(player); // no building to come back to yet; keep waiting
            }
            return;
        }
        String owner = PlayerRoles.commanderOf(player.getName().getString());
        if (!ResourcesServerEvents.canAfford(owner, ResourceName.FOOD, DEPLOY_FOOD)) {
            if (state.foodNeeded != DEPLOY_FOOD) {
                state.foodNeeded = DEPLOY_FOOD;
                sync(player); // keep waiting until the commander can pay
            }
            return;
        }
        ResourcesServerEvents.addSubtractResources(new Resources(owner, -DEPLOY_FOOD, 0, 0));
        state.foodNeeded = 0;
        state.deployed = true;
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

    // soldiers never gather or break buildings block by block (they hit buildings instead, see onLeftClickBlock),
    // and only break other blocks with doPlayerGriefing. Highest priority: other handlers act on building blocks
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent evt) {
        if (evt.getLevel().isClientSide() || !states.containsKey(evt.getPlayer().getName().getString()))
            return;
        boolean griefing = evt.getPlayer().level().getGameRules().getRule(GameRuleRegistrar.DO_PLAYER_GRIEFING).get();
        if (!griefing || ResourceSources.getFromBlockState(evt.getState()) != null ||
                BuildingUtils.isPosInsideAnyBuilding(false, evt.getPos()))
            evt.setCanceled(true);
    }

    // hitting an enemy building damages it like a melee unit, once per full attack swing
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock evt) {
        if (!(evt.getEntity() instanceof ServerPlayer player) || !states.containsKey(player.getName().getString()))
            return;
        BuildingPlacement building = BuildingUtils.findBuilding(false, evt.getPos());
        if (building == null)
            return;
        evt.setCanceled(true);
        String side = PlayerRoles.commanderOf(player.getName().getString());
        if (building.ownerName.isBlank() || AlliancesServerEvents.isAlliedOrOwned(side, building.ownerName))
            return;
        if (player.getAttackStrengthScale(0.5f) < 0.9f)
            return;
        building.damage(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * building.getMeleeDamageMult());
        player.resetAttackStrengthTicker();
    }

    // soldiers are weak alone: less damage with no friendly unit nearby
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent evt) {
        if (!(evt.getSource().getEntity() instanceof ServerPlayer attacker) || !states.containsKey(attacker.getName().getString()))
            return;
        String side = PlayerRoles.commanderOf(attacker.getName().getString());
        boolean alone = attacker.level().getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(ALONE_RANGE),
                e -> e instanceof Unit unit && AlliancesServerEvents.isAlliedOrOwned(side, unit.getOwnerName())).isEmpty();
        if (alone)
            evt.setAmount(evt.getAmount() * ALONE_DAMAGE_MULT);
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
        String name = player.getName().getString();
        if (kitToClear.remove(name)) {
            player.getInventory().clearContent();
            SoldierStateClientboundPacket.clear(player);
        }
        if (PlayerRoles.roleOf(name) != PlayerRole.SOLDIER)
            return;
        State state = states.computeIfAbsent(name, n -> { // none after a server restart mid-match
            State restored = new State();
            restored.deployed = true;
            return restored;
        });
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
            } else {
                kitToClear.add(name);
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
                (int) ((ticks - state.orderTick) / 20), state.deployed, state.foodNeeded));
    }
}
