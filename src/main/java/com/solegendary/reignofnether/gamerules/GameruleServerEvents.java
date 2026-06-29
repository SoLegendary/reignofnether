package com.solegendary.reignofnether.gamerules;

import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Map;

public class GameruleServerEvents {

    @SubscribeEvent
    public static void onCommandUsed(CommandEvent evt) {
        List<ParsedCommandNode<CommandSourceStack>> nodes = evt.getParseResults().getContext().getNodes();
        if (nodes.size() <= 2)
            return;
        if (!nodes.get(0).getNode().getName().equals("gamerule"))
            return;

        if (nodes.get(1).getNode().getName().equals("maxPopulation")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                UnitServerEvents.maxPopulation = (int) args.get("value").getResult();
                GameruleClientboundPacket.setMaxPopulation(UnitServerEvents.maxPopulation);
            }
        } else if (nodes.get(1).getNode().getName().equals("groundYLevel")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                double groundYLevel = ((Integer) args.get("value").getResult()).doubleValue();
                GameruleClientboundPacket.setGroundYLevel((long) groundYLevel);
            }
        }  else if (nodes.get(1).getNode().getName().equals("flyingMaxYLevel")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                double flyingMaxYLevel = ((Integer) args.get("value").getResult()).doubleValue();
                GameruleClientboundPacket.setFlyingMaxYLevel((long) flyingMaxYLevel);
            }
        } else if (nodes.get(1).getNode().getName().equals("neutralAggro")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                boolean value = (boolean) args.get("value").getResult();
                GameruleClientboundPacket.setNeutralAggro(value);
            }
        } else if (nodes.get(1).getNode().getName().equals("allowBeacons")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                boolean value = (boolean) args.get("value").getResult();
                GameruleClientboundPacket.setAllowBeacons(value);
            }
        } else if (nodes.get(1).getNode().getName().equals("pvpModesOnly")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                boolean value = (boolean) args.get("value").getResult();
                GameruleClientboundPacket.setPvpModesOnly(value);
            }
        } else if (nodes.get(1).getNode().getName().equals("beaconWinMinutes")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                double beaconWinMinutes = ((Integer) args.get("value").getResult()).doubleValue();
                GameruleClientboundPacket.setBeaconWinMinutes((long) beaconWinMinutes);
            }
        } else if (nodes.get(1).getNode().getName().equals("slantedBuilding")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                boolean value = (boolean) args.get("value").getResult();
                GameruleClientboundPacket.setSlantedBuilding(value);
            }
        } else if (nodes.get(1).getNode().getName().equals("allowedHeroes")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                double value = ((Integer) args.get("value").getResult()).doubleValue();
                GameruleClientboundPacket.setAllowedHeroes((long) value);
            }
        } else if (nodes.get(1).getNode().getName().equals("lockAlliances")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                boolean value = (boolean) args.get("value").getResult();
                GameruleClientboundPacket.setLockAlliances(value);
            }
        } else if (nodes.get(1).getNode().getName().equals("scenarioMode")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                boolean value = (boolean) args.get("value").getResult();
                GameruleClientboundPacket.setScenarioMode(value);
            }
        } else if (nodes.get(1).getNode().getName().equals("coopMode")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                boolean value = (boolean) args.get("value").getResult();
                GameruleClientboundPacket.setCoopMode(value);
            }
        } else if (nodes.get(1).getNode().getName().equals("buildingsOutsideBorder")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                boolean value = (boolean) args.get("value").getResult();
                GameruleClientboundPacket.setBuildingsOutsideBorder(value);
            }
        } else if (nodes.get(1).getNode().getName().equals("improvedPathfinding")) {
            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                boolean value = (boolean) args.get("value").getResult();
                UnitServerEvents.improvedPathfinding = value;
                GameruleClientboundPacket.setImprovedPathfinding(value);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        MinecraftServer server = evt.getEntity().level().getServer();
        if (server != null) {
            boolean logFalling = server.getGameRules().getRule(GameRuleRegistrar.LOG_FALLING).get();
            GameruleClientboundPacket.setLogFalling(logFalling);
            boolean neutralAggro = server.getGameRules().getRule(GameRuleRegistrar.NEUTRAL_AGGRO).get();
            GameruleClientboundPacket.setNeutralAggro(neutralAggro);
            int maxPopulation = server.getGameRules().getInt(GameRuleRegistrar.MAX_POPULATION);
            GameruleClientboundPacket.setMaxPopulation(maxPopulation);
            boolean unitGriefing = server.getGameRules().getRule(GameRuleRegistrar.DO_UNIT_GRIEFING).get();
            GameruleClientboundPacket.setUnitGriefing(unitGriefing);
            boolean playerGriefing = server.getGameRules().getRule(GameRuleRegistrar.DO_PLAYER_GRIEFING).get();
            GameruleClientboundPacket.setPlayerGriefing(playerGriefing);
            int groundYLevel = server.getGameRules().getRule(GameRuleRegistrar.GROUND_Y_LEVEL).get();
            GameruleClientboundPacket.setGroundYLevel(groundYLevel);
            int flyingMaxYLevel = server.getGameRules().getRule(GameRuleRegistrar.FLYING_MAX_Y_LEVEL).get();
            GameruleClientboundPacket.setFlyingMaxYLevel(flyingMaxYLevel);
            boolean allowBeacons = server.getGameRules().getRule(GameRuleRegistrar.ALLOW_BEACONS).get();
            GameruleClientboundPacket.setAllowBeacons(allowBeacons);
            boolean pvpModesOnly = server.getGameRules().getRule(GameRuleRegistrar.PVP_MODES_ONLY).get();
            GameruleClientboundPacket.setPvpModesOnly(pvpModesOnly);
            int beaconWinMinutes = server.getGameRules().getRule(GameRuleRegistrar.BEACON_WIN_MINUTES).get();
            GameruleClientboundPacket.setBeaconWinMinutes(beaconWinMinutes);
            boolean slantedBuilding = server.getGameRules().getRule(GameRuleRegistrar.SLANTED_BUILDING).get();
            GameruleClientboundPacket.setSlantedBuilding(slantedBuilding);
            int allowHeroes = server.getGameRules().getRule(GameRuleRegistrar.ALLOWED_HEROES).get();
            GameruleClientboundPacket.setAllowedHeroes(allowHeroes);
            boolean lockAlliances = server.getGameRules().getRule(GameRuleRegistrar.LOCK_ALLIANCES).get();
            GameruleClientboundPacket.setLockAlliances(lockAlliances);
            boolean scenarioMode = server.getGameRules().getRule(GameRuleRegistrar.SCENARIO_MODE).get();
            GameruleClientboundPacket.setScenarioMode(scenarioMode);
            boolean coopMode = server.getGameRules().getRule(GameRuleRegistrar.COOP_MODE).get();
            GameruleClientboundPacket.setCoopMode(coopMode);
            boolean buildingsOutsideBorder = server.getGameRules().getRule(GameRuleRegistrar.BUILDINGS_OUTSIDE_BORDER).get();
            GameruleClientboundPacket.setBuildingsOutsideBorder(buildingsOutsideBorder);
            boolean improvedPathfinding = server.getGameRules().getRule(GameRuleRegistrar.IMPROVED_PATHFINDING).get();
            UnitServerEvents.improvedPathfinding = improvedPathfinding;
            GameruleClientboundPacket.setImprovedPathfinding(improvedPathfinding);
            // Let the joining player know this is an RTS-optimised map (small world border) where improved
            // pathfinding is on. Sent only to them, not broadcast.
            if (improvedPathfinding)
                evt.getEntity().sendSystemMessage(Component.literal("[RoN] RTS-optimised map, improved pathfinding enabled"));
        }
    }
}