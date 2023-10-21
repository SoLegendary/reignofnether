package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ResourcesServerEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    public static final int STARTING_FOOD = 100;
    public static final int STARTING_WOOD = 400;
    public static final int STARTING_ORE = 250;

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

        Resources playerResources = null;
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(playerName))
                playerResources = resources;

        if (playerResources == null) {
            playerResources = new Resources(playerName,
                    STARTING_FOOD,
                    STARTING_WOOD,
                    STARTING_ORE);
            resourcesList.add(playerResources);
        }
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
        // prevent natural growth, use our algorithm instead
        else if (block instanceof CropBlock || block instanceof StemBlock) {
            int newAge = blockState.getValue(BlockStateProperties.AGE_7) + (random.nextFloat() > 0.6f ? 1 : 2);
            if (newAge > 7)
                newAge = 7;
            BlockState grownState = block.defaultBlockState().setValue(BlockStateProperties.AGE_7, newAge);
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
                        Blocks.CRIMSON_STEM, Blocks.WARPED_STEM)
                .contains(bs.getBlock());
    }
    public static boolean isLeafBlock(BlockState bs) {
        return List.of(Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.SPRUCE_LEAVES,
                        BlockRegistrar.DECAYABLE_NETHER_WART_BLOCK.get(), BlockRegistrar.DECAYABLE_WARPED_WART_BLOCK.get())
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


















