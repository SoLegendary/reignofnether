package com.solegendary.reignofnether.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.21.1 made {@code StructureBlockEntity}'s {@code mode} and {@code structureName} fields private,
 * and the access transformer entries that used to open them up are not visible to
 * {@code compileJava}. {@code RTSStructureBlockEntity} reads and writes both.
 *
 * <p>{@code getRelatedCorners} and {@code updateBlockState} are not exposed here: they are called
 * from inside {@code detectSize} and {@code setMode}, so overriding them in the subclass would
 * never take effect. {@code StructureBlockEntityMixin} redirects those instead.
 */
@Mixin(StructureBlockEntity.class)
public interface StructureBlockEntityAccessor {

    @Accessor("mode")
    StructureMode reignOfNether$getModeField();

    @Accessor("mode")
    void reignOfNether$setModeField(StructureMode mode);

    @Accessor("structureName")
    ResourceLocation reignOfNether$getStructureNameField();
}