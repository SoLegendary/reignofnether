package com.solegendary.reignofnether.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import javax.annotation.Nullable;

/** Turns an entity's model into a flat translucent silhouette, dropping everything else it draws (nametags, leashes) */
public class GhostBufferSource implements MultiBufferSource {

    private static final VertexConsumer DISCARD = new GhostVertexConsumer(null, 0, 0, 0, 0);

    private final MultiBufferSource.BufferSource buffer;
    private int r, g, b, a;

    public GhostBufferSource(MultiBufferSource.BufferSource buffer) {
        this.buffer = buffer;
    }

    public void setColor(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        if (type.mode() != VertexFormat.Mode.QUADS || type.format() != DefaultVertexFormat.NEW_ENTITY)
            return DISCARD;
        return new GhostVertexConsumer(buffer.getBuffer(MyRenderer.GHOST), r, g, b, a);
    }

    private record GhostVertexConsumer(@Nullable VertexConsumer delegate, int r, int g, int b, int a) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            if (delegate != null)
                delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            if (delegate != null)
                delegate.color(r, g, b, a);
            return this;
        }

        @Override
        public void endVertex() {
            if (delegate != null)
                delegate.endVertex();
        }

        @Override public VertexConsumer uv(float u, float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void defaultColor(int red, int green, int blue, int alpha) { }
        @Override public void unsetDefaultColor() { }
    }
}
