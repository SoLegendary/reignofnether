package com.solegendary.reignofnether.alliance;

import com.solegendary.reignofnether.chat.SystemMessage;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.solegendary.reignofnether.ai.BotServerEvents;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Alliance commands take a player name rather than an online player, so bots (RTS players with no
// ServerPlayer) can be allied with too; a request to a bot is answered by its BotDiplomacy.
public class AllyCommand {

    // online players and every RTS player (bots included)
    private static final SuggestionProvider<CommandSourceStack> NAMES = (ctx, builder) -> {
        Set<String> names = new TreeSet<>(Arrays.asList(ctx.getSource().getServer().getPlayerNames()));
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers)
                names.add(p.name);
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ally")
                .then(Commands.argument("player", StringArgumentType.word()).suggests(NAMES)
                        .executes(AllyCommand::ally)));

        dispatcher.register(Commands.literal("allyconfirm")
                .then(Commands.argument("player", StringArgumentType.word()).suggests(NAMES)
                        .executes(AllyCommand::allyConfirm)));

        dispatcher.register(Commands.literal("allycancelrequest")
                .then(Commands.argument("player", StringArgumentType.word()).suggests(NAMES)
                        .executes(AllyCommand::allyCancelRequest)));

        dispatcher.register(Commands.literal("disband")
                .then(Commands.argument("player", StringArgumentType.word()).suggests(NAMES)
                        .executes(AllyCommand::disband)));

        dispatcher.register(Commands.literal("allycontrol")
                .executes(AllyCommand::getAlliedControl));

        dispatcher.register(Commands.literal("allycontrol")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(AllyCommand::setAlliedControl)));
    }

    @Nullable
    private static ServerPlayer online(String name) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getPlayerList().getPlayerByName(name);
    }

    private static boolean exists(String name) {
        return online(name) != null || PlayerServerEvents.isRTSPlayer(name);
    }

    // a faction's players: the owner and their co-commanders
    private static void tell(String name, Component msg) {
        for (ServerPlayer p : PlayerServerEvents.players)
            if (PlayerRoles.ownerOf(p.getName().getString()).equals(name))
                p.sendSystemMessage(SystemMessage.info(msg));
    }

    private static void tellError(String name, Component msg) {
        for (ServerPlayer p : PlayerServerEvents.players)
            if (PlayerRoles.ownerOf(p.getName().getString()).equals(name))
                p.sendSystemMessage(SystemMessage.error(msg));
    }

    // the faction a command acts for: a co-commander's lead. Soldiers don't do diplomacy
    @Nullable
    private static String faction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = context.getSource().getPlayerOrException().getName().getString();
        if (PlayerRoles.roleOf(name) == PlayerRole.SOLDIER) {
            context.getSource().sendFailure(SystemMessage.error(Component.translatable("alliance.reignofnether.soldier_no_diplomacy")));
            return null;
        }
        return PlayerRoles.ownerOf(name);
    }

    private static void tellOthers(String a, String b, Component msg) {
        for (ServerPlayer p : PlayerServerEvents.players) {
            String n = p.getName().getString();
            if (!n.equals(a) && !n.equals(b)) {
                p.sendSystemMessage(SystemMessage.info(msg));
                SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, n);
            }
        }
    }

    // null when alliances between these two may change now, else why not
    @Nullable
    public static Component lockedReason(String a, String b) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return null;
        boolean rts = PlayerServerEvents.isRTSPlayer(a) || PlayerServerEvents.isRTSPlayer(b);
        if (rts && server.getGameRules().getRule(GameRuleRegistrar.LOCK_ALLIANCES).get())
            return Component.translatable("alliance.reignofnether.alliances_lock");
        if (rts && server.getGameRules().getRule(GameRuleRegistrar.COOP_MODE).get())
            return Component.translatable("alliance.reignofnether.alliances_lock_coop");
        return null;
    }

    private static int ally(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String playerName = faction(context);
        if (playerName == null)
            return 0;
        String target = PlayerRoles.ownerOf(StringArgumentType.getString(context, "player"));
        if (!exists(target)) {
            context.getSource().sendFailure(SystemMessage.error(Component.translatable("argument.entity.notfound.player")));
            return 0;
        }
        Component locked = lockedReason(playerName, target);
        if (locked != null) {
            tellError(playerName, locked);
            return 0;
        }
        if (playerName.equals(target)) {
            tellError(playerName, Component.translatable("alliance.reignofnether.ally_self", playerName));
            return 0;
        }
        requestAlliance(playerName, target);
        return Command.SINGLE_SUCCESS;
    }

    // from asks to to ally; a bot target decides on its own (BotDiplomacy)
    public static void requestAlliance(String from, String to) {
        AlliancesServerEvents.pendingAlliances.computeIfAbsent(to, k -> new HashSet<>()).add(from);
        AllianceClientboundPacket.addPendingAlliance(to, from);
        tell(from, Component.translatable("alliance.reignofnether.sent_request", to));
        SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, to);
        tell(to, Component.translatable("alliance.reignofnether.ally_confirm", from, from));
        if (PlayerServerEvents.isRTSPlayer(to))
            tell(to, Component
                .translatable("alliance.reignofnether.accept") // [CLick to accept]
                .withStyle(Style.EMPTY
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/allyconfirm " + from))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("alliance.reignofnether.accept"))))
                .append(Component
                    .translatable("alliance.reignofnether.cancel") // [Click to cancel]
                    .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/allycancelrequest " + from))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("alliance.reignofnether.cancel")))
                    )
                )
            );
        SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, from);
        BotServerEvents.onAllyRequest(from, to);
    }

    private static int allyCancelRequest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String playerName = faction(context);
        if (playerName == null)
            return 0;
        String other = StringArgumentType.getString(context, "player");
        if (playerName.equals(other))
            return 0;
        cancelRequest(playerName, other);
        return Command.SINGLE_SUCCESS;
    }

    // drops a request between the two either way: a requester taking it back, or the target turning it down
    public static void cancelRequest(String playerName, String other) {
        AlliancesServerEvents.pendingAlliances.getOrDefault(playerName, new HashSet<>()).remove(other);
        AlliancesServerEvents.pendingAlliances.getOrDefault(other, new HashSet<>()).remove(playerName);
        AllianceClientboundPacket.cancelPendingAlliance(other, playerName);
        tell(playerName, Component.translatable("alliance.reignofnether.ally_request_cancelled_self", other));
        SoundClientboundPacket.playSoundForPlayer(SoundAction.ENEMY, other);
        tell(other, Component.translatable("alliance.reignofnether.ally_request_cancelled", playerName));
        SoundClientboundPacket.playSoundForPlayer(SoundAction.ENEMY, playerName);
    }

    public static boolean hasRequest(String target, String requester) {
        return AlliancesServerEvents.pendingAlliances.getOrDefault(target, Set.of()).contains(requester);
    }

    private static int allyConfirm(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String playerName = faction(context);
        if (playerName == null)
            return 0;
        String requester = StringArgumentType.getString(context, "player");
        if (!confirmAlliance(playerName, requester))
            context.getSource().sendFailure(SystemMessage.error(Component.translatable("alliance.reignofnether.no_request", requester)));
        return Command.SINGLE_SUCCESS;
    }

    // playerName accepts requester's pending request; false if there's none
    public static boolean confirmAlliance(String playerName, String requester) {
        if (!hasRequest(playerName, requester))
            return false;
        AlliancesServerEvents.addAlliance(playerName, requester);
        AlliancesServerEvents.pendingAlliances.getOrDefault(playerName, new HashSet<>()).remove(requester);
        AlliancesServerEvents.pendingAlliances.getOrDefault(requester, new HashSet<>()).remove(playerName);

        tell(playerName, Component.translatable("alliance.reignofnether.now_allied", requester));
        SoundClientboundPacket.playSoundForPlayer(SoundAction.ALLY, requester);
        tell(requester, Component.translatable("alliance.reignofnether.ally_accepted", playerName));
        SoundClientboundPacket.playSoundForPlayer(SoundAction.ALLY, playerName);
        tellOthers(playerName, requester, Component.translatable("alliance.reignofnether.now_allied_third_party", playerName, requester));
        return true;
    }

    private static int disband(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String playerName = faction(context);
        if (playerName == null)
            return 0;
        String allyName = PlayerRoles.ownerOf(StringArgumentType.getString(context, "player"));
        Component locked = lockedReason(playerName, allyName);
        if (locked != null) {
            tellError(playerName, locked);
            return 0;
        }
        if (playerName.equals(allyName)) {
            context.getSource().sendFailure(SystemMessage.error(Component.translatable("alliance.reignofnether.disband_self")));
            return 0;
        }
        disbandAlliance(playerName, allyName);
        return Command.SINGLE_SUCCESS;
    }

    public static void disbandAlliance(String playerName, String allyName) {
        AlliancesServerEvents.removeAlliance(playerName, allyName);
        tell(playerName, Component.translatable("alliance.reignofnether.disbanded", allyName));
        SoundClientboundPacket.playSoundForPlayer(SoundAction.ENEMY, playerName);
        tell(allyName, Component.translatable("alliance.reignofnether.disbanded", playerName));
        SoundClientboundPacket.playSoundForPlayer(SoundAction.ENEMY, allyName);
        tellOthers(playerName, allyName, Component.translatable("alliance.reignofnether.disbanded_third_party", playerName, allyName));
        BotServerEvents.onDisbanded(playerName, allyName);
    }

    private static int getAlliedControl(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String playerName = faction(context);
        if (playerName == null)
            return 0;
        boolean value = AlliancesServerEvents.playersWithAlliedControl.contains(playerName);
        String msgKey = value ? "alliance.reignofnether.ally_control_enabled" : "alliance.reignofnether.ally_control_disabled";
        context.getSource().sendSuccess(()->SystemMessage.info(Component.translatable(msgKey, playerName)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setAlliedControl(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String playerName = faction(context);
        if (playerName == null)
            return 0;
        boolean value = BoolArgumentType.getBool(context, "value");
        String msgKey = value ? "alliance.reignofnether.ally_control_enable" : "alliance.reignofnether.ally_control_disable";

        if (value)
            AlliancesServerEvents.playersWithAlliedControl.add(playerName);
        else
            AlliancesServerEvents.playersWithAlliedControl.remove(playerName);

        AllianceClientboundPacket.setAllyControl(playerName, value);
        context.getSource().sendSuccess(()->SystemMessage.info(Component.translatable(msgKey, playerName)), false);
        SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, playerName);

        for (String allyPlayerName : AlliancesServerEvents.getAllAllies(playerName)) {
            PlayerServerEvents.sendMessageToPlayer(allyPlayerName, msgKey, false, playerName);
            SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, allyPlayerName);
        }
        return Command.SINGLE_SUCCESS;
    }
}
