package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.isSolidBlocking;

public class GhastUnitFireball extends LargeFireball {

    public static final int SOULSAND_DURATION = 200;

    // 1.21.1's LargeFireball(Level, LivingEntity, Vec3, int) takes the shoot direction instead of a
    // spawn offset, and the fireball is always launched from the shooter's position.
    public GhastUnitFireball(Level pLevel, LivingEntity pShooter, Vec3 pDirection, int pExplosionPower) {
        super(pLevel, pShooter, pDirection, pExplosionPower);
    }

    // 1.21.1 passes the exploding Explosion in, so the projectile can tell what is damaging it
    @Override
    public boolean ignoreExplosion(net.minecraft.world.level.Explosion explosion) {
        return true;
    }

    protected void onHit(HitResult pResult) {
        super.onHit(pResult);

        if (level().isClientSide() || !(getOwner() instanceof GhastUnit))
            return;

        ArrayList<BlockPos> bps = new ArrayList<>();

        if (getOwner() instanceof GhastUnit ghastUnit)
            for (int x = -4; x < 4; x++)
                for (int y = -4; y < 4; y++)
                    for (int z = -4; z < 4; z++) {
                        BlockPos bp = blockPosition().offset(x, y, z);
                        if (bp.distSqr(blockPosition()) <= 9 &&
                            isSolidBlocking(level(), bp) &&
                            !isSolidBlocking(level(), bp.above()))
                            bps.add(bp.above());
                    }

        ArrayList<BlockPos> fireBps = new ArrayList<>();
        Collections.shuffle(bps);
        if (bps.size() >= 1)
            fireBps.add(bps.get(0));
        if (bps.size() >= 2)
            fireBps.add(bps.get(1));
        if (bps.size() >= 3)
            fireBps.add(bps.get(2));

        for (BlockPos bp : fireBps) {
            level().setBlockAndUpdate(bp, Blocks.FIRE.defaultBlockState());

            if (getOwner() instanceof GhastUnit ghastUnit && ResearchServerEvents.playerHasResearch(ghastUnit.getOwnerName(), ProductionItems.RESEARCH_SOUL_FIREBALLS)) {
                for (BlockPos bpAdj : List.of(bp, bp.north(), bp.south(), bp.east(), bp.west())) {
                    BlockState bsBelow = level().getBlockState(bpAdj.below());
                    if (!bsBelow.isAir())
                        BlockServerEvents.addTempBlock((ServerLevel) level(), bpAdj.below(), Blocks.SOUL_SAND.defaultBlockState(), bsBelow, SOULSAND_DURATION);
                }
                for (BlockPos bpAdj : List.of(
                        bp.north().north(),
                        bp.south().south(),
                        bp.east().east(),
                        bp.west().west(),
                        bp.north().west(),
                        bp.north().east(),
                        bp.south().west(),
                        bp.south().east()
                )) {
                    if (random.nextFloat() > 0.4f) {
                        BlockState bsBelow = level().getBlockState(bpAdj.below());
                        if (!bsBelow.isAir())
                            BlockServerEvents.addTempBlock((ServerLevel) level(), bpAdj.below(), Blocks.SOUL_SAND.defaultBlockState(), bsBelow, SOULSAND_DURATION);
                    }
                }
            }
        }
    }
}
