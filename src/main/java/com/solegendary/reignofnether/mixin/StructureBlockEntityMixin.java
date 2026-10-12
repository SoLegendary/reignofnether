package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.blocks.RTSStructureBlockEntity;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * {@code StructureBlockEntity#getRelatedCorners} and {@code #updateBlockState} became private in
 * 1.21.1, so {@code RTSStructureBlockEntity} can no longer override them. Both are only ever
 * called from inside the parent class ({@code detectSize} and {@code setMode}/{@code
 * loadAdditional} respectively), so overriding in the subclass would silently do nothing — the
 * vanilla versions would run instead and never see the mod's own structure block.
 *
 * <p>Injecting here keeps the RTS variants on the code path that actually runs, and only when the
 * entity really is one of ours.
 */
@Mixin(StructureBlockEntity.class)
public abstract class StructureBlockEntityMixin {

    @Shadow private StructureMode mode;

    @Shadow public abstract StructureMode getMode();

    @Inject(
            method = "updateBlockState",
            at = @At("HEAD"),
            cancellable = true
    )
    private void reignofnether$updateBlockState(CallbackInfo ci) {
        if (!((Object) this instanceof RTSStructureBlockEntity))
            return;

        StructureBlockEntity self = (StructureBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null)
            return;

        BlockPos pos = self.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockRegistrar.RTS_STRUCTURE_BLOCK.get()))
            level.setBlock(pos, state.setValue(StructureBlock.MODE, this.mode), 2);

        ci.cancel();
    }

    @Inject(
            method = "getRelatedCorners",
            at = @At("HEAD"),
            cancellable = true
    )
    private void reignofnether$getRelatedCorners(BlockPos pMinPos, BlockPos pMaxPos,
                                                 CallbackInfoReturnable<Stream<BlockPos>> cir) {
        if (!((Object) this instanceof RTSStructureBlockEntity rts))
            return;

        Level level = ((StructureBlockEntity) (Object) this).getLevel();
        Objects.requireNonNull(level);

        // same as vanilla's, but matching on our block instead of Blocks.STRUCTURE_BLOCK
        cir.setReturnValue(BlockPos.betweenClosedStream(pMinPos, pMaxPos)
                .filter(pos -> level.getBlockState(pos).is(BlockRegistrar.RTS_STRUCTURE_BLOCK.get()))
                .map(level::getBlockEntity)
                .filter(be -> be instanceof StructureBlockEntity)
                .filter(be -> ((StructureBlockEntity) be).getMode() == StructureMode.CORNER
                        && Objects.equals(rts.getStructureName(), ((StructureBlockEntity) be).getStructureName()))
                .map(BlockEntity::getBlockPos));
    }
}