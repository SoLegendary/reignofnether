package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.GruntUnit;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WalkableMagmaBlock extends Block {
    public static final int DAMAGE_DELAY = 20; // higher == damage less often
    public static final int DAMAGE = 3;

    public WalkableMagmaBlock(BlockBehaviour.Properties pProperties) {
        super(pProperties);
    }

    public void stepOn(Level pLevel, BlockPos pPos, BlockState pState, Entity pEntity) {
        boolean isPiglinFaction = pEntity instanceof Unit unit && unit.getFaction() == Faction.PIGLINS && !pEntity.isOnFire();
        boolean isDamageTick = pEntity.tickCount % DAMAGE_DELAY == 0;

        if (!pEntity.isSteppingCarefully() &&
                pEntity instanceof LivingEntity &&
                !(pEntity instanceof GruntUnit) &&
                !EnchantmentHelper.hasFrostWalker((LivingEntity)pEntity) &&
                !isPiglinFaction && isDamageTick) {
            pEntity.hurt(pEntity.damageSources().hotFloor(), DAMAGE);
        }
    }

    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        BubbleColumnBlock.updateColumn(pLevel, pPos.above(), pState);
    }

    public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (pFacing == Direction.UP && pFacingState.is(Blocks.WATER)) {
            pLevel.scheduleTick(pCurrentPos, this, 20);
        }

        return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        BlockPos blockpos = pPos.above();
        if (pLevel.getFluidState(pPos).canExtinguish(pLevel, pPos)) {
            pLevel.playSound((Player)null, pPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.8F);
            pLevel.sendParticles(ParticleTypes.LARGE_SMOKE, (double)blockpos.getX() + 0.5, (double)blockpos.getY() + 0.25, (double)blockpos.getZ() + 0.5, 8, 0.5, 0.25, 0.5, 0.0);
        }
    }

    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        pLevel.scheduleTick(pPos, this, 20);
    }
}
