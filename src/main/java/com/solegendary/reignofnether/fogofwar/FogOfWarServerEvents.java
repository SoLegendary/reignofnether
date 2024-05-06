package com.solegendary.reignofnether.fogofwar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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

        for (int x = 0; x < 16; x++) {
            for (int y = 16; y > 0; y--) {
                for (int z = 0; z < 16; z++) {
                    BlockPos bp = renderChunkOrigin.offset(x,y,z);
                    BlockState bs = serverLevel.getBlockState(bp);
                    BlockState bsAbove = serverLevel.getBlockState(bp.above());

                    serverLevel.setBlockAndUpdate(bp, Blocks.BEDROCK.defaultBlockState());
                    serverLevel.setBlockAndUpdate(bp, bs);
                }
            }
        }
    }
}
