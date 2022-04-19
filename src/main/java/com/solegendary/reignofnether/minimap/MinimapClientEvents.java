package com.solegendary.reignofnether.minimap;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MinimapClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    public static final int WORLD_RADIUS = 100; // how many world blocks should be mapped
    public static final int RENDER_RADIUS = 50; // actual size on GUI
    private static final int REFRESH_TICKS_MAX = 100;
    private static int REFRESH_TICKS_CURRENT = 100;
    private static final float CORNER_OFFSET = 10;
    private static final float BG_OFFSET = 6;

    private static final DynamicTexture MAP_TEXTURE = new DynamicTexture(WORLD_RADIUS *2, WORLD_RADIUS *2, true);
    private static final RenderType MAP_RENDER_TYPE = RenderType.textSeeThrough(Minecraft.getInstance()
            .textureManager.register(ReignOfNether.MOD_ID + "_" + "minimap", MAP_TEXTURE));

    private static List<Integer> mapColours = new ArrayList<>();
    private static int xc_world = 0; // world pos x centre, maps to xc
    private static int zc_world = 0; // world pos zcentre, maps to yc

    // viewquad coordinates and offsets to show where the screen is on the minimap
    private static int xtl_quad = 0;
    private static int ytl_quad = 0;
    private static int xbr_quad = 0;
    private static int ybr_quad = 0;
    private static int x_quad_offset = 0;
    private static int y_quad_offset = 0;

    // map vertex coordinates (left, centre, right, top, centre, bottom)
    private static float xl, xc, xr, yt, yc, yb;

    private static final Set<Block> BLOCK_IGNORE_LIST = Set.of(
            Blocks.FERN,
            Blocks.GRASS,
            Blocks.TALL_GRASS,
            Blocks.WHEAT,
            Blocks.MELON_STEM,
            Blocks.POTATOES,
            Blocks.CARROTS,
            Blocks.BEETROOTS,
            Blocks.BROWN_MUSHROOM,
            Blocks.RED_MUSHROOM
    );

    public static void setMapCentre(double x, double z) {
        xc_world = (int) x;
        zc_world = (int) z;
        calcViewQuad(true);
    }

    public static void updateMapTexture()
    {
        NativeImage pixels = MAP_TEXTURE.getPixels();
        if (pixels != null)
        {
            int i = 0;
            for (int y = 0; y < WORLD_RADIUS *2; y ++) {
                for (int x = 0; x < WORLD_RADIUS *2; x ++) {
                    pixels.setPixelRGBA(x, y, mapColours.get(i));
                    i += 1;
                }
            }
            MAP_TEXTURE.upload();
        }
    }

    private static void updateMapColours()
    {
        if (MC.level == null || MC.player == null)
            return;

        long timeBefore = System.currentTimeMillis();

        mapColours = new ArrayList<>();
        for (int z = zc_world - WORLD_RADIUS; z < zc_world + WORLD_RADIUS; z++)
        {
            for (int x = xc_world - WORLD_RADIUS; x < xc_world + WORLD_RADIUS; x++) {

                int y = MC.level.getChunkAt(new BlockPos(x,0,z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                Block block;
                do {
                    block = MC.level.getBlockState(new BlockPos(x,y,z)).getBlock();
                    if (BLOCK_IGNORE_LIST.contains(block))
                        y -= 1;
                    else
                        break;
                } while (true);

                int yNorth = MC.level.getChunkAt(new BlockPos(x,0,z-1)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z-1);
                Block blockNorth;
                do {
                    blockNorth = MC.level.getBlockState(new BlockPos(x,yNorth,z-1)).getBlock();
                    if (BLOCK_IGNORE_LIST.contains(blockNorth))
                        yNorth -= 1;
                    else
                        break;
                } while (true);

                Material mat = MC.level.getBlockState(new BlockPos(x,yNorth,z-1)).getMaterial();
                int col = mat.getColor().col;

                // shade blocks to give elevation effects, excluding liquids and nonblocking blocks (eg. grass, flowers)
                if (!mat.isLiquid()) {
                    if (yNorth > y)
                        col = shadeRGB(col, 0.82F);
                    else if (yNorth < y) {
                        col = shadeRGB(col, 1.16F);
                    }
                }
                else { // shade liquid based on depth
                    int depth = 0;
                    int depthMax = 20;
                    Material matBelow;
                    do {
                        depth += 1;
                        matBelow = MC.level.getBlockState(new BlockPos(x,y-depth,z)).getMaterial();
                    }
                    while(matBelow.isLiquid() && depth < depthMax);

                    // only reduce shade every nth step to have the map look sharper
                    depth = (int) (5*(Math.ceil(Math.abs(depth/5))));

                    col = shadeRGB(col, 1.2F - (0.025F * depth));
                }

                // append 0xFF to include 100% alpha (<< 4 shifts by 1 hex digit)
                mapColours.add(reverseRGB(col) | (0xFF << 24));
            }
        }
        //System.out.println("updated in: " + (System.currentTimeMillis() - timeBefore) + "ms");
        //System.out.println("blocks: " + mapColours.size());
    }

    // lightens or darkens a hex RGB value
    private static int shadeRGB(int col, float mult) {
        int red = (col >> 16) & 0xFF;
        int green = (col >> 8) & 0xFF;
        int blue = (col) & 0xFF;

        if (mult > 1) { // prevent colours going > 255 (0xFF)
            red = Math.min(Math.round(red * mult), 0xFF);
            green = Math.min(Math.round(green * mult), 0xFF);
            blue = Math.min(Math.round(blue * mult), 0xFF);
        }
        else { // prevent colours going < 0
            red = Math.max(Math.round(red * mult), 0);
            green = Math.max(Math.round(green * mult), 0);
            blue = Math.max(Math.round(blue * mult), 0);
        }
        return (red << 16) | (green << 8) | (blue);
    }

    // convert col from RGB -> BGR (for some reason setPixelRGBA reads them backwards)
    private static int reverseRGB(int col) {
        int red = (col >> 16) & 0xFF;
        int green = (col >> 8) & 0xFF;
        int blue = (col) & 0xFF;

        return (blue << 16) | (green << 8) | (red);
    }

    private static void renderMap(PoseStack stack)
    {
        Matrix4f matrix4f = stack.last().pose();

        // place vertices in a diamond shape - left, centre, right, top, centre, bottom
        xl = MC.getWindow().getGuiScaledWidth() - (RENDER_RADIUS * 2) - CORNER_OFFSET;
        xc = MC.getWindow().getGuiScaledWidth() - RENDER_RADIUS - CORNER_OFFSET;
        xr = MC.getWindow().getGuiScaledWidth() - CORNER_OFFSET;
        yt = MC.getWindow().getGuiScaledHeight() - (RENDER_RADIUS * 2) - CORNER_OFFSET;
        yc = MC.getWindow().getGuiScaledHeight() - RENDER_RADIUS - CORNER_OFFSET;
        yb = MC.getWindow().getGuiScaledHeight() - CORNER_OFFSET;

        // background vertex coords need to be slightly larger
        float xl_bg = xl - BG_OFFSET;
        float xc_bg = xc;
        float xr_bg = xr + BG_OFFSET;
        float yt_bg = yt - BG_OFFSET;
        float yc_bg = yc;
        float yb_bg = yb + BG_OFFSET;

        /*
        // render map background first
        ResourceLocation iconFrameResource = new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/map_background.png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        // code taken from GuiComponent.blit()
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f, xc_bg, yb_bg, 0.0F).uv(0.0F, 1.0F).endVertex();
        bufferbuilder.vertex(matrix4f, xr_bg, yc_bg, 0.0F).uv(1.0F, 1.0F).endVertex();
        bufferbuilder.vertex(matrix4f, xc_bg, yt_bg, 0.0F).uv(1.0F, 0.0F).endVertex();
        bufferbuilder.vertex(matrix4f, xl_bg, yc_bg, 0.0F).uv(0.0F, 0.0F).endVertex();
        bufferbuilder.end();
        BufferUploader.end(bufferbuilder);
         */

        // render map itself
        /*
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer consumer = buffer.getBuffer(MAP_RENDER_TYPE);
        consumer.vertex(matrix4f, xc, yb, 0.0F).color(255, 255, 255, 155).uv(0.0F, 1.0F).uv2(255).endVertex();
        consumer.vertex(matrix4f, xr, yc, 0.0F).color(255, 255, 255, 155).uv(1.0F, 1.0F).uv2(255).endVertex();
        consumer.vertex(matrix4f, xc, yt, 0.0F).color(255, 255, 255, 155).uv(1.0F, 0.0F).uv2(255).endVertex();
        consumer.vertex(matrix4f, xl, yc, 0.0F).color(255, 255, 255, 155).uv(0.0F, 0.0F).uv2(255).endVertex();

        buffer.endBatch();

         */
    }

    private static Vec2 worldPosToMapScreenPos(int worldx, int worldz) {
        return new Vec2(0,0);
    }

    // calculates corners of the quad that denotes which part of the map is being viewed right now
    // if saveOffset, then will record the calculated coords to offset the quad for rendering
    // (so that it always appears centered on the map on a reset)
    private static void calcViewQuad(Boolean saveOffset) {
        // ratio of screen pixels to world blocks on the map
        float pixelsToBlocks = (float) RENDER_RADIUS / (float) WORLD_RADIUS;

        // get world pos of top left and bottom right screen corners
        Vector3d vec3tl = MiscUtil.screenPosToWorldPos(MC, 0,0);
        Vector3d vec3br = MiscUtil.screenPosToWorldPos(MC, MC.getWindow().getGuiScaledWidth(),MC.getWindow().getGuiScaledHeight());
        double xtl_world = vec3tl.x;
        double ztl_world = vec3tl.z;
        double xbr_world = vec3br.x;
        double zbr_world = vec3br.z;

        // calculate the screen location if the map was NOT angled 45 degrees, then rotate them
        double xtl = (xtl_world - xc_world) * pixelsToBlocks;
        double ytl = (ztl_world - zc_world) * pixelsToBlocks;
        double xbr = (xbr_world - xc_world) * pixelsToBlocks;
        double ybr = (zbr_world - zc_world) * pixelsToBlocks;

        Vec2 tl_rot = MyMath.rotateCoords((float) xtl, (float) ytl, -45);
        Vec2 br_rot = MyMath.rotateCoords((float) xbr, (float) ybr, -45);

        xtl_quad = (int) tl_rot.x;
        ytl_quad = (int) tl_rot.y;
        xbr_quad = (int) br_rot.x;
        ybr_quad = (int) br_rot.y;

        if (saveOffset) {
            x_quad_offset = (int) tl_rot.x;
            y_quad_offset = (int) tl_rot.y;
        }
    }

    // the quad that denotes which part of the map is being viewed right now
    private static void renderViewQuad(PoseStack stack) {

        GuiComponent.drawString(stack, MC.font, "xtl " + (x_quad_offset), 0,0, 0xFFFFFF);
        GuiComponent.drawString(stack, MC.font, "ztl " + (y_quad_offset), 0,10, 0xFFFFFF);
        GuiComponent.drawString(stack, MC.font, "xbr " + (x_quad_offset), 0,20, 0xFFFFFF);
        GuiComponent.drawString(stack, MC.font, "zbr " + (y_quad_offset), 0,30, 0xFFFFFF);

        GuiComponent.fill(stack,
                xtl_quad - x_quad_offset + (int) xc,
                ytl_quad - y_quad_offset + (int) yc,
                xbr_quad - x_quad_offset + (int) xc,
                ybr_quad - y_quad_offset + (int) yc,
                0x0841e868); // ARGB(hex); note that alpha ranges between ~0-16, not 0-255
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseDragEvent.Pre evt) {
        // TODO: when clicking on map move player there
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post evt) {
        if (!OrthoviewClientEvents.isEnabled())
            return;

        REFRESH_TICKS_CURRENT -= 1;
        if (REFRESH_TICKS_CURRENT <= 0) {
            REFRESH_TICKS_CURRENT = REFRESH_TICKS_MAX;
            updateMapColours();
            updateMapTexture();
        }
        renderMap(evt.getMatrixStack());
        calcViewQuad(false);
        renderViewQuad(evt.getMatrixStack());
    }
}