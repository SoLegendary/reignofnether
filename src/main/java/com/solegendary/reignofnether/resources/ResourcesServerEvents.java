package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ResourcesServerEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    public static final int STARTING_FOOD_TUTORIAL = 750;
    public static final int STARTING_WOOD_TUTORIAL = 750;
    public static final int STARTING_ORE_TUTORIAL = 150;
    public static final int STARTING_FOOD = 100;
    public static final int STARTING_WOOD = 400;
    public static final int STARTING_ORE = 150;

    // to avoid having to save units too often add on all unit resources here too and just add directly on load
    public static void saveResources(ServerLevel serverLevel) {
        if (serverLevel == null)
            return;

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
            for (Building building : BuildingServerEvents.getBuildings()) {
                if (building instanceof ProductionBuilding pBuilding) {
                    for (ProductionItem item : pBuilding.productionQueue) {
                        prodFood += item.foodCost;
                        prodWood += item.woodCost;
                        prodOre += item.oreCost;
                    }
                }
            }
            data.resources.add(new Resources(
                    r.ownerName,
                    r.food + r.foodToAdd + unitFood + prodFood,
                    r.wood + r.woodToAdd + unitWood + prodWood,
                    r.ore + r.oreToAdd + unitOre + prodOre
            ));
            System.out.println("saved resources in serverevents: " + r.ownerName + "|" + r.food + "|" + r.wood + "|" + r.ore);
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

            System.out.println("saved " + data.resources.size() + " resources in serverevents");
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
        if (level != null)
            saveResources(level);
    }

    public static void resetResources(String playerName) {
        for (Resources resources : resourcesList) {
            if (resources.ownerName.equals(playerName)) {
                if (TutorialServerEvents.isEnabled()) {
                    resources.food = STARTING_FOOD_TUTORIAL;
                    resources.wood = STARTING_WOOD_TUTORIAL;
                    resources.ore = STARTING_ORE_TUTORIAL;
                }
                else {
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
                resources.changeInstantly(
                    resourcesToAdd.food,
                    resourcesToAdd.wood,
                    resourcesToAdd.ore
                );
                // change clientside over time
                ResourcesClientboundPacket.addSubtractResources(new Resources(
                    resourcesToAdd.ownerName,
                    resourcesToAdd.food,
                    resourcesToAdd.wood,
                    resourcesToAdd.ore
                ));
            }
        }
    }

    public static boolean canAfford(String ownerName, ResourceName resourceName, int cost) {
        if (cost <= 0)
            return true;

        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName)) {
                switch(resourceName) {
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
        for (Resources res : resourcesList) {
            if (res.ownerName.equals(playerName)) {
                if (TutorialServerEvents.isEnabled()) {
                    res.food = STARTING_FOOD_TUTORIAL;
                    res.wood = STARTING_WOOD_TUTORIAL;
                    res.ore = STARTING_ORE_TUTORIAL;
                } else {
                    res.food = STARTING_FOOD;
                    res.wood = STARTING_WOOD;
                    res.ore = STARTING_ORE;
                }
            }
        }
        Resources resources;
        if (TutorialServerEvents.isEnabled()) {
            resources = new Resources(playerName,
                    STARTING_FOOD_TUTORIAL,
                    STARTING_WOOD_TUTORIAL,
                    STARTING_ORE_TUTORIAL);
        } else {
            resources = new Resources(playerName,
                    STARTING_FOOD,
                    STARTING_WOOD,
                    STARTING_ORE);
        }
        resourcesList.add(resources);
        ResourcesClientboundPacket.syncResources(resourcesList);
    }

    private static final Random random = new Random();

    // speed up crop growth without having to increase gamerule randomTickSpeed (as that causes more lag)
    @SubscribeEvent
    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre evt) {
        BlockState blockState = evt.getLevel().getBlockState(evt.getPos());
        Block block = blockState.getBlock();
        if (block instanceof BeetrootBlock) {
            evt.setResult(Event.Result.ALLOW);
        }
        // always allow growth of gourd blocks
        else if (block instanceof StemBlock && blockState.getValue(BlockStateProperties.AGE_7) == 7) {
            evt.setResult(Event.Result.ALLOW);
        }
        // prevent natural growth, use our algorithm instead to randomly speed up growth
        else if (block instanceof CropBlock || block instanceof StemBlock) {
            int newAge = blockState.getValue(BlockStateProperties.AGE_7) + (random.nextFloat() > 0.6f ? 1 : 2);
            if (newAge > 7)
                newAge = 7;
            BlockState grownState = block.defaultBlockState().setValue(BlockStateProperties.AGE_7, newAge);
            evt.getLevel().setBlock(evt.getPos(), grownState, 2);
            evt.setResult(Event.Result.DENY);
        }
        else if (block instanceof NetherWartBlock) {
            int newAge = blockState.getValue(BlockStateProperties.AGE_3) + (random.nextFloat() > 0.42f ? 0 : 1);
            if (newAge > 3)
                newAge = 3;
            BlockState grownState = block.defaultBlockState().setValue(BlockStateProperties.AGE_3, newAge);
            evt.getLevel().setBlock(evt.getPos(), grownState, 2);
            evt.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onPlayerBlockBreak(BlockEvent.BreakEvent evt) {
        if (isLogBlock(evt.getState()) && !BuildingUtils.isPosInsideAnyBuilding(false, evt.getPos()))
            breakAdjacentLogs(evt.getPos(), new ArrayList<>(), (Level) evt.getLevel());
    }

    // if a tree is touched, destroy any adjacent logs that are above the ground after some time to avoid leaving tall trees behind
    public static void breakAdjacentLogs(BlockPos bp, ArrayList<BlockPos> bpsExcluded, Level level) {
        BlockState bs = level.getBlockState(bp);

        List<BlockPos> bpsAdj = List.of(
                bp.north(), bp.south(), bp.east(), bp.west(), bp.above(),
                bp.above().north(), bp.above().south(), bp.above().east(), bp.above().west(),
                bp.north().east(), bp.north().west(), bp.south().east(), bp.south().west(),
                bp.above().north().east(), bp.above().north().west(), bp.above().south().east(), bp.above().south().west());

        for (BlockPos bpAdj : bpsAdj) {
            BlockState bsAdj = level.getBlockState(bpAdj);
            if (isLogBlock(bsAdj) && !bpsExcluded.contains(bpAdj)) {
                if (numAirOrLeafBlocksBelow(bpAdj, level) >= 5)
                    level.destroyBlock(bpAdj, true);
                bpsExcluded.add(bpAdj);
                breakAdjacentLogs(bpAdj, bpsExcluded, level);
            }
        }
    }

    public static boolean isLogBlock(BlockState bs) {
        return List.of(Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.JUNGLE_LOG, Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG,
                        Blocks.OAK_WOOD, Blocks.BIRCH_WOOD, Blocks.ACACIA_WOOD, Blocks.DARK_OAK_WOOD, Blocks.JUNGLE_WOOD, Blocks.MANGROVE_WOOD, Blocks.SPRUCE_WOOD,
                        Blocks.CRIMSON_STEM, Blocks.WARPED_STEM, Blocks.MUSHROOM_STEM, Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE)
                .contains(bs.getBlock());
    }
    public static boolean isLeafBlock(BlockState bs) {
        if (bs.getMaterial() == Material.LEAVES)
            return true;
        return List.of(Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.SPRUCE_LEAVES,
                        BlockRegistrar.DECAYABLE_NETHER_WART_BLOCK.get(), BlockRegistrar.DECAYABLE_WARPED_WART_BLOCK.get(),
                        Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK)
                .contains(bs.getBlock());
    }

    public static int numAirOrLeafBlocksBelow(BlockPos bp, Level level) {
        int blocks = 0;
        for (int i = -1; i > -10; i--) {
            BlockState bs = level.getBlockState(bp.offset(0,i,0));
            if (bs.isAir() || isLeafBlock(bs))
                blocks += 1;
            else if (!isLogBlock(bs)) // stop counting if we hit a non-log solid block to avoid counting underground blocks
                break;
        }
        return blocks;
    }
}


















