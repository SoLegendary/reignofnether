package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.worldborder.WorldBorderClientEvents;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

// Wraps an existing BlockColor and multiplies the result by FOG_TINT_RGB in dark chunks.
// Biome-tinted blocks are skipped - BiomeColorsMixin handles those.
public class FogTintingBlockColor implements BlockColor {

    @Nullable private final BlockColor delegate;

    public FogTintingBlockColor(@Nullable BlockColor delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        int base = (delegate != null) ? delegate.getColor(state, level, pos, tintIndex) : -1;
        if (pos == null || level == null) return base;

        int tint = 0;
        if (WorldBorderClientEvents.isOutsideWorldBorder(pos)) {
            tint = WorldBorderClientEvents.OUTSIDE_WORLD_BORDER_TINT;
        }
        else if (FogOfWarClientEvents.isEnabled() && !FogOfWarClientEvents.isBlockVisible(pos)) {
            tint = FogOfWarClientEvents.FOG_TINT_RGB;
        }
        if (tint == 0) return base;

        int original = (base == -1) ? 0xFFFFFF : base;
        int r = (((original >> 16) & 0xFF) * ((tint >> 16) & 0xFF)) / 255;
        int g = (((original >> 8)  & 0xFF) * ((tint >> 8)  & 0xFF)) / 255;
        int b = (( original        & 0xFF) * ( tint        & 0xFF)) / 255;
        return (r << 16) | (g << 8) | b;
    }
}
