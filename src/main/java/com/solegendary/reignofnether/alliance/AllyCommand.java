package com.solegendary.reignofnether.alliance;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AllyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ally")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(AllyCommand::ally)));

        dispatcher.register(Commands.literal("allyconfirm")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(AllyCommand::allyConfirm)));

        dispatcher.register(Commands.literal("allycancelrequest")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(AllyCommand::allyCancelRequest)));

        dispatcher.register(Commands.literal("disband")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(AllyCommand::disband)));

        dispatcher.register(Commands.literal("allycontrol")
                .executes(AllyCommand::getAlliedControl));

        dispatcher.register(Commands.literal("allycontrol")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(AllyCommand::setAlliedControl)));
    }

    private static int ally(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer allyPlayer = EntityArgument.getPlayer(context, "player");
        String playerName = player.getName().getString();
        String allyPlayerName = allyPlayer.getName().getString();

        if (context.getSource().getLevel().getGameRules().getRule(GameRuleRegistrar.LOCK_ALLIANCES).get() &&
            (PlayerServerEvents.isRTSPlayer(playerName) || PlayerServerEvents.isRTSPlayer(allyPlayerName))) {
            player.sendSystemMessage(Component.translatable("alliance.reignofnether.alliances_lock"));
            return 0;
        }

        if (player.equals(allyPlayer)) {
            player.sendSystemMessage(Component.translatable("alliance.reignofnether.ally_self", playerName));
            return 0;
        }
        AlliancesServerEvents.pendingAlliances.computeIfAbsent(allyPlayerName, k -> new HashSet<>()).add(playerName);
        AllianceClientboundPacket.addPendingAlliance(allyPlayerName, playerName);
        context.getSource().sendSuccess(()->Component.translatable("alliance.reignofnether.sent_request", allyPlayerName), false);
        SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, allyPlayerName);
        allyPlayer.sendSystemMessage(Component.translatable("alliance.reignofnether.ally_confirm", playerName, playerName));
        SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, playerName);

        return Command.SINGLE_SUCCESS;
    }

    private static int allyCancelRequest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer allyPlayer = EntityArgument.getPlayer(context, "player");
        String playerName = player.getName().getString();
        String allyPlayerName = allyPlayer.getName().getString();

        if (player.equals(allyPlayer)) {
            return 0;
        }
        AlliancesServerEvents.pendingAlliances.getOrDefault(playerName, new HashSet<>()).remove(allyPlayerName);
        AllianceClientboundPacket.cancelPendingAlliance(allyPlayerName, playerName);
        context.getSource().sendSuccess(()->Component.translatable("alliance.reignofnether.ally_request_cancelled_self", allyPlayerName), false);
        SoundClientboundPacket.playSoundForPlayer(SoundAction.ENEMY, allyPlayerName);
        allyPlayer.sendSystemMessage(Component.translatable("alliance.reignofnether.ally_request_cancelled", playerName));
        SoundClientboundPacket.playSoundForPlayer(SoundAction.ENEMY, playerName);

        return Command.SINGLE_SUCCESS;
    }

    private static int allyConfirm(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer requesterPlayer = EntityArgument.getPlayer(context, "player");
        String playerName = player.getName().getString();
        String requesterPlayerName = requesterPlayer.getName().getString();

        if (AlliancesServerEvents.pendingAlliances.getOrDefault(playerName, new HashSet<>()).contains(requesterPlayerName)) {
            AlliancesServerEvents.addAlliance(playerName, requesterPlayerName);
            AlliancesServerEvents.pendingAlliances.getOrDefault(requesterPlayerName, new HashSet<>()).remove(playerName);

            context.getSource().sendSuccess(()->Component.translatable("alliance.reignofnether.now_allied", requesterPlayerName), false);
            SoundClientboundPacket.playSoundForPlayer(SoundAction.ALLY, requesterPlayerName);
            requesterPlayer.sendSystemMessage(Component.translatable("alliance.reignofnether.ally_accepted", playerName));
            SoundClientboundPacket.playSoundForPlayer(SoundAction.ALLY, playerName);

            for (ServerPlayer serverPlayer : PlayerServerEvents.players) {
                if (!serverPlayer.equals(player) && !serverPlayer.equals(requesterPlayer)) {
                    serverPlayer.sendSystemMessage(Component.translatable("alliance.reignofnether.now_allied_third_party", playerName, requesterPlayerName));
                    SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, playerName);
                }
            }
        } else {
            context.getSource().sendFailure(Component.translatable("alliance.reignofnether.no_request", requesterPlayerName));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int disband(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer allyPlayer = EntityArgument.getPlayer(context, "player");
        String playerName = player.getName().getString();
        String allyPlayerName = allyPlayer.getName().getString();

        if (context.getSource().getLevel().getGameRules().getRule(GameRuleRegistrar.LOCK_ALLIANCES).get() &&
                (PlayerServerEvents.isRTSPlayer(playerName) || PlayerServerEvents.isRTSPlayer(allyPlayerName))) {
            player.sendSystemMessage(Component.translatable("alliance.reignofnether.alliances_lock"));
            return 0;
        }

        if (player.equals(allyPlayer)) {
            context.getSource().sendFailure(Component.translatable("alliance.reignofnether.disband_self"));
            return 0;
        }
        AlliancesServerEvents.removeAlliance(playerName, allyPlayerName);

        player.sendSystemMessage(Component.translatable("alliance.reignofnether.disbanded", allyPlayerName));
        SoundClientboundPacket.playSoundForPlayer(SoundAction.ENEMY, playerName);
        allyPlayer.sendSystemMessage(Component.translatable("alliance.reignofnether.disbanded", playerName));
        SoundClientboundPacket.playSoundForPlayer(SoundAction.ENEMY, allyPlayerName);

        for (ServerPlayer serverPlayer : PlayerServerEvents.players) {
            if (!serverPlayer.equals(player) && !serverPlayer.equals(allyPlayer)) {
                serverPlayer.sendSystemMessage(Component.translatable("alliance.reignofnether.disbanded_third_party", playerName, allyPlayerName));
                SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, playerName);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int getAlliedControl(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String playerName = player.getName().getString();
        boolean value = AlliancesServerEvents.playersWithAlliedControl.contains(playerName);
        String msgKey = value ? "alliance.reignofnether.ally_control_enabled" : "alliance.reignofnether.ally_control_disabled";
        context.getSource().sendSuccess(()->Component.translatable(msgKey, playerName), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setAlliedControl(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String playerName = player.getName().getString();
        boolean value = BoolArgumentType.getBool(context, "value");
        String msgKey = value ? "alliance.reignofnether.ally_control_enable" : "alliance.reignofnether.ally_control_disable";

        if (value)
            AlliancesServerEvents.playersWithAlliedControl.add(playerName);
        else
            AlliancesServerEvents.playersWithAlliedControl.remove(playerName);

        AllianceClientboundPacket.setAllyControl(playerName, value);
        context.getSource().sendSuccess(()->Component.translatable(msgKey, playerName), false);
        SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, playerName);

        for (String allyPlayerName : AlliancesServerEvents.getAllAllies(playerName)) {
            PlayerServerEvents.sendMessageToPlayerNoNewLines(allyPlayerName, msgKey, false, playerName);
            SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, allyPlayerName);
        }
        return Command.SINGLE_SUCCESS;
    }
}
