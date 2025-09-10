package com.solegendary.reignofnether.resources;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

import static com.solegendary.reignofnether.resources.BlockUtils.isLogBlock;

public class ResourcesServerEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    public static final int STARTING_FOOD_TUTORIAL = 750;
    public static final int STARTING_WOOD_TUTORIAL = 850;
    public static final int STARTING_ORE_TUTORIAL = 250;
    public static final int STARTING_FOOD_SANDBOX = 999999;
    public static final int STARTING_WOOD_SANDBOX = 999999;
    public static final int STARTING_ORE_SANDBOX = 999999;
    public static final int STARTING_FOOD = 100;
    public static final int STARTING_WOOD = 450;
    public static final int STARTING_ORE = 250;

    public static final float NEUTRAL_UNIT_BOUNTY_PERCENT = 0.25f;
    public static final float NEUTRAL_BUILDING_BOUNTY_PERCENT = 0.25f;

    // to avoid having to save units too often add on all unit resources here too and just add directly on load
    public static void saveResources(ServerLevel serverLevel) {
        if (serverLevel == null) {
            return;
        }
        ResourcesSaveData data = ResourcesSaveData.getInstance(serverLevel);
        data.resources.clear();
        resourcesList.forEach(r -> {

            // add all unit held resources to resources so we don't have to save unit items
            int unitFood = 0;
            int unitWood = 0;
            int unitOre = 0;
            for (LivingEntity le : UnitServerEvents.getAllUnits()) {
                if (le instanceof Unit u && u.getOwnerName().equals(r.ownerName)) {
                    Resources unitRes = Resources.getTotalResourcesFromItems(u.getItems());
                    unitFood += unitRes.food;
                    unitWood += unitRes.wood;
                    unitOre += unitRes.ore;
                }
            }
            // add all production item costs since they will be cancelled on server shutdown
            int prodFood = 0;
            int prodWood = 0;
            int prodOre = 0;
            for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
                if (building instanceof ProductionPlacement pBuilding) {
                    for (ActiveProduction item : pBuilding.productionQueue) {
                        prodFood += item.item.getCost(false, pBuilding.ownerName).food;
                        prodWood += item.item.getCost(false, pBuilding.ownerName).wood;
                        prodOre += item.item.getCost(false, pBuilding.ownerName).ore;
                    }
                }
            }
            data.resources.add(new Resources(r.ownerName,
                r.food + r.foodToAdd + unitFood + prodFood,
                r.wood + r.woodToAdd + unitWood + prodWood,
                r.ore + r.oreToAdd + unitOre + prodOre
            ));
            //ReignOfNether.LOGGER.info("saved resources in serverevents: " + r.ownerName + "|" + r.food + "|" + r.wood + "|" + r.ore);
        });
        data.save();
        serverLevel.getDataStorage().save();
    }

    @SubscribeEvent
    public static void loadResources(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        if (level != null) {
            ResourcesSaveData data = ResourcesSaveData.getInstance(level);
            resourcesList.clear();
            resourcesList.addAll(data.resources);

            //ReignOfNether.LOGGER.info("saved " + data.resources.size() + " resources in serverevents");
        }
    }

    private static final int SAVE_TICKS_MAX = 600;
    private static int saveTicks = 0;
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        saveTicks += 1;
        if (saveTicks >= SAVE_TICKS_MAX) {
            ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
            if (level != null) {
                saveResources(level);
                saveTicks = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
        if (level != null) {
            saveResources(level);
        }
    }

    public static void resetResources(String playerName) {
        for (Resources resources : resourcesList) {
            if (resources.ownerName.equals(playerName)) {
                if (TutorialServerEvents.isEnabled()) {
                    resources.food = STARTING_FOOD_TUTORIAL;
                    resources.wood = STARTING_WOOD_TUTORIAL;
                    resources.ore = STARTING_ORE_TUTORIAL;
                } else if (SandboxServer.isSandboxPlayer(playerName)) {
                    resources.food = STARTING_FOOD_SANDBOX;
                    resources.wood = STARTING_WOOD_SANDBOX;
                    resources.ore = STARTING_ORE_SANDBOX;
                } else {
                    resources.food = STARTING_FOOD;
                    resources.wood = STARTING_WOOD;
                    resources.ore = STARTING_ORE;
                }
                ResourcesClientboundPacket.syncResources(resourcesList);
                break;
            }
        }
    }

    public static void addSubtractResources(Resources resourcesToAdd) {
        for (Resources resources : resourcesList) {
            if (resources.ownerName.equals(resourcesToAdd.ownerName)) {
                // change serverside instantly
                resources.changeInstantly(resourcesToAdd.food, resourcesToAdd.wood, resourcesToAdd.ore);
                // change clientside over time
                ResourcesClientboundPacket.addSubtractResources(new Resources(resourcesToAdd.ownerName,
                    resourcesToAdd.food,
                    resourcesToAdd.wood,
                    resourcesToAdd.ore
                ));
            }
        }
    }

    public static boolean canAfford(String ownerName, ResourceName resourceName, int cost) {
        if (cost <= 0) {
            return true;
        }
        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName)) {
                switch (resourceName) {
                    case FOOD -> {
                        return resources.food >= cost;
                    }
                    case WOOD -> {
                        return resources.wood >= cost;
                    }
                    case ORE -> {
                        return resources.ore >= cost;
                    }
                }
            }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        String playerName = evt.getEntity().getName().getString();
        ResourcesClientboundPacket.syncResources(resourcesList);
    }

    public static void assignResources(String playerName) {
        resourcesList.removeIf(r -> r.ownerName.equals(playerName));
        Resources resources;
        if (TutorialServerEvents.isEnabled()) {
            resources = new Resources(playerName,
                STARTING_FOOD_TUTORIAL,
                STARTING_WOOD_TUTORIAL,
                STARTING_ORE_TUTORIAL
            );
        } else {
            resources = new Resources(playerName, STARTING_FOOD, STARTING_WOOD, STARTING_ORE);
        }
        resourcesList.add(resources);
        ResourcesClientboundPacket.syncResources(resourcesList);
    }

    private static final Random random = new Random();

    // prevent vanilla growth mechanics because they're slow and random, see AbstractFarm instead
    @SubscribeEvent
    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre evt) {
        if (BuildingUtils.isPosInsideAnyBuilding(evt.getLevel().isClientSide(), evt.getPos()))
            evt.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public static void onPlayerBlockBreak(BlockEvent.BreakEvent evt) {
        if (BuildingUtils.isPosInsideAnyBuilding(false, evt.getPos())) {
            evt.setCanceled(true);
            if (evt.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.setBlockAndUpdate(evt.getPos(), Blocks.AIR.defaultBlockState());
            }
        }

        if (isLogBlock(evt.getState()) && !BuildingUtils.isPosInsideAnyBuilding(false, evt.getPos())) {
            fellAdjacentLogs(evt.getPos(), new ArrayList<>(), (Level) evt.getLevel());
        }
    }

    // if a tree is touched, destroy any adjacent logs that are above the ground after some time to avoid leaving
    // tall trees behind
    public static void fellAdjacentLogs(BlockPos bp, ArrayList<BlockPos> bpsExcluded, Level level) {

        if (!level.getGameRules().getRule(GameRuleRegistrar.LOG_FALLING).get())
            return;

        BlockState bs = level.getBlockState(bp);

        List<BlockPos> bpsAdj = List.of(bp.north(),
            bp.south(),
            bp.east(),
            bp.west(),
            bp.above(),
            bp.above().north(),
            bp.above().south(),
            bp.above().east(),
            bp.above().west(),
            bp.north().east(),
            bp.north().west(),
            bp.south().east(),
            bp.south().west(),
            bp.above().north().east(),
            bp.above().north().west(),
            bp.above().south().east(),
            bp.above().south().west()
        );

        for (BlockPos bpAdj : bpsAdj) {
            BlockState bsAdj = level.getBlockState(bpAdj);
            if (isLogBlock(bsAdj) && !bpsExcluded.contains(bpAdj)) {
                Block fallingLogBlock = FALLING_LOGS.get(bsAdj.getBlock());
                if (fallingLogBlock != null && !BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), bpAdj)) {
                    if (bsAdj.hasProperty(BlockStateProperties.AXIS)) {
                        level.setBlockAndUpdate(bpAdj, fallingLogBlock.defaultBlockState()
                                .setValue(BlockStateProperties.AXIS, bsAdj.getValue(BlockStateProperties.AXIS)));
                    } else {
                        level.setBlockAndUpdate(bpAdj, fallingLogBlock.defaultBlockState());
                    }
                    bpsExcluded.add(bpAdj);
                    fellAdjacentLogs(bpAdj, bpsExcluded, level);
                }
                bpsExcluded.add(bpAdj);
                fellAdjacentLogs(bpAdj, bpsExcluded, level);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent evt) {

        evt.getDispatcher().register(Commands.literal("sendfood")
            .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
            .executes((command) -> trySendingResources(command, ResourceName.FOOD)))));

        evt.getDispatcher().register(Commands.literal("sendwood")
            .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
            .executes((command) -> trySendingResources(command, ResourceName.WOOD)))));

        evt.getDispatcher().register(Commands.literal("sendore")
            .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
            .executes((command) -> trySendingResources(command, ResourceName.ORE)))));
    }

    public static int trySendingResources(CommandContext<CommandSourceStack> context, ResourceName resourceName) throws CommandSyntaxException {
        Player sendingPlayer = context.getSource().getPlayer();
        Player receivingPlayer = EntityArgument.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        if (sendingPlayer == null)
            return 0;
        String sendingPlayerName = sendingPlayer.getName().getString();
        String receivingPlayerName = receivingPlayer.getName().getString();

        Resources res = null;
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(sendingPlayer.getName().getString()))
                res = resources;
        if (res == null)
            return 0;

        if (sendingPlayerName.equals(receivingPlayerName)) {
            PlayerServerEvents.sendMessageToPlayer(sendingPlayerName, "server.resources.reignofnether.sending_to_self");
            return 0;
        } else if (!PlayerServerEvents.isRTSPlayer(receivingPlayerName)) {
            PlayerServerEvents.sendMessageToPlayer(sendingPlayerName, "server.resources.reignofnether.not_rts_player");
            return 0;
        } else if (!canAfford(sendingPlayerName, resourceName, amount)) {
            ResourcesClientboundPacket.warnInsufficientResources(sendingPlayerName,
                    resourceName == ResourceName.FOOD,
                    resourceName == ResourceName.WOOD,
                    resourceName == ResourceName.ORE
            );
            return 0;
        } else {
            addSubtractResources(new Resources(sendingPlayerName,
                    resourceName == ResourceName.FOOD ? -amount : 0,
                    resourceName == ResourceName.WOOD ? -amount : 0,
                    resourceName == ResourceName.ORE ? -amount : 0
            ));
            addSubtractResources(new Resources(receivingPlayerName,
                    resourceName == ResourceName.FOOD ? amount : 0,
                    resourceName == ResourceName.WOOD ? amount : 0,
                    resourceName == ResourceName.ORE ? amount : 0
            ));
            switch (resourceName) {
                case FOOD -> {
                    PlayerServerEvents.sendMessageToPlayer(sendingPlayerName, "server.resources.reignofnether.sent_food", false, amount, receivingPlayerName);
                    PlayerServerEvents.sendMessageToPlayer(receivingPlayerName, "server.resources.reignofnether.received_food", false, amount, sendingPlayerName);
                }
                case WOOD -> {
                    PlayerServerEvents.sendMessageToPlayer(sendingPlayerName, "server.resources.reignofnether.sent_wood", false, amount, receivingPlayerName);
                    PlayerServerEvents.sendMessageToPlayer(receivingPlayerName, "server.resources.reignofnether.received_wood", false, amount, sendingPlayerName);
                }
                case ORE -> {
                    PlayerServerEvents.sendMessageToPlayer(sendingPlayerName, "server.resources.reignofnether.sent_ore", false, amount, receivingPlayerName);
                    PlayerServerEvents.sendMessageToPlayer(receivingPlayerName, "server.resources.reignofnether.received_ore", false, amount, sendingPlayerName);
                }
            }
            return 1;
        }
    }

    private static final Map<Block, Block> FALLING_LOGS = new HashMap<>();
    static {
        FALLING_LOGS.put(Blocks.OAK_LOG, BlockRegistrar.FALLING_OAK_LOG.get());
        FALLING_LOGS.put(Blocks.SPRUCE_LOG, BlockRegistrar.FALLING_SPRUCE_LOG.get());
        FALLING_LOGS.put(Blocks.BIRCH_LOG, BlockRegistrar.FALLING_BIRCH_LOG.get());
        FALLING_LOGS.put(Blocks.JUNGLE_LOG, BlockRegistrar.FALLING_JUNGLE_LOG.get());
        FALLING_LOGS.put(Blocks.ACACIA_LOG, BlockRegistrar.FALLING_ACACIA_LOG.get());
        FALLING_LOGS.put(Blocks.DARK_OAK_LOG, BlockRegistrar.FALLING_DARK_OAK_LOG.get());
        FALLING_LOGS.put(Blocks.MANGROVE_LOG, BlockRegistrar.FALLING_MANGROVE_LOG.get());
        FALLING_LOGS.put(Blocks.OAK_WOOD, BlockRegistrar.FALLING_OAK_LOG.get());
        FALLING_LOGS.put(Blocks.SPRUCE_WOOD, BlockRegistrar.FALLING_SPRUCE_LOG.get());
        FALLING_LOGS.put(Blocks.BIRCH_WOOD, BlockRegistrar.FALLING_BIRCH_LOG.get());
        FALLING_LOGS.put(Blocks.JUNGLE_WOOD, BlockRegistrar.FALLING_JUNGLE_LOG.get());
        FALLING_LOGS.put(Blocks.ACACIA_WOOD, BlockRegistrar.FALLING_ACACIA_LOG.get());
        FALLING_LOGS.put(Blocks.DARK_OAK_WOOD, BlockRegistrar.FALLING_DARK_OAK_LOG.get());
        FALLING_LOGS.put(Blocks.MANGROVE_WOOD, BlockRegistrar.FALLING_MANGROVE_LOG.get());
        FALLING_LOGS.put(Blocks.WARPED_STEM, BlockRegistrar.FALLING_WARPED_STEM.get());
        FALLING_LOGS.put(Blocks.WARPED_HYPHAE, BlockRegistrar.FALLING_WARPED_STEM.get());
        FALLING_LOGS.put(Blocks.CRIMSON_STEM, BlockRegistrar.FALLING_CRIMSON_STEM.get());
        FALLING_LOGS.put(Blocks.CRIMSON_HYPHAE, BlockRegistrar.FALLING_CRIMSON_STEM.get());
    }
}


















