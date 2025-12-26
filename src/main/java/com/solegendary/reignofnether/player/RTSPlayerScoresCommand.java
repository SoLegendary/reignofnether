package com.solegendary.reignofnether.player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RTSPlayerScoresCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rts-scores")
                .executes(RTSPlayerScoresCommand::execute)
                .then(Commands.argument("target", EntityArgument.player())
                .executes((command) -> executeOnPlayer(command, EntityArgument.getPlayer(command, "target"))))
        );
    }

    public static int executeOnPlayer(CommandContext<CommandSourceStack> command, Player targetPlayer) {
        if (command.getSource().getEntity() instanceof Player player) {
            String targetName = targetPlayer.getDisplayName().getString();

            for (RTSPlayer rtsPlayer : PlayerServerEvents.postGameRtsPlayers) {
                if (Objects.equals(rtsPlayer.name, targetName)) {
                    player.sendSystemMessage(Component.literal(displayScores(rtsPlayer)));
                    return Command.SINGLE_SUCCESS;
                }
            }
            player.sendSystemMessage(Component.literal("Player is either currently in a match, is a spectator or doesn't exist."));
        }

        return Command.SINGLE_SUCCESS;
    }

    public static int execute(CommandContext<CommandSourceStack> command) {
        if (command.getSource().getEntity() instanceof Player player) {
            List<RTSPlayer> rtsPlayers = PlayerServerEvents.postGameRtsPlayers;
            player.sendSystemMessage(Component.literal(displayScores(rtsPlayers)));
        }

        return Command.SINGLE_SUCCESS;
    }

    public static String displayScores(RTSPlayer rtsPlayer) {
        String scores = "";
        HashMap<RTSPlayerScoresEnum, Integer> playerScoreList = rtsPlayer.scores.getScoreList();

        scores += rtsPlayer.name + "\n";

        for (RTSPlayerScoresEnum i : RTSPlayerScoresEnum.values()) {
            scores += i.toString() + ": " + playerScoreList.get(i) + "\n";
        }

        return scores;
    }

    public static String displayScores(List<RTSPlayer> rtsPlayerList) {
        if (rtsPlayerList.isEmpty()) {
            return "No players have been defeated yet.";
        }

        String scores = "      ";

        for (RTSPlayer rtsPlayer : rtsPlayerList) {
            scores += rtsPlayer.name + "   ";
        }

        scores += "\n";

        for (RTSPlayerScoresEnum i : RTSPlayerScoresEnum.values()) {
            scores += RTSPlayerScores.getScoreInitials(i) + ": ";
            for (RTSPlayer j : rtsPlayerList) {
                HashMap<RTSPlayerScoresEnum, Integer> playerScoreList = j.scores.getScoreList();
                scores += playerScoreList.get(i) + "   ";
            }
            scores += "\n";
        }

        return scores;
    }
}
