package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.resources.ResourceSources;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class BlockServerEvents {

    public static ArrayList<TemporaryBlock> tempBlocks = new ArrayList<>();

    public static void addTempBlock(ServerLevel level, BlockPos bp, BlockState bs, BlockState oldBs, int lifespan) {
        if (BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), bp))
            return;

        if (level.getBlockState(bp) != bs)
            tempBlocks.add(new TemporaryBlock(level, bp, bs, oldBs, lifespan));
        else {
            for (TemporaryBlock block : tempBlocks)
                if (bp.equals(block.bp))
                    block.tickAge = 0;
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD) {
            return;
        }
        tempBlocks.removeIf(tb -> tb.tick((ServerLevel) evt.level));
    }

    @SubscribeEvent
    public static void onPlayerBlockBreak(BlockEvent.BreakEvent evt) {
        if (evt.getLevel().isClientSide() || evt.getLevel().getServer() == null)
            return;

        boolean isResource = ResourceSources.getFromBlockState(evt.getState()) != null;
        boolean isBuilding = BuildingUtils.isPosInsideAnyBuilding(false, evt.getPos());

        if (!evt.getLevel().getServer().getGameRules().getRule(GameRuleRegistrar.DO_PLAYER_GRIEFING).get() &&
            !isResource && !isBuilding) {
            evt.setCanceled(true);
        }
    }
}
