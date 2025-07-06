package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.building.CustomBuildingClientEvents;
import com.solegendary.reignofnether.registrars.BlockEntityRegistrar;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;

import java.util.Objects;
import java.util.stream.Stream;

public class RTSStructureBlockEntity extends StructureBlockEntity {

    public RTSStructureBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(pPos, pBlockState);

        this.type = BlockEntityRegistrar.RTS_STRUCTURE_BLOCK_ENTITY.get();
    }

    @Override
    public void updateBlockState() {
        if (this.level != null) {
            BlockPos $$0 = this.getBlockPos();
            BlockState $$1 = this.level.getBlockState($$0);
            if ($$1.is(BlockRegistrar.RTS_STRUCTURE_BLOCK.get())) {
                this.level.setBlock($$0, $$1.setValue(StructureBlock.MODE, this.mode), 2);
            }
        }
    }

    @Override
    public void setMode(StructureMode pMode) {
        this.mode = pMode;
        BlockState $$1 = this.level.getBlockState(this.getBlockPos());
        if ($$1.is(BlockRegistrar.RTS_STRUCTURE_BLOCK.get())) {
            this.level.setBlock(this.getBlockPos(), $$1.setValue(StructureBlock.MODE, pMode), 2);
        }
    }

    @Override
    public Stream<BlockPos> getRelatedCorners(BlockPos pMinPos, BlockPos pMaxPos) {
        Objects.requireNonNull(level);
        Stream<BlockPos> stream = BlockPos.betweenClosedStream(pMinPos, pMaxPos).filter((p_272561_) ->
                level.getBlockState(p_272561_).is(BlockRegistrar.RTS_STRUCTURE_BLOCK.get()));
        return stream.map(level::getBlockEntity).filter((p_155802_) ->
                p_155802_ instanceof StructureBlockEntity)
            .filter((p_155787_) ->
                ((StructureBlockEntity) p_155787_).mode == StructureMode.CORNER &&
                    Objects.equals(this.structureName, ((StructureBlockEntity) p_155787_).structureName)
            ).map(BlockEntity::getBlockPos);
    }

    @Override
    public boolean detectSize() {
        boolean result = super.detectSize();
        if (result) {
            if (this.getShowBoundingBox()) {
                CustomBuildingClientEvents.rtsStructuresToRenderBB.add(this.getBlockPos());
            }
        }
        return result;
    }
}
