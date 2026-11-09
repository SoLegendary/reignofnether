package com.solegendary.reignofnether.minimap;

import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * The colour of one column on the map, shaded for height and water depth. Shared by the client's minimap and the
 * server, which draws the whole map once at load (MapColourServerEvents), so both paint the same picture.
 */
public final class MapColours {

    private MapColours() { }

    /** Plain RGB, before fog or border darkening. northZ is the row compared for slope shading (z itself at an edge). */
    public static int column(Level level, int x, int z, int northZ, BlockPos.MutableBlockPos pos) {
        int y = surfaceY(level, x, z, pos);
        // a full snow layer reports the block above it as the surface
        boolean snow = level.getBlockState(pos.set(x, y, z)).getBlock() instanceof SnowLayerBlock
                || level.getBlockState(pos.set(x, y - 1, z)).getBlock() instanceof SnowLayerBlock;
        int yNorth = surfaceY(level, x, northZ, pos);

        pos.set(x, yNorth, northZ);
        MapColor mat = level.getBlockState(pos).getMapColor(level, pos);
        int rgb = mat.col;
        if (snow)
            rgb = 0xFFFFFF;

        // shade blocks to give elevation effects, excluding liquids and nonblocking blocks (eg. grass, flowers)
        if (level.getBlockState(pos).getFluidState().isEmpty()) {
            if (yNorth > y)
                rgb = MiscUtil.shadeHexRGB(rgb, 0.82F);
            else if (yNorth < y)
                rgb = MiscUtil.shadeHexRGB(rgb, 1.16F);
        } else { // shade liquid based on depth
            int depth = 0;
            int depthMax = 20;
            BlockState matBelow;
            do {
                depth += 1;
                matBelow = level.getBlockState(pos.set(x, y - depth, z));
            } while (!matBelow.getFluidState().isEmpty() && depth < depthMax);

            // only reduce shade every nth step to have the map look sharper
            depth = (int) (5 * (Math.ceil(Math.abs(depth / 5))));
            rgb = MiscUtil.shadeHexRGB(rgb, 1.2F - (0.025F * depth));
        }
        return rgb;
    }

    // the highest solid or liquid block, or the top of a snow layer
    private static int surfaceY(Level level, int x, int z, BlockPos.MutableBlockPos pos) {
        int y = level.getChunkAt(pos.set(x, 0, z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        while (true) {
            BlockState bs = level.getBlockState(pos.set(x, y, z));
            if (bs.getBlock() instanceof SnowLayerBlock)
                return y + (int) (bs.getValue(SnowLayerBlock.LAYERS) * (1.0F / 8.0F));
            if (!bs.isSolid() && bs.getFluidState().isEmpty() && y > 0)
                y -= 1;
            else
                return y;
        }
    }
}
