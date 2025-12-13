package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.registrars.BlockEntityRegistrar;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull StructureMode getMode() {
        if (this.mode == StructureMode.LOAD)
            return StructureMode.SAVE;
        return this.mode;
    }

    @Override
    public void setMode(@NotNull StructureMode pMode) {
        if (pMode == StructureMode.LOAD)
            pMode = StructureMode.CORNER; // don't allow loading by block, only by building placement menu
        this.mode = pMode;
        BlockState $$1 = this.level.getBlockState(this.getBlockPos());
        if ($$1.is(BlockRegistrar.RTS_STRUCTURE_BLOCK.get())) {
            this.level.setBlock(this.getBlockPos(), $$1.setValue(StructureBlock.MODE, pMode), 2);
        }
    }

    @Override
    public @NotNull Stream<BlockPos> getRelatedCorners(@NotNull BlockPos pMinPos, @NotNull BlockPos pMaxPos) {
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
        if (result && level != null && level.getServer() != null) { // this function only runs serverside but we can only render clientside unless in singleplayer
            if (this.getShowBoundingBox() && (!level.isClientSide() && !level.getServer().isDedicatedServer())) {
                CustomBuildingClientEvents.rtsStructuresToRenderBB.add(this.getBlockPos());
            }
        }
        return result;
    }

    @Override
    public boolean saveStructure(boolean pWriteToDisk) {
        boolean result = super.saveStructure(pWriteToDisk);
        if (result && level != null) {
            if (!level.isClientSide()) {
                CustomBuildingServerEvents.createAndRegisterNewCustomBuilding(structureName, getStructureName(), (ServerLevel) this.level, getBlockPos());
            }
        }
        return result;
    }
}
