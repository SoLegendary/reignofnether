package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID)
public class GridInvalidationEvents {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent evt) {
        invalidate(evt.getLevel(), evt.getPos());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent evt) {
        invalidate(evt.getLevel(), evt.getPos());
    }

    private static void invalidate(LevelAccessor level, BlockPos bp) {
        if (level == null || bp == null) return;
        WalkabilityGrid.get(level).invalidateColumn(bp);
    }
}
