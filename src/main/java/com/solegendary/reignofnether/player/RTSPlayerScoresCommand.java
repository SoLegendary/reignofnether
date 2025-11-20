package com.solegendary.reignofnether.player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class RTSPlayerScoresCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rts-scores").executes((command) -> {
            return execute(command);
        }));
    }

    public static int execute(CommandContext<CommandSourceStack> command) {
        if (command.getSource().getEntity() instanceof Player) {
            Player player = (Player) command.getSource().getEntity();
            player.sendSystemMessage(Component.literal("Boomba"));
        }

        return Command.SINGLE_SUCCESS;
    }
}
