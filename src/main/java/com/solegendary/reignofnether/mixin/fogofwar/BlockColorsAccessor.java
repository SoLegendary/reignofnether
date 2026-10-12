package com.solegendary.reignofnether.mixin.fogofwar;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

// expose BlockColors' map so we can wrap existing providers with FogTintingBlockColor
@Mixin(BlockColors.class)
public interface BlockColorsAccessor {
    @Accessor
    Map<Block, BlockColor> getBlockColors();
}
