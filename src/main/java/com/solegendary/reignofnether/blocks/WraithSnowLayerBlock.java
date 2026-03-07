package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.registrars.BlockEntityRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.WretchedWraithUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class WraithSnowLayerBlock extends BaseEntityBlock {

    private static final int MOVEMENT_SLOWDOWN_AMP_PER_LAYER = 2;
    private static final int DMG_TAKEN_INCREASE_AMP_PER_LAYER = 2;
    private static final int ATTACK_SLOWDOWN_AMP_PER_LAYER = 2;

    public WraithSnowLayerBlock(BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(
                this.defaultBlockState()
                        .setValue(LAYERS, 1)
        );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WraithSnowBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                type,
                BlockEntityRegistrar.WRAITH_SNOW_BLOCK_ENTITY.get(),
                WraithSnowBlockEntity::tick
        );
    }

    @Override
    public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
        return true;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE_BY_LAYER[0];
    }

    @Override
    public void entityInside(@NotNull BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        BlockEntity be = pLevel.getBlockEntity(pPos);
        int movementSlowdownAmp = (pState.getValue(LAYERS) * MOVEMENT_SLOWDOWN_AMP_PER_LAYER) - 1;
        int dmgIncreaseAmp = (pState.getValue(LAYERS) * DMG_TAKEN_INCREASE_AMP_PER_LAYER) - 1;
        int attackSlowdownAmp = (pState.getValue(LAYERS) * ATTACK_SLOWDOWN_AMP_PER_LAYER) - 1;
        if (pEntity instanceof LivingEntity livingEntity && pEntity.tickCount % 5 == 0 &&
                !(pEntity instanceof WretchedWraithUnit) && !pLevel.isClientSide() &&
                be instanceof WraithSnowBlockEntity snowBe) {

            Relationship rs = Relationship.NEUTRAL;
            if (pEntity instanceof Unit unit) {
                rs = UnitServerEvents.getUnitToEntityRelationship(unit, pLevel, snowBe.getOwnerId());
            }
            if (rs != Relationship.FRIENDLY && rs != Relationship.OWNED) {
                MobEffectInstance existingMovementSlowdown = livingEntity.getEffect(MobEffectRegistrar.MINOR_MOVEMENT_SLOWDOWN.get());
                if (existingMovementSlowdown == null || existingMovementSlowdown.getAmplifier() < movementSlowdownAmp) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffectRegistrar.MINOR_MOVEMENT_SLOWDOWN.get(), 10, movementSlowdownAmp, true, false));
                }
                MobEffectInstance existingDamageIncrease = livingEntity.getEffect(MobEffectRegistrar.DAMAGE_TAKEN_INCREASE.get());
                if (existingDamageIncrease == null || existingDamageIncrease.getAmplifier() < dmgIncreaseAmp) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffectRegistrar.DAMAGE_TAKEN_INCREASE.get(), 10, dmgIncreaseAmp, true, false));
                }
                MobEffectInstance existingAttackSlowdown = livingEntity.getEffect(MobEffectRegistrar.ATTACK_SLOWDOWN.get());
                if (existingAttackSlowdown == null || existingAttackSlowdown.getAmplifier() < attackSlowdownAmp) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffectRegistrar.ATTACK_SLOWDOWN.get(), 10, attackSlowdownAmp, true, false));
                }
            }
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int layers = state.getValue(LAYERS);
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + ((layers + 1) * 0.125D);
        double z = pos.getZ() + random.nextDouble();

        level.sendParticles(
                ParticleTypes.SOUL,
                x, y, z,
                1,
                0.0D, 0.01D, 0.0D,
                0
        );
    }

    public static final IntegerProperty LAYERS;
    protected static final VoxelShape[] SHAPE_BY_LAYER;

    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE_BY_LAYER[pState.getValue(LAYERS)];
    }

    public VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return SHAPE_BY_LAYER[pState.getValue(LAYERS)];
    }

    public VoxelShape getVisualShape(BlockState pState, BlockGetter pReader, BlockPos pPos, CollisionContext pContext) {
        return SHAPE_BY_LAYER[pState.getValue(LAYERS)];
    }

    public boolean useShapeForLightOcclusion(BlockState pState) {
        return true;
    }

    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.getValue(LAYERS) == 8 ? 0.2F : 1.0F;
    }

    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockState $$3 = pLevel.getBlockState(pPos.below());
        if ($$3.is(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON)) {
            return false;
        } else if ($$3.is(BlockTags.SNOW_LAYER_CAN_SURVIVE_ON)) {
            return true;
        } else {
            return Block.isFaceFull($$3.getCollisionShape(pLevel, pPos.below()), Direction.UP) || $$3.is(this) && (Integer)$$3.getValue(LAYERS) == 8;
        }
    }

    public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        return !pState.canSurvive(pLevel, pCurrentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    public boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
        int $$2 = pState.getValue(LAYERS);
        if (pUseContext.getItemInHand().is(this.asItem()) && $$2 < 8) {
            if (pUseContext.replacingClickedOnBlock()) {
                return pUseContext.getClickedFace() == Direction.UP;
            } else {
                return true;
            }
        } else {
            return $$2 == 1;
        }
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState $$1 = pContext.getLevel().getBlockState(pContext.getClickedPos());
        if ($$1.is(this)) {
            int $$2 = $$1.getValue(LAYERS);
            return $$1.setValue(LAYERS, Math.min(8, $$2 + 1));
        } else {
            return super.getStateForPlacement(pContext);
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(new Property[]{LAYERS});
    }

    static {
        LAYERS = BlockStateProperties.LAYERS;
        SHAPE_BY_LAYER = new VoxelShape[]{Shapes.empty(), Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0), Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0), Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0), Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0), Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0), Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0), Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0), Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)};
    }
}