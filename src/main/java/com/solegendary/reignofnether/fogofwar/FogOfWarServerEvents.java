package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FogOfWarServerEvents {

    private static final ArrayList<Pair<String, Set<ChunkPos>>> exploredChunksData = new ArrayList<>();

    private static ServerLevel serverLevel = null;

    // keep track of the explored chunks of every player
    public static void saveExploredChunks(String playerName, int[] xPos, int[] zPos) {
        if (serverLevel == null || xPos.length != zPos.length)
            return;

        boolean foundPlayer = false;
        for (Pair<String, Set<ChunkPos>> data : exploredChunksData) {
            if (data.getFirst().equals(playerName)) {
                foundPlayer = true;
                break;
            }
        }
        if (!foundPlayer)
            exploredChunksData.add(new Pair<>(playerName, ConcurrentHashMap.newKeySet()));

        for (Pair<String, Set<ChunkPos>> data : exploredChunksData) {
            if (data.getFirst().equals(playerName)) {
                data.getSecond().clear();
                for (int i = 0; i < xPos.length; i++)
                    data.getSecond().add(serverLevel.getChunk(new BlockPos(xPos[i], 0, zPos[i])).getPos());
                System.out.println("saved " + data.getSecond().size() + " explored chunks for: " + playerName);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD)
            return;
        serverLevel = (ServerLevel) evt.level;
    }

    // whenever a player joins, send them what we have for saved explored chunks
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        for (Pair<String, Set<ChunkPos>> data : exploredChunksData)
            if (data.getFirst().equals(evt.getEntity().getName().getString()))
                FogOfWarClientboundPacket.loadExploredChunks(data.getFirst(), data.getSecond());
    }
}










