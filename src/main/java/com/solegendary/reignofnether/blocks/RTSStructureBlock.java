//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class RTSStructureBlock extends BaseEntityBlock implements GameMasterBlock {
    public static final EnumProperty<StructureMode> MODE;

    public RTSStructureBlock(BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, StructureMode.LOAD));
    }

    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new RTSStructureBlockEntity(pPos, pState);
    }

    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        BlockEntity $$6 = pLevel.getBlockEntity(pPos);
        if ($$6 instanceof RTSStructureBlockEntity) {
            return ((RTSStructureBlockEntity)$$6).usedBy(pPlayer) ? InteractionResult.sidedSuccess(pLevel.isClientSide) : InteractionResult.PASS;
        } else {
            return InteractionResult.PASS;
        }
    }

    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        if (!pLevel.isClientSide) {
            if (pPlacer != null) {
                BlockEntity $$5 = pLevel.getBlockEntity(pPos);
                if ($$5 instanceof RTSStructureBlockEntity) {
                    ((RTSStructureBlockEntity)$$5).createdBy(pPlacer);
                }
            }

        }
    }

    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(new Property[]{MODE});
    }

    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        if (pLevel instanceof ServerLevel) {
            BlockEntity $$6 = pLevel.getBlockEntity(pPos);
            if ($$6 instanceof RTSStructureBlockEntity) {
                RTSStructureBlockEntity $$7 = (RTSStructureBlockEntity)$$6;
                boolean $$8 = pLevel.hasNeighborSignal(pPos);
                boolean $$9 = $$7.isPowered();
                if ($$8 && !$$9) {
                    $$7.setPowered(true);
                    this.trigger((ServerLevel)pLevel, $$7);
                } else if (!$$8 && $$9) {
                    $$7.setPowered(false);
                }

            }
        }
    }

    private void trigger(ServerLevel pLevel, RTSStructureBlockEntity pBlockEntity) {
        switch (pBlockEntity.getMode()) {
            case SAVE:
                pBlockEntity.saveStructure(false);
                break;
            case LOAD:
                pBlockEntity.loadStructure(pLevel, false);
                break;
            case CORNER:
                pBlockEntity.unloadStructure();
            case DATA:
        }

    }

    static {
        MODE = BlockStateProperties.STRUCTUREBLOCK_MODE;
    }
}
