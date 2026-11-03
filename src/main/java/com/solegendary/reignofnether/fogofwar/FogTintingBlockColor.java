package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.worldborder.WorldBorderClientEvents;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

// Wraps an existing BlockColor and multiplies the result by FOG_TINT_RGB in dark chunks.
// Biome-tinted blocks only get fogged on FOG_ONLY_TINT_INDEX quads - BiomeColorsMixin handles their tinted quads.
public class FogTintingBlockColor implements BlockColor {

    // tint index FogTintingBakedModel assigns to originally-untinted quads: fog only, never the block's own colour
    public static final int FOG_ONLY_TINT_INDEX = 1000;

    @Nullable private final BlockColor delegate;
    private final boolean biomeTinted;

    public FogTintingBlockColor(@Nullable BlockColor delegate, boolean biomeTinted) {
        this.delegate = delegate;
        this.biomeTinted = biomeTinted;
    }

    @Override
    public int getColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        boolean fogOnly = tintIndex == FOG_ONLY_TINT_INDEX;
        int base = (delegate != null && !fogOnly) ? delegate.getColor(state, level, pos, tintIndex) : -1;
        if (pos == null || level == null || (biomeTinted && !fogOnly)) return base;

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
