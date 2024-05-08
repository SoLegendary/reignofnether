package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;

import static com.solegendary.reignofnether.player.PlayerServerEvents.sendMessageToAllPlayers;

public class FogOfWarServerEvents {

    private static boolean enabled = false; // enforced for all clients
    private static ServerLevel serverLevel = null;

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        syncClientFog();
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        sendMessageToAllPlayers((enabled ? "Enabled" : "Disabled") + " fog of war for all players");
        syncClientFog();
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD)
            return;

        serverLevel = (ServerLevel) evt.level;
    }

    // sets the fog to match what all
    private static void syncClientFog() {
        FogOfWarClientboundPacket.setEnabled(enabled);
    }

    // updates all blocks in the renderchunk to force all clients to match the server
    public static void syncClientBlocks(BlockPos renderChunkOrigin) {
        if (serverLevel == null)
            return;

        ArrayList<Pair<BlockPos, BlockState>> plants = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos bp = renderChunkOrigin.offset(x,y,z);
                    BlockState bs = serverLevel.getBlockState(bp);
                    if (bs.getMaterial() == Material.PLANT ||
                        bs.getMaterial() == Material.REPLACEABLE_PLANT) {
                        plants.add(new Pair<>(bp, bs));
                    }
                }
            }
        }
        for (Pair<BlockPos, BlockState> plant : plants)
            serverLevel.setBlockAndUpdate(plant.getFirst(), Blocks.AIR.defaultBlockState());

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos bp = renderChunkOrigin.offset(x,y,z);
                    BlockState bs = serverLevel.getBlockState(bp);
                    serverLevel.setBlockAndUpdate(bp, Blocks.BEDROCK.defaultBlockState());
                    serverLevel.setBlockAndUpdate(bp, bs);
                }
            }
        }
        plants.sort(Comparator.comparing(p -> ((Pair<BlockPos, BlockState>) p).getFirst().getY()).reversed());
        for (Pair<BlockPos, BlockState> plant : plants)
            serverLevel.setBlockAndUpdate(plant.getFirst(), plant.getSecond());

    }
}
