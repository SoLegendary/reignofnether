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
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
            .then(Commands.argument("ownerName", StringArgumentType.string())
                .then(unitSelectionTail(ctx -> StringArgumentType.getString(ctx, "ownerName")))
            )
            .then(Commands.argument("ownerSelector", EntityArgument.player())
                .then(unitSelectionTail(ctx -> getPlayerName(EntityArgument.getPlayer(ctx, "ownerSelector"))))
            )
        );

        dispatcher.register(Commands.literal("rtsapi-set-building-owner")
            .requires(source -> source.hasPermission(2))
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
    }

    private static ArgumentBuilder<CommandSourceStack, ?> placeBuildingTail(NameResolver ownerResolver) {
        return Commands.argument("autoBuild", BoolArgumentType.bool())
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> placeBuilding(
                    ctx,
                    StringArgumentType.getString(ctx, "buildingName"),
                    ownerResolver.resolve(ctx),
                    BoolArgumentType.getBool(ctx, "autoBuild"),
                    BlockPosArgument.getLoadedBlockPos(ctx, "pos")
                ))
            );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> unitSelectionTail(NameResolver ownerResolver) {
        return Commands.argument("from", BlockPosArgument.blockPos())
            .then(Commands.argument("to", BlockPosArgument.blockPos())
                .executes(ctx -> setUnitOwner(
                    ctx,
                    ownerResolver.resolve(ctx),
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
        BlockPos pos
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
            Rotation.NONE,
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
        String ownerName,
        BlockPos from,
        BlockPos to
    ) {
        BlockPos min = min(from, to);
        BlockPos max = max(from, to);

        List<Integer> ids = new ArrayList<>();
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof Unit unit && isWithin(entity.getOnPos(), min, max)) {
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

