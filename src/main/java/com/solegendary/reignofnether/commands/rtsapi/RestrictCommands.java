package com.solegendary.reignofnether.commands.rtsapi;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.commands.rtsapi.argument.PlayerNameArgument;
import com.solegendary.reignofnether.restrictions.RestrictionsServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// /rtsapi restrict <player|*> deny|allow <id>, /rtsapi restrict <player|*> clear, /rtsapi restrict list
public class RestrictCommands {

    private static final SuggestionProvider<CommandSourceStack> IDS = (ctx, builder) -> SharedSuggestionProvider.suggestResource(
            Stream.of(
                ReignOfNetherRegistries.PRODUCTION_ITEM.keySet().stream(),
                ReignOfNetherRegistries.BUILDING.keySet().stream(),
                CustomBuildingServerEvents.customBuildings.stream().map(RonIds::of),
                Arrays.stream(UnitAction.values()).map(RonIds::actionId)
            ).flatMap(s -> s), builder);

    public static void register(final LiteralArgumentBuilder<CommandSourceStack> commandBuilder) {
        commandBuilder
            .then(Commands.literal("restrict")
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        if (RestrictionsServerEvents.getDenied().isEmpty())
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.reignofnether.restrict.list.empty"), false);
                        RestrictionsServerEvents.getDenied().forEach((player, ids) -> ctx.getSource().sendSuccess(
                            () -> Component.translatable("commands.reignofnether.restrict.list.entry", player,
                                ids.stream().map(ResourceLocation::toString).sorted().collect(Collectors.joining(", "))), false));
                        return RestrictionsServerEvents.getDenied().size();
                    })
                )
                .then(Commands.argument("player", PlayerNameArgument.players())
                    .then(Commands.literal("deny")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                            .suggests(IDS)
                            .executes(ctx -> {
                                String player = player(ctx);
                                ResourceLocation id = id(ctx, true);
                                if (id == null)
                                    return 0;
                                RestrictionsServerEvents.deny(player, id);
                                ctx.getSource().sendSuccess(() -> Component.translatable("commands.reignofnether.restrict.deny.success", id, player), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("allow")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                RestrictionsServerEvents.getDenied().getOrDefault(player(ctx), Set.of()).stream(), builder))
                            .executes(ctx -> {
                                String player = player(ctx);
                                ResourceLocation id = id(ctx, false);
                                RestrictionsServerEvents.allow(player, id);
                                ctx.getSource().sendSuccess(() -> Component.translatable("commands.reignofnether.restrict.allow.success", id, player), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("clear")
                        .executes(ctx -> {
                            String player = player(ctx);
                            RestrictionsServerEvents.clear(player);
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.reignofnether.restrict.clear.success", player), true);
                            return 1;
                        })
                    )
                )
            );
    }

    // "*" parses to a null name. A seat name is kept as typed: it means whoever plays that seat when it's checked
    private static String player(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = PlayerNameArgument.getTypedName(ctx, "player");
        return name == null ? RestrictionsServerEvents.ALL : name;
    }

    // strict: only known unit/research/building ids; otherwise anything goes, so stale entries can still be removed
    private static ResourceLocation id(CommandContext<CommandSourceStack> ctx, boolean strict) {
        ResourceLocation input = RonIds.arg(ctx, "id");
        ResourceLocation id = RonIds.restrictable(input.toString());
        if (id == null && strict)
            ctx.getSource().sendFailure(Component.translatable("commands.reignofnether.restrict.unknown", input));
        return id != null || strict ? id : input;
    }
}
