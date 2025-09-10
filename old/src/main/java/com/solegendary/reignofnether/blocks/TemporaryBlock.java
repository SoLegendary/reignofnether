package com.solegendary.reignofnether.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

public class TemporaryBlock {

    public static final Random RANDOM = new Random();

    public final int lifespan;
    public final BlockPos bp;
    public final BlockState bs;
    public final BlockState oldBs;
    public int tickAge;
    public boolean isPlaced = false;

    public TemporaryBlock(ServerLevel level, BlockPos bp, BlockState bs, BlockState oldBs, int lifespan) {
        this.bp = bp;
        this.bs = bs;
        this.oldBs = oldBs;
        this.lifespan = lifespan + RANDOM.nextInt(-10,10);
        this.tickAge = RANDOM.nextInt(-5,-1);
    }

    // returns true if the block lifespan is done
    public boolean tick(ServerLevel level) {
        if (level.isClientSide())
            return false;

        tickAge += 1;

        if (!isPlaced && tickAge > 0) {
            if (oldBs.isAir()) {
                level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, bp, Block.getId(bs));
                level.levelEvent(bs.getSoundType().getPlaceSound().hashCode(), bp, Block.getId(bs));
            }
            level.setBlockAndUpdate(bp, bs);
            isPlaced = true;
        }
        else if (isPlaced && tickAge >= lifespan &&
                level.getBlockState(bp).getBlock() == bs.getBlock()) {
            if (oldBs.isAir()) {
                level.destroyBlock(bp, false);
            } else {
                level.setBlockAndUpdate(bp, oldBs);
            }
            return true;
        }
        return false;
    }
}
