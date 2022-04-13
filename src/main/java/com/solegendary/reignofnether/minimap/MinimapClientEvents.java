package com.solegendary.reignofnether.minimap;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class MinimapClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    public static final int RADIUS = 100;
    private static final int mapRefreshTicksMax = 100;
    private static int mapRefreshTicksCurrent = 100;
    private static List<Integer> mapColours = new ArrayList<>();

    private static DynamicTexture MAP_TEXTURE = new DynamicTexture(RADIUS*2, RADIUS*2, true);
    private static RenderType MAP_RENDER_TYPE = RenderType.textSeeThrough(Minecraft.getInstance()
            .textureManager.register(ReignOfNether.MOD_ID + "_" + "minimap", MAP_TEXTURE));

    public static void updateMapTexture()
    {
        if (MAP_TEXTURE != null)
            MAP_TEXTURE.close();

        //MAP_TEXTURE = new DynamicTexture(RADIUS*2, RADIUS*2, true);
        NativeImage pixels = MAP_TEXTURE.getPixels();
        if (pixels != null)
        {
            int i = 0;
            for (int y = 0; y < RADIUS*2; y ++) {
                for (int x = 0; x < RADIUS*2; x ++) {
                    i += 1;
                    //pixels.setPixelRGBA(x, y, MaterialColor.getColorFromPackedId(mapColours.get(i)));
                    pixels.setPixelRGBA(x, y, 0x7FB23864);
                }
            }
            MAP_TEXTURE.upload();
            MAP_RENDER_TYPE = RenderType.textSeeThrough(Minecraft.getInstance()
                    .textureManager.register(ReignOfNether.MOD_ID + "_" + "minimap", MAP_TEXTURE));
        }
    }

    private static void updateMapColours()
    {
        if (MC.level == null || MC.player == null)
            return;

        long timeBefore = System.currentTimeMillis();

        mapColours = new ArrayList<>();
        for (int x = (int) MC.player.getX() - RADIUS; x < (int) MC.player.getX() + RADIUS; x++)
        {
            for (int z = (int) MC.player.getZ() - RADIUS; z < (int) MC.player.getZ() + RADIUS; z++) {
                int y = MC.level.getChunkAt(new BlockPos(x,0,z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                int col = MC.level.getBlockState(new BlockPos(x,y,z)).getMaterial().getColor().col;

                // add 0xFF000000 to include 100% transparency
                mapColours.add(col + (1677721600));
            }
        }
        System.out.println("updated in: " + (System.currentTimeMillis() - timeBefore) + "ms");
        System.out.println("blocks: " + mapColours.size());
    }

    private static void renderMap(PoseStack stack, float x, float y, float x2, float y2)
    {
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer consumer = buffer.getBuffer(MAP_RENDER_TYPE);
        Matrix4f matrix4f = stack.last().pose();

        putTextureVertex(consumer, matrix4f, x, y, x2, y2, 255, 1.0F);
        buffer.endBatch();
    }

    public static void putTextureVertex(VertexConsumer consumer, Matrix4f matrix4f, float x, float y, float x2, float y2, int light, float alpha)
    {
        int a = (int) (alpha * 255.0F);
        consumer.vertex(matrix4f, x, y2, 0.0F).color(255, 255, 255, a).uv(0.0F, 1.0F).uv2(light).endVertex();
        consumer.vertex(matrix4f, x2, y2, 0.0F).color(255, 255, 255, a).uv(1.0F, 1.0F).uv2(light).endVertex();
        consumer.vertex(matrix4f, x2, y, 0.0F).color(255, 255, 255, a).uv(1.0F, 0.0F).uv2(light).endVertex();
        consumer.vertex(matrix4f, x, y, 0.0F).color(255, 255, 255, a).uv(0.0F, 0.0F).uv2(light).endVertex();
    }

    @SubscribeEvent
    public static void renderOverlay(RenderGameOverlayEvent.Post evt) {
        mapRefreshTicksCurrent -= 1;

        if (mapRefreshTicksCurrent <= 0) {
            mapRefreshTicksCurrent = mapRefreshTicksMax;
            updateMapColours();
            updateMapTexture();
        }
        renderMap(evt.getMatrixStack(), 0,0, RADIUS, RADIUS);
    }
}
