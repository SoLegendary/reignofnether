package com.solegendary.reignofnether.fogofwar;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// give untinted quads FOG_ONLY_TINT_INDEX so the BlockColor fog wrap runs on every face without the block's own
// colour bleeding in (tintIndex 0 would paint grass-block dirt sides biome-green)
public class FogTintingBakedModel extends BakedModelWrapper<BakedModel> {

    public FogTintingBakedModel(BakedModel original) {
        super(original);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(BlockState state, Direction side, @NotNull RandomSource rand) {
        return inject(super.getQuads(state, side, rand));
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(BlockState state, Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, RenderType renderType) {
        return inject(super.getQuads(state, side, rand, data, renderType));
    }

    private static List<BakedQuad> inject(List<BakedQuad> input) {
        if (input.isEmpty()) return input;
        // skip alloc when nothing needs rewriting
        boolean needsRewrite = false;
        for (int i = 0; i < input.size(); i++) {
            if (input.get(i).getTintIndex() == -1) { needsRewrite = true; break; }
        }
        if (!needsRewrite) return input;
        List<BakedQuad> out = new ArrayList<>(input.size());
        for (BakedQuad q : input) {
            if (q.getTintIndex() == -1) {
                out.add(new BakedQuad(q.getVertices(), FogTintingBlockColor.FOG_ONLY_TINT_INDEX, q.getDirection(), q.getSprite(), q.isShade()));
            } else {
                out.add(q);
            }
        }
        return out;
    }
}
