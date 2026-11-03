package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class TemporaryWalkableMagmaBlock extends WalkableMagmaBlock {

    public static final int MELT_TICKS = 200; // 10 seconds
    public static final int STEP_TICKS = 20;  // must match the delay used in WalkableMagmaBlock.onPlace
    public static final int STEPS = MELT_TICKS / STEP_TICKS;

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, STEPS - 1);

    public TemporaryWalkableMagmaBlock(BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AGE);
    }

    @Override
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        super.tick(pState, pLevel, pPos, pRandom); // keep the bubble column behaviour

        int age = pState.getValue(AGE);

        if (inRangeOfMagmaWalker(pPos)) {
            // refresh the block instead of letting it decay
            if (age != 0)
                pLevel.setBlock(pPos, pState.setValue(AGE, 0), Block.UPDATE_CLIENTS);
            pLevel.scheduleTick(pPos, this, STEP_TICKS);
            return;
        }

        if (age < STEPS - 1) {
            pLevel.setBlock(pPos, pState.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            pLevel.scheduleTick(pPos, this, STEP_TICKS);
        } else {
            melt(pLevel, pPos);
        }
    }

    private void melt(ServerLevel pLevel, BlockPos pPos) {
        pLevel.setBlockAndUpdate(pPos, Blocks.LAVA.defaultBlockState());
        pLevel.playSound(null, pPos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.5F, 1.0F);
        pLevel.sendParticles(ParticleTypes.LAVA,
                pPos.getX() + 0.5, pPos.getY() + 1.0, pPos.getZ() + 0.5,
                4, 0.3, 0.1, 0.3, 0.0);
    }

    private boolean inRangeOfMagmaWalker(BlockPos pPos) {
        for (LivingEntity le : UnitServerEvents.getAllUnits())
            if (le instanceof UnitInventory inv
                    && inv.isHolding(UnitItems.MAGMA_WALKER_BOOTS)
                    && le.blockPosition().closerToCenterThan(pPos.getCenter(), 5))
                return true;
        return false;
    }
}