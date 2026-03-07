package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.ability.heroAbilities.wildfire.SoulsAflame;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.units.piglins.WildfireUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class UnextinguishableSoulFireBlock extends BaseFireBlock {
    public UnextinguishableSoulFireBlock(BlockBehaviour.Properties p_56653_) {
        super(p_56653_, 2.0F);
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        return this.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return pLevel.getBlockState(pPos.below()).isSolid();
    }

    @Override
    protected boolean canBurn(BlockState pState) {
        return true;
    }

    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        boolean inRangeOfWildfireUlt = false;
        for (LivingEntity unit : UnitServerEvents.getAllUnits()) {
            if (unit instanceof WildfireUnit wildfireUnit && wildfireUnit.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get()) &&
                pPos.distToCenterSqr(wildfireUnit.position()) <= SoulsAflame.RANGE * SoulsAflame.RANGE) {
                inRangeOfWildfireUlt = true;
                break;
            }
        }
        if (!inRangeOfWildfireUlt) {
            pLevel.setBlockAndUpdate(pPos, Blocks.FIRE.defaultBlockState());
        }
    }
}
