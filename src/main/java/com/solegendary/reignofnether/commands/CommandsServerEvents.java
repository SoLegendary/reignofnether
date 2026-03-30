package com.solegendary.reignofnether.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitActionItem;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class CommandsServerEvents {

    private static final SimpleCommandExceptionType UNKNOWN_BUILDING =
        new SimpleCommandExceptionType(Component.translatable("commands.reignofnether.error.unknown_building"));
    private static final SimpleCommandExceptionType UNKNOWN_RESOURCE =
        new SimpleCommandExceptionType(Component.translatable("commands.reignofnether.error.unknown_resource"));
    private static final SimpleCommandExceptionType NO_SERVER_LEVEL =
        new SimpleCommandExceptionType(Component.translatable("commands.reignofnether.error.no_server_level"));

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("rtsapi-place-building")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("buildingName", StringArgumentType.string())
                .then(placeBuildingTail(ctx -> ""))
                .then(Commands.argument("ownerName", StringArgumentType.string())
                    .then(placeBuildingTail(ctx -> StringArgumentType.getString(ctx, "ownerName")))
                )
                .then(Commands.argument("ownerSelector", EntityArgument.player())
                    .then(placeBuildingTail(ctx -> getPlayerName(EntityArgument.getPlayer(ctx, "ownerSelector"))))
                )
            )
        );

        dispatcher.register(Commands.literal("rtsapi-destroy-building")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> destroyBuildingsAt(BlockPosArgument.getLoadedBlockPos(ctx, "pos"), ctx.getSource()))
            )
        );

        dispatcher.register(Commands.literal("rtsapi-set-unit-owner")
                .requires(source -> source.hasPermission(2))
                .then(setUnitOwnerTail(ctx -> ""))
                .then(Commands.argument("ownerName", StringArgumentType.string())
                        .then(setUnitOwnerTail(ctx -> StringArgumentType.getString(ctx, "ownerName")))
                        .then(Commands.argument("newOwnerName", StringArgumentType.string())
                                .then(setUnitOwnerTail(
                                        ctx -> StringArgumentType.getString(ctx, "ownerName"),
                                        ctx -> StringArgumentType.getString(ctx, "newOwnerName")
                                ))
                        )
                        .then(Commands.argument("newOwnerSelector", EntityArgument.player())
                                .then(setUnitOwnerTail(
                                        ctx -> StringArgumentType.getString(ctx, "ownerName"),
                                        ctx -> getPlayerName(EntityArgument.getPlayer(ctx, "newOwnerSelector"))
                                ))
                        )
                )
                .then(Commands.argument("ownerSelector", EntityArgument.player())
                        .then(setUnitOwnerTail(ctx -> getPlayerName(EntityArgument.getPlayer(ctx, "ownerSelector"))))
                        .then(Commands.argument("newOwnerName", StringArgumentType.string())
                                .then(setUnitOwnerTail(
                                        ctx -> getPlayerName(EntityArgument.getPlayer(ctx, "ownerSelector")),
                                        ctx -> StringArgumentType.getString(ctx, "newOwnerName")
                                ))
                        )
                        .then(Commands.argument("newOwnerSelector", EntityArgument.player())
                                .then(setUnitOwnerTail(
                                        ctx -> getPlayerName(EntityArgument.getPlayer(ctx, "ownerSelector")),
                                        ctx -> getPlayerName(EntityArgument.getPlayer(ctx, "newOwnerSelector"))
                                ))
                        )
                )
        );

        dispatcher.register(Commands.literal("rtsapi-set-building-owner")
            .requires(source -> source.hasPermission(2))
            .then(buildingSelectionTail(ctx -> ""))
            .then(Commands.argument("ownerName", StringArgumentType.string())
                .then(buildingSelectionTail(ctx -> StringArgumentType.getString(ctx, "ownerName")))
            )
            .then(Commands.argument("ownerSelector", EntityArgument.player())
                .then(buildingSelectionTail(ctx -> getPlayerName(EntityArgument.getPlayer(ctx, "ownerSelector"))))
            )
        );

        dispatcher.register(Commands.literal("rtsapi-set-anchor")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("from", BlockPosArgument.blockPos())
                .then(Commands.argument("to", BlockPosArgument.blockPos())
                    .then(Commands.argument("anchor", BlockPosArgument.blockPos())
                        .executes(ctx -> setAnchor(
                            ctx,
                            BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                            BlockPosArgument.getLoadedBlockPos(ctx, "to"),
                            BlockPosArgument.getLoadedBlockPos(ctx, "anchor")
                        ))
                    )
                )
            )
        );

        dispatcher.register(Commands.literal("rtsapi-remove-anchor")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("from", BlockPosArgument.blockPos())
                .then(Commands.argument("to", BlockPosArgument.blockPos())
                    .executes(ctx -> removeAnchor(
                        ctx,
                        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                        BlockPosArgument.getLoadedBlockPos(ctx, "to")
                    ))
                )
            )
        );

        dispatcher.register(Commands.literal("rtsapi-change-resources")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("resource", StringArgumentType.string())
                .then(Commands.argument("amount", IntegerArgumentType.integer())
                    .then(Commands.argument("playerName", StringArgumentType.string())
                        .executes(ctx -> changeResources(
                            ctx,
                            StringArgumentType.getString(ctx, "resource"),
                            IntegerArgumentType.getInteger(ctx, "amount"),
                            StringArgumentType.getString(ctx, "playerName")
                        ))
                    )
                    .then(Commands.argument("playerSelector", EntityArgument.player())
                        .executes(ctx -> changeResources(
                            ctx,
                            StringArgumentType.getString(ctx, "resource"),
                            IntegerArgumentType.getInteger(ctx, "amount"),
                            getPlayerName(EntityArgument.getPlayer(ctx, "playerSelector"))
                        ))
                    )
                )
            )
        );

        dispatcher.register(Commands.literal("rtsapi-unit-action")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("ownerName", StringArgumentType.string())
                        .then(unitActionTail(ctx -> StringArgumentType.getString(ctx, "ownerName")))
                )
                .then(Commands.argument("ownerSelector", EntityArgument.player())
                        .then(unitActionTail(ctx -> getPlayerName(EntityArgument.getPlayer(ctx, "ownerSelector"))))
                )
        );

        dispatcher.register(Commands.literal("rtsapi-unit-action")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("ownerName", StringArgumentType.string())
                .then(unitActionTail(ctx -> StringArgumentType.getString(ctx, "ownerName")))
            )
            .then(Commands.argument("ownerSelector", EntityArgument.player())
                .then(unitActionTail(ctx -> getPlayerName(EntityArgument.getPlayer(ctx, "ownerSelector"))))
            )
        );

        dispatcher.register(Commands.literal("rtsapi-victory")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("ownerName", StringArgumentType.string())
                .then(Commands.argument("reason", StringArgumentType.string())
                    .executes(ctx -> victoryPlayer(
                        StringArgumentType.getString(ctx, "ownerName"),
                        StringArgumentType.getString(ctx, "reason")
                ))))
            .then(Commands.argument("ownerSelector", EntityArgument.player())
                .then(Commands.argument("reason", StringArgumentType.string())
                    .executes(ctx -> victoryPlayer(
                        StringArgumentType.getString(ctx, "ownerName"),
                        StringArgumentType.getString(ctx, "reason")
                    ))))
        );

        dispatcher.register(Commands.literal("rtsapi-defeat")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("ownerName", StringArgumentType.string())
                .then(Commands.argument("reason", StringArgumentType.string())
                    .executes(ctx -> defeatPlayer(
                        StringArgumentType.getString(ctx, "ownerName"),
                        StringArgumentType.getString(ctx, "reason")
                    ))))
            .then(Commands.argument("ownerSelector", EntityArgument.player())
                .then(Commands.argument("reason", StringArgumentType.string())
                    .executes(ctx -> defeatPlayer(
                        StringArgumentType.getString(ctx, "ownerName"),
                        StringArgumentType.getString(ctx, "reason")
                    ))))
        );

        dispatcher.register(Commands.literal("rtsapi-summon")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("ownerName", StringArgumentType.string())
                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                        BuiltInRegistries.ENTITY_TYPE.keySet().stream(), builder))
                                .executes(ctx -> summonEntity(
                                        ctx,
                                        StringArgumentType.getString(ctx, "ownerName"),
                                        ResourceLocationArgument.getId(ctx, "entity"),
                                        BlockPos.containing(ctx.getSource().getPosition()),
                                        null
                                ))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> summonEntity(
                                                ctx,
                                                StringArgumentType.getString(ctx, "ownerName"),
                                                ResourceLocationArgument.getId(ctx, "entity"),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
                                                null
                                        ))
                                        .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> summonEntity(
                                                        ctx,
                                                        StringArgumentType.getString(ctx, "ownerName"),
                                                        ResourceLocationArgument.getId(ctx, "entity"),
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "nbt")
                                                ))
                                        )
                                )
                        )
                )
        );

        dispatcher.register(Commands.literal("rtsapi-add-research")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("researchItem", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                ReignOfNetherRegistries.PRODUCTION_ITEM.keySet().stream(), builder))
                        .then(Commands.argument("playerName", StringArgumentType.string())
                                .executes(ctx -> addResearch(
                                        ctx,
                                        ResourceLocationArgument.getId(ctx, "researchItem"),
                                        StringArgumentType.getString(ctx, "playerName")
                                ))
                        )
                        .then(Commands.argument("playerSelector", EntityArgument.player())
                                .executes(ctx -> addResearch(
                                        ctx,
                                        ResourceLocationArgument.getId(ctx, "researchItem"),
                                        getPlayerName(EntityArgument.getPlayer(ctx, "playerSelector"))
                                ))
                        )
                )
        );

        dispatcher.register(Commands.literal("rtsapi-remove-research")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("researchItem", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                ReignOfNetherRegistries.PRODUCTION_ITEM.keySet().stream(), builder))
                        .then(Commands.argument("playerName", StringArgumentType.string())
                                .executes(ctx -> removeResearch(
                                        ctx,
                                        ResourceLocationArgument.getId(ctx, "researchItem"),
                                        StringArgumentType.getString(ctx, "playerName")
                                ))
                        )
                        .then(Commands.argument("playerSelector", EntityArgument.player())
                                .executes(ctx -> removeResearch(
                                        ctx,
                                        ResourceLocationArgument.getId(ctx, "researchItem"),
                                        getPlayerName(EntityArgument.getPlayer(ctx, "playerSelector"))
                                ))
                        )
                )
        );

        dispatcher.register(Commands.literal("rtsapi-set-unit-enemy-search")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("ownerName", StringArgumentType.string())
                        .then(searchBehaviourTail(ctx -> StringArgumentType.getString(ctx, "ownerName")))
                )
                .then(Commands.argument("ownerSelector", EntityArgument.player())
                        .then(searchBehaviourTail(ctx -> getPlayerName(EntityArgument.getPlayer(ctx, "ownerSelector"))))
                )
        );

        dispatcher.register(Commands.literal("rtsapi-set-camera")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("playerName", StringArgumentType.string())
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setRTSCamera(
                                        ctx,
                                        StringArgumentType.getString(ctx, "playerName"),
                                        BoolArgumentType.getBool(ctx, "value")
                                ))))
                .then(Commands.argument("playerSelector", EntityArgument.player())
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setRTSCamera(
                                        ctx,
                                        StringArgumentType.getString(ctx, "playerSelector"),
                                        BoolArgumentType.getBool(ctx, "value")
                                ))))
        );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> placeBuildingTail(NameResolver ownerResolver) {
        return Commands.argument("autoBuild", BoolArgumentType.bool())
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> placeBuilding(
                    ctx,
                    StringArgumentType.getString(ctx, "buildingName"),
                    ownerResolver.resolve(ctx),
                    BoolArgumentType.getBool(ctx, "autoBuild"),
                    BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
                    Rotation.NONE
                ))
                // with rotation
                .then(Commands.argument("rotation", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        return SharedSuggestionProvider.suggest(
                                List.of("0", "90", "180", "270"),
                                builder
                        );
                    })
                    .executes(ctx -> placeBuilding(
                            ctx,
                            StringArgumentType.getString(ctx, "buildingName"),
                            ownerResolver.resolve(ctx),
                            BoolArgumentType.getBool(ctx, "autoBuild"),
                            BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
                            parseRotation(StringArgumentType.getString(ctx, "rotation"))
                    ))
                )
            );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> unitActionTail(NameResolver ownerResolver) {
        return Commands.argument("selectFrom", BlockPosArgument.blockPos())
                .then(Commands.argument("selectTo", BlockPosArgument.blockPos())
                        .then(Commands.argument("action", StringArgumentType.word())
                                .suggests((ctx, builder) ->
                                        SharedSuggestionProvider.suggest(
                                                List.of(
                                                        UnitAction.NONE.name(),
                                                        UnitAction.ATTACK.name(),
                                                        UnitAction.ATTACK_BUILDING.name(),
                                                        UnitAction.STOP.name(),
                                                        UnitAction.HOLD.name(),
                                                        UnitAction.MOVE.name(),
                                                        UnitAction.GARRISON.name(),
                                                        UnitAction.UNGARRISON.name(),
                                                        UnitAction.ATTACK_MOVE.name(),
                                                        UnitAction.FOLLOW.name(),
                                                        UnitAction.BUILD_REPAIR.name(),
                                                        UnitAction.FARM.name(),
                                                        UnitAction.RETURN_RESOURCES.name(),
                                                        UnitAction.RETURN_RESOURCES_TO_CLOSEST.name(),
                                                        UnitAction.DELETE.name()
                                                ),
                                                builder
                                        )
                                )
                                // (1) action only – no position or entity target
                                .executes(ctx -> issueUnitAction(
                                        ctx,
                                        ownerResolver.resolve(ctx),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "selectFrom"),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "selectTo"),
                                        StringArgumentType.getString(ctx, "action"),
                                        null,   // targetPos
                                        null
                                ))
                                // (2) action + targetPos (MOVE, ATTACK_MOVE, GARRISON, BUILD_REPAIR, FARM, RETURN_RESOURCES, etc)
                                .then(Commands.argument("targetPos", BlockPosArgument.blockPos())
                                        .executes(ctx -> issueUnitAction(
                                                ctx,
                                                ownerResolver.resolve(ctx),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "selectFrom"),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "selectTo"),
                                                StringArgumentType.getString(ctx, "action"),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "targetPos"),
                                                null
                                        ))
                                )
                                // (3) action + target unit (first unit found between targetFrom to targetTo) (ATTACK, FOLLOW, entity-targeting abilities)
                                .then(Commands.argument("targetFrom", BlockPosArgument.blockPos())
                                    .then(Commands.argument("targetTo", BlockPosArgument.blockPos())
                                            .executes(ctx -> issueUnitAction(
                                                    ctx,
                                                    ownerResolver.resolve(ctx),
                                                    BlockPosArgument.getLoadedBlockPos(ctx, "selectFrom"),
                                                    BlockPosArgument.getLoadedBlockPos(ctx, "selectTo"),
                                                    StringArgumentType.getString(ctx, "action"),
                                                    BlockPosArgument.getLoadedBlockPos(ctx, "targetFrom"),
                                                    BlockPosArgument.getLoadedBlockPos(ctx, "targetTo")
                                            ))
                                    )
                                )
                        )
                );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> searchBehaviourTail(NameResolver ownerResolver) {
        return Commands.argument("selectFrom", BlockPosArgument.blockPos())
                .then(Commands.argument("selectTo", BlockPosArgument.blockPos())
                        .then(Commands.argument("behaviour", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        List.of(
                                                EnemySearchBehaviour.NEAREST_ENEMY_BUILDING.name(),
                                                EnemySearchBehaviour.NEAREST_ENEMY_UNIT.name(),
                                                EnemySearchBehaviour.NEAREST_ENEMY_WORKER.name(),
                                                EnemySearchBehaviour.NONE.name()
                                        ),
                                        builder
                                ))
                                .executes(ctx -> setUnitSearchBehaviour(
                                        ctx,
                                        ownerResolver.resolve(ctx),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "selectFrom"),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "selectTo"),
                                        StringArgumentType.getString(ctx, "behaviour")
                                ))
                        )
                );
    }

    /**
     * Command-block friendly way to issue UnitActions
     * @param ownerName  selects only units with this ownerName
     * @param selectFrom start of unit selection range
     * @param selectTo   end of unit selection range
     * @param actionName string value of UnitAction to enact
     * @param targetFrom targeted pos (or start of target selection range if targetTo is non-null)
     * @param targetTo   end of target selection range
     */
    private static int issueUnitAction(
            CommandContext<CommandSourceStack> ctx,
            String ownerName,
            BlockPos selectFrom,
            BlockPos selectTo,
            String actionName,
            BlockPos targetFrom,
            BlockPos targetTo
    ) {
        UnitAction action;
        try {
            action = UnitAction.valueOf(actionName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown action '" + actionName + "'. Valid values: " +
                            java.util.Arrays.stream(UnitAction.values())
                                    .map(a -> a.name().toLowerCase())
                                    .collect(java.util.stream.Collectors.joining(", "))
            ));
            return 0;
        }
        int[] unitIds = collectUnitIds(selectFrom, selectTo);
        if (unitIds.length == 0) {
            ctx.getSource().sendFailure(Component.literal("No units found in the selection"));
            return 0;
        }
        int[] targetUnitIds = new int[]{};
        if (targetFrom != null && targetTo != null) { // find one
            targetUnitIds = collectUnitIds(targetFrom, targetTo);
        }

        UnitActionItem item = new UnitActionItem(
                ownerName,
                action,
                targetUnitIds.length != 0 ? targetUnitIds[0] : -1,
                unitIds,
                targetFrom != null ? targetFrom : new BlockPos(0,0,0),
                new BlockPos(0,0,0)
        );
        item.action(ctx.getSource().getLevel());

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Issued " + action.name().toLowerCase() +
                                " to " + unitIds.length + " unit(s)" +
                                (ownerName.isEmpty() ? "" : " (owner: " + ownerName + ")")
                ),
                true
        );
        return unitIds.length;
    }

    private static int victoryPlayer(
            String ownerName,
            String reason
    ) {
        int playersDefeated = 0;
        ArrayList<String> playersToDefeat = new ArrayList<>();
        synchronized (PlayerServerEvents.rtsPlayers) {
            if (PlayerServerEvents.isRTSPlayer(ownerName))
                for (RTSPlayer rtsPlayer : PlayerServerEvents.rtsPlayers)
                    if (!rtsPlayer.name.equals(ownerName) && !AlliancesServerEvents.isAllied(rtsPlayer.name, ownerName))
                        playersToDefeat.add(rtsPlayer.name);
        }
        for (String playerName : playersToDefeat) {
            PlayerServerEvents.defeat(playerName, reason);
            playersDefeated += 1;
        }
        return playersDefeated;
    }

    private static int defeatPlayer(
            String ownerName,
            String reason
    ) {
        synchronized (PlayerServerEvents.rtsPlayers) {
            if (PlayerServerEvents.isRTSPlayer(ownerName)) {
                PlayerServerEvents.defeat(ownerName, reason);
                return 1;
            }
        }
        return 0;
    }

    private static int summonEntity(
            CommandContext<CommandSourceStack> ctx,
            String ownerName,
            ResourceLocation entityId,
            BlockPos pos,
            CompoundTag nbt
    ) {
        ServerLevel level = ctx.getSource().getLevel();

        CompoundTag tag = nbt != null ? nbt.copy() : new CompoundTag();
        tag.putString("id", entityId.toString());

        Entity entity = EntityType.loadEntityRecursive(tag, level, e -> {
                    e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                            e.getYRot(), e.getXRot());
                    return e;
                });

        if (entity == null) {
            ctx.getSource().sendFailure(Component.literal("Failed to create entity: " + entityId));
            return 0;
        }
        level.addFreshEntity(entity);

        if (entity instanceof Unit unit) {
            unit.setOwnerName(ownerName);
            UnitSyncClientboundPacket.sendSyncOwnerNamePacket(unit);
        }

        ctx.getSource().sendSuccess(
                () -> Component.literal("Summoned " + entityId + " for " + ownerName + " at " + formatPos(pos)), true
        );
        return 1;
    }

    private static Rotation parseRotation(String input) throws CommandSyntaxException {
        return switch (input.toLowerCase()) {
            case "0" -> Rotation.NONE;
            case "90" -> Rotation.CLOCKWISE_90;
            case "180" -> Rotation.CLOCKWISE_180;
            case "270" -> Rotation.COUNTERCLOCKWISE_90;
            default -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
                    .create("Invalid rotation: " + input);
        };
    }

    private static ArgumentBuilder<CommandSourceStack, ?> setUnitOwnerTail(NameResolver ownerResolver) {
        return setUnitOwnerTail(null, ownerResolver);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> setUnitOwnerTail(NameResolver currentOwnerResolver, NameResolver newOwnerResolver) {
        return Commands.argument("from", BlockPosArgument.blockPos())
                .then(Commands.argument("to", BlockPosArgument.blockPos())
                        .executes(ctx -> setUnitOwner(
                                ctx,
                                currentOwnerResolver != null ? currentOwnerResolver.resolve(ctx) : null,
                                newOwnerResolver.resolve(ctx),
                                BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                                BlockPosArgument.getLoadedBlockPos(ctx, "to")
                        ))
                );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildingSelectionTail(NameResolver ownerResolver) {
        return Commands.argument("from", BlockPosArgument.blockPos())
            .then(Commands.argument("to", BlockPosArgument.blockPos())
                .executes(ctx -> setBuildingOwner(
                    ctx,
                    ownerResolver.resolve(ctx),
                    BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                    BlockPosArgument.getLoadedBlockPos(ctx, "to")
                ))
            );
    }

    private static String getPlayerName(ServerPlayer player) {
        return player.getName().getString();
    }

    @FunctionalInterface
    private interface NameResolver {
        String resolve(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

    private static int placeBuilding(
        CommandContext<CommandSourceStack> ctx,
        String buildingName,
        String ownerName,
        boolean autoBuild,
        BlockPos pos,
        Rotation rotation
    ) throws CommandSyntaxException {
        Building building = resolveBuilding(buildingName);
        if (building == null) {
            throw UNKNOWN_BUILDING.create();
        }
        if (BuildingServerEvents.getServerLevel() == null) {
            throw NO_SERVER_LEVEL.create();
        }

        BuildingPlacement placement = BuildingServerEvents.placeBuilding(
            building,
            pos,
            rotation,
            ownerName,
            new int[0],
            false,
            false
        );
        if (placement == null) {
            ctx.getSource().sendFailure(Component.literal("Unable to place building at " + formatPos(pos)));
            return 0;
        }
        if (autoBuild) {
            placement.selfBuilding = true;
        }
        ctx.getSource().sendSuccess(
            () -> Component.literal("Placed " + building.name + " for " + ownerName + " at " + formatPos(pos)),
            true
        );
        return 1;
    }

    private static int destroyBuildingsAt(BlockPos pos, CommandSourceStack source) {
        int removed = 0;
        for (BuildingPlacement placement : BuildingServerEvents.getBuildings()) {
            if (!placement.isPosInsideBuilding(pos)) continue;
            placement.destroy(source.getLevel());
            removed++;
        }

        if (removed == 0) {
            source.sendFailure(Component.literal("No buildings found at " + formatPos(pos)));
        } else {
            int finalRemoved = removed;
            source.sendSuccess(
                () -> Component.literal("Destroyed " + finalRemoved + " building(s) at " + formatPos(pos)),
                true
            );
        }
        return removed;
    }

    private static int setUnitOwner(
        CommandContext<CommandSourceStack> ctx,
        String currentOwnerName,
        String ownerName,
        BlockPos from,
        BlockPos to
    ) {
        BlockPos min = min(from, to);
        BlockPos max = max(from, to);

        List<Integer> ids = new ArrayList<>();
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof Unit unit &&
                (currentOwnerName == null || unit.getOwnerName().equals(currentOwnerName)) &&
                isWithin(entity.getOnPos(), min, max)) {
                ids.add(entity.getId());
            }
        }
        var idArray = new int[ids.size()];
        for (int i = 0; i < idArray.length; i++) {
            idArray[i] = ids.get(i);
        }
        SandboxServer.setUnitOwner(idArray, ownerName);
        ctx.getSource().sendSuccess(
            () -> Component.literal("Assigned " + ids.size() + " unit(s) to " + ownerName),
            true
        );
        return ids.size();
    }

    private static int setBuildingOwner(
        CommandContext<CommandSourceStack> ctx,
        String ownerName,
        BlockPos from,
        BlockPos to
    ) {
        BlockPos min = min(from, to);
        BlockPos max = max(from, to);
        int changed = 0;
        for (BuildingPlacement placement : BuildingServerEvents.getBuildings()) {
            if (intersects(placement, min, max)) {
                placement.ownerName = ownerName;
                changed++;
            }
        }
        if (changed == 0) {
            ctx.getSource().sendFailure(Component.literal("No buildings found in selection"));
        } else {
            int finalChanged = changed;
            ctx.getSource().sendSuccess(
                () -> Component.literal("Updated owner for " + finalChanged + " building(s)"),
                true
            );
        }
        return changed;
    }

    private static int setAnchor(
        CommandContext<CommandSourceStack> ctx,
        BlockPos from,
        BlockPos to,
        BlockPos anchor
    ) {
        int[] ids = collectUnitIds(from, to);
        SandboxServer.setAnchor(ids, anchor);
        ctx.getSource().sendSuccess(
            () -> Component.literal("Set anchor for " + ids.length + " unit(s)"),
            true
        );
        return ids.length;
    }

    private static int removeAnchor(
        CommandContext<CommandSourceStack> ctx,
        BlockPos from,
        BlockPos to
    ) {
        int[] ids = collectUnitIds(from, to);
        SandboxServer.removeAnchor(ids);
        ctx.getSource().sendSuccess(
            () -> Component.literal("Removed anchor for " + ids.length + " unit(s)"),
            true
        );
        return ids.length;
    }

    private static int changeResources(
        CommandContext<CommandSourceStack> ctx,
        String resourceName,
        int amount,
        String playerName
    ) throws CommandSyntaxException {
        ResourceName resource = resolveResource(resourceName);
        int food = resource == ResourceName.FOOD ? amount : 0;
        int wood = resource == ResourceName.WOOD ? amount : 0;
        int ore = resource == ResourceName.ORE ? amount : 0;

        ResourcesServerEvents.addSubtractResources(new Resources(playerName, food, wood, ore));
        ctx.getSource().sendSuccess(
            () -> Component.literal("Changed " + resource.name().toLowerCase() + " by " + amount + " for " + playerName),
            true
        );
        return 1;
    }

    private static int addResearch(
            CommandContext<CommandSourceStack> ctx,
            ResourceLocation researchItemName,
            String playerName
    ) {
        ResearchServerEvents.addResearch(playerName, researchItemName);
        ResearchServerEvents.syncResearch(playerName);
        ctx.getSource().sendSuccess(
                () -> Component.literal("Added research '" + researchItemName + "' for " + playerName),
                true
        );
        return 1;
    }

    private static int removeResearch(
            CommandContext<CommandSourceStack> ctx,
            ResourceLocation researchItemName,
            String playerName
    ) {
        ResearchServerEvents.removeResearch(playerName, researchItemName);
        ResearchServerEvents.syncResearch(playerName);
        ctx.getSource().sendSuccess(
                () -> Component.literal("Removed research '" + researchItemName + "' for " + playerName),
                true
        );
        return 1;
    }

    private static int setUnitSearchBehaviour(
            CommandContext<CommandSourceStack> ctx,
            String ownerName,
            BlockPos from,
            BlockPos to,
            String behaviourName
    ) {
        EnemySearchBehaviour behaviour = EnemySearchBehaviour.valueOf(behaviourName.trim().toUpperCase());
        BlockPos min = min(from, to);
        BlockPos max = max(from, to);

        int changed = 0;
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof AttackerUnit attacker
                    && entity instanceof Unit unit
                    && unit.getOwnerName().equals(ownerName)
                    && isWithin(entity.getOnPos(), min, max)) {
                attacker.setEnemySearchBehaviour(behaviour);
                changed++;
            }
        }
        if (changed == 0) {
            ctx.getSource().sendFailure(Component.literal("No attacker units owned by '" + ownerName + "' found in selection"));
        } else {
            int finalChanged = changed;
            ctx.getSource().sendSuccess(
                    () -> Component.literal("Set search behaviour to " + behaviour.name() + " for " + finalChanged + " unit(s)"),
                    true
            );
        }
        return changed;
    }

    private static int setRTSCamera(
            CommandContext<CommandSourceStack> ctx,
            String playerName,
            Boolean value
    ) {
        PlayerClientboundPacket.setRTSCamera(playerName, value);
        ctx.getSource().sendSuccess(
                () -> Component.literal("Set RTS camera '" + value + "' for " + playerName),
                true
        );
        return 1;
    }

    private static int[] collectUnitIds(BlockPos from, BlockPos to) {
        BlockPos min = min(from, to);
        BlockPos max = max(from, to);
        List<Integer> ids = new ArrayList<>();
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof Unit && isWithin(entity.getOnPos(), min, max)) {
                ids.add(entity.getId());
            }
        }

        var idArray = new int[ids.size()];
        for (int i = 0; i < idArray.length; i++) {
            idArray[i] = ids.get(i);
        }
        return idArray;
    }

    private static Building resolveBuilding(String input) {
        ResourceLocation location = null;
        if (input.contains(":")) {
            location = ResourceLocation.tryParse(input);
        } else {
            location = ResourceLocation.tryParse(ReignOfNether.MOD_ID + ":" + input);
        }
        Building building = location == null ? null : ReignOfNetherRegistries.BUILDING.get(location);
        if (building == null) {
            building = CustomBuildingServerEvents.getCustomBuilding(input);
        }
        return building;
    }

    private static ResourceName resolveResource(String name) throws CommandSyntaxException {
        try {
            return ResourceName.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw UNKNOWN_RESOURCE.create();
        }
    }

    private static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ())
        );
    }

    private static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()),
            Math.max(a.getZ(), b.getZ())
        );
    }

    private static boolean isWithin(BlockPos target, BlockPos min, BlockPos max) {
        return target.getX() >= min.getX() && target.getX() <= max.getX()
            && target.getY() >= min.getY() && target.getY() <= max.getY()
            && target.getZ() >= min.getZ() && target.getZ() <= max.getZ();
    }

    private static boolean intersects(BuildingPlacement placement, BlockPos min, BlockPos max) {
        BlockPos bMin = placement.minCorner;
        BlockPos bMax = placement.maxCorner;
        return !(bMax.getX() < min.getX() || bMin.getX() > max.getX()
            || bMax.getY() < min.getY() || bMin.getY() > max.getY()
            || bMax.getZ() < min.getZ() || bMin.getZ() > max.getZ());
    }

    private static String formatPos(BlockPos pos) {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }
}

