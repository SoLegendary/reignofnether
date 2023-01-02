package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
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
}
