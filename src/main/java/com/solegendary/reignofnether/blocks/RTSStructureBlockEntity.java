package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.mixin.StructureBlockEntityAccessor;
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

    // updateBlockState and getRelatedCorners became private in 1.21.1 and are only ever called
    // from inside StructureBlockEntity, so they cannot be overridden here; StructureBlockEntityMixin
    // redirects them to the RTS versions instead.

    @Override
    public @NotNull StructureMode getMode() {
        // read the raw field, not getMode(), which this method overrides
        StructureMode raw = ((StructureBlockEntityAccessor) this).reignOfNether$getModeField();
        if (raw == StructureMode.LOAD)
            return StructureMode.SAVE;
        return raw;
    }

    @Override
    public void setMode(@NotNull StructureMode pMode) {
        if (pMode == StructureMode.LOAD)
            pMode = StructureMode.CORNER; // don't allow loading by block, only by building placement menu
        ((StructureBlockEntityAccessor) this).reignOfNether$setModeField(pMode);
        BlockState $$1 = this.level.getBlockState(this.getBlockPos());
        if ($$1.is(BlockRegistrar.RTS_STRUCTURE_BLOCK.get())) {
            this.level.setBlock(this.getBlockPos(), $$1.setValue(StructureBlock.MODE, pMode), 2);
        }
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
                BlockPos pos = getBlockPos().offset(getStructurePos()).offset(0,-1,0);
                CustomBuildingServerEvents.createAndRegisterNewCustomBuilding(((StructureBlockEntityAccessor) this).reignOfNether$getStructureNameField(), getStructureName(), (ServerLevel) this.level, pos, getStructureSize());
            }
        }
        return result;
    }
}
