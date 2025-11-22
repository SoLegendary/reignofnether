package com.solegendary.reignofnether.player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;

public class RTSPlayerScoresCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rts-scores").executes((command) -> {
            return execute(command);
        }));
    }

    public static int execute(CommandContext<CommandSourceStack> command) {
        if (command.getSource().getEntity() instanceof Player) {
            Player player = (Player) command.getSource().getEntity();
            List<RTSPlayer> rtsPlayers = PlayerServerEvents.getRTSPlayers();

            player.sendSystemMessage(Component.literal(displayScores(rtsPlayers)));
        }

        return Command.SINGLE_SUCCESS;
    }

    public static String displayScores(List<RTSPlayer> rtsPlayerList) {
        String scores = "";

        for (RTSPlayer rtsPlayer : rtsPlayerList) {
            scores += rtsPlayer.name + "   ";
        }

        scores += "\n";

        for (RTSPlayerScoresEnum i : RTSPlayerScoresEnum.values()) {
            for (RTSPlayer j : rtsPlayerList) {
                HashMap<RTSPlayerScoresEnum, Integer> playerScoreList = j.scores.getScoreList();
                scores += playerScoreList.get(i) + "   ";
            }
            scores += "\n";
        }

        return scores;
    }
}
