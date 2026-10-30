package com.solegendary.reignofnether.startpos;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.chat.SystemMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

// the seat holder's answers to a join request by hand (the roster has buttons for it), and removing a co-commander later
public class CoCommanderCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cocommander")
                .then(Commands.literal("accept").then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> answer(ctx, true))))
                .then(Commands.literal("decline").then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> answer(ctx, false))))
                .then(Commands.literal("kick").then(Commands.argument("player", StringArgumentType.word())
                        .executes(CoCommanderCommand::kick))));
    }

    private static int answer(CommandContext<CommandSourceStack> ctx, boolean accept) throws CommandSyntaxException {
        String player = StringArgumentType.getString(ctx, "player");
        if (StartPosServerEvents.answerJoin(ctx.getSource().getPlayerOrException(), player, accept))
            return Command.SINGLE_SUCCESS;
        ctx.getSource().sendFailure(SystemMessage.error(Component.translatable("startpos.reignofnether.co_commander_no_request", player)));
        return 0;
    }

    private static int kick(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String player = StringArgumentType.getString(ctx, "player");
        if (StartPosServerEvents.kickCoCommander(ctx.getSource().getPlayerOrException(), player))
            return Command.SINGLE_SUCCESS;
        ctx.getSource().sendFailure(SystemMessage.error(Component.translatable("startpos.reignofnether.co_commander_not_yours", player)));
        return 0;
    }
}
