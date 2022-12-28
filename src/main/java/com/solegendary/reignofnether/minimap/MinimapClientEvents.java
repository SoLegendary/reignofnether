package com.solegendary.reignofnether.minimap;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class MinimapClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    public static final int WORLD_RADIUS = 100; // how many world blocks should be mapped
    public static final int MAP_RADIUS = 50; // actual size on GUI
    private static final int REFRESH_TICKS_MAX = 100;
    private static int refreshTicksCurrent = 0;
    private static final float CORNER_OFFSET = 10;
    private static final float BG_OFFSET = 6;

    private static final int UNIT_RADIUS = 3;
    private static final int UNIT_THICKNESS = 1;
    private static final int BUILDING_RADIUS = 7;
    private static final int BUILDING_THICKNESS = 2;

    private static final DynamicTexture MAP_TEXTURE = new DynamicTexture(WORLD_RADIUS * 2, WORLD_RADIUS * 2, true);
    private static final RenderType MAP_RENDER_TYPE = RenderType.textSeeThrough(Minecraft.getInstance()
            .textureManager.register(ReignOfNether.MOD_ID + "_" + "minimap", MAP_TEXTURE));

    private static int[][] mapColours = new int[WORLD_RADIUS*2][WORLD_RADIUS*2];
    private static int xc_world = 0; // world pos x centre, maps to xc
    private static int zc_world = 0; // world pos zcentre, maps to yc

    private static float xl, xc, xr, yt, yc, yb;

    public static void setMapCentre(double x, double z) {
        xc_world = (int) x;
        zc_world = (int) z;
    }

    public static void updateMapTexture()
    {
        // if camera is off the map, start panning the centre of the map
        double xCam = MC.player.getX();
        double zCam = MC.player.getZ();
        double xDiff1 = xCam - (xc_world + WORLD_RADIUS);
        if (xDiff1 > 0)
            xc_world += xDiff1;
        double zDiff1 = zCam - (zc_world + WORLD_RADIUS);
        if (zDiff1 > 0)
            zc_world += zDiff1;
        double xDiff2 = xCam - (xc_world - WORLD_RADIUS);
        if (xDiff2 < 0)
            xc_world += xDiff2;
        double zDiff2 = zCam - (zc_world - WORLD_RADIUS);
        if (zDiff2 < 0)
            zc_world += zDiff2;

        NativeImage pixels = MAP_TEXTURE.getPixels();
        if (pixels != null)
        {
            int i = 0;
            for (int z = 0; z < WORLD_RADIUS *2; z ++) {
                for (int x = 0; x < WORLD_RADIUS *2; x ++) {
                    pixels.setPixelRGBA(x, z, mapColours[x][z]);
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

        // get world position of corners of the screen
        Vector3d[] corners = new Vector3d[] {
            MiscUtil.screenPosToWorldPos(MC, 0,0),
            MiscUtil.screenPosToWorldPos(MC, 0, MC.getWindow().getGuiScaledHeight()),
            MiscUtil.screenPosToWorldPos(MC, MC.getWindow().getGuiScaledWidth(), MC.getWindow().getGuiScaledHeight()),
            MiscUtil.screenPosToWorldPos(MC, MC.getWindow().getGuiScaledWidth(), 0)
        };
        // adjust corners according to camera angle
        Vector3d lookVector = CursorClientEvents.getPlayerLookVector();
        corners[0] = MyMath.addVector3d(corners[0], lookVector, 90-OrthoviewClientEvents.getCamRotY());
        corners[1] = MyMath.addVector3d(corners[1], lookVector, 75-OrthoviewClientEvents.getCamRotY());
        corners[2] = MyMath.addVector3d(corners[2], lookVector, 75-OrthoviewClientEvents.getCamRotY());
        corners[3] = MyMath.addVector3d(corners[3], lookVector, 90-OrthoviewClientEvents.getCamRotY());

        mapColours = new int[WORLD_RADIUS*2][WORLD_RADIUS*2];

        for (int z = zc_world - WORLD_RADIUS; z < zc_world + WORLD_RADIUS; z++)
        {
            for (int x = xc_world - WORLD_RADIUS; x < xc_world + WORLD_RADIUS; x++) {

                int y = MC.level.getChunkAt(new BlockPos(x,0,z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                BlockState bs;
                do {
                    bs = MC.level.getBlockState(new BlockPos(x,y,z));
                    if (!bs.getMaterial().isSolid() && !bs.getMaterial().isLiquid() && y > 0)
                        y -= 1;
                    else
                        break;
                } while (true);

                int yNorth = MC.level.getChunkAt(new BlockPos(x,0,z-1)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z-1);
                BlockState bsNorth;
                do {
                    bsNorth = MC.level.getBlockState(new BlockPos(x,yNorth,z-1));
                    if (!bsNorth.getMaterial().isSolid() && !bsNorth.getMaterial().isLiquid() && yNorth > 0)
                        yNorth -= 1;
                    else
                        break;
                } while (true);

                Material mat = MC.level.getBlockState(new BlockPos(x,yNorth,z-1)).getMaterial();
                int rgb = mat.getColor().col;

                // shade blocks to give elevation effects, excluding liquids and nonblocking blocks (eg. grass, flowers)
                if (!mat.isLiquid()) {
                    if (yNorth > y)
                        rgb = MiscUtil.shadeHexRGB(rgb, 0.82F);
                    else if (yNorth < y) {
                        rgb = MiscUtil.shadeHexRGB(rgb, 1.16F);
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
                    rgb = MiscUtil.shadeHexRGB(rgb, 1.2F - (0.025F * depth));
                }

                // draw view quad
                for (int i = 0; i < corners.length; i++) {
                    int j = i + 1;
                    if (j >= corners.length) j = 0;

                    if (MyMath.isPointOnLine(
                            new Vec2((float) corners[i].x, (float) corners[i].z),
                            new Vec2((float) corners[j].x, (float) corners[j].z),
                            new Vec2(x,z),
                            OrthoviewClientEvents.getZoom() * 2 // larger = thicker line
                    ))
                        rgb = 0xFFFFFF;
                }
                // normalise xz back to 0,0
                int x0 = x - xc_world + WORLD_RADIUS;
                int z0 = z - zc_world + WORLD_RADIUS;
                // append 0xFF to include 100% alpha (<< 4 shifts by 1 hex digit)
                mapColours[x0][z0] = MiscUtil.reverseHexRGB(rgb) | (0xFF << 24);
            }
        }


        // draw buildings
        for (Building building : BuildingClientEvents.getBuildings()) {
            int xc = building.originPos.getX() + (BUILDING_RADIUS / 2);
            int zc = building.originPos.getZ() + (BUILDING_RADIUS / 2);

            for (int x = xc - BUILDING_RADIUS; x < xc + BUILDING_RADIUS; x++) {
                for (int z = zc - BUILDING_RADIUS; z < zc + BUILDING_RADIUS; z++) {
                    if (isXZinsideMap(x,z)) {
                        int x0 = x - xc + BUILDING_RADIUS;
                        int z0 = z - zc + BUILDING_RADIUS;
                        int rgb = 0x000000;

                        // if pixel is on the edge of the square keep it coloured black
                        if (!(x0 < BUILDING_THICKNESS || x0 >= (BUILDING_RADIUS * 2) - BUILDING_THICKNESS ||
                              z0 < BUILDING_THICKNESS || z0 >= (BUILDING_RADIUS * 2) - BUILDING_THICKNESS)) {
                            switch (BuildingClientEvents.getPlayerToBuildingRelationship(building)) {
                                case OWNED -> rgb = 0x00FF00;
                                case FRIENDLY -> rgb = 0x0000FF;
                                case HOSTILE -> rgb = 0xFF0000;
                                case NEUTRAL -> rgb = 0xFFFF00;
                            }
                        }
                        int xN = x - xc_world + (MAP_RADIUS * 2);
                        int zN = z - zc_world + (MAP_RADIUS * 2);
                        mapColours[xN][zN] = MiscUtil.reverseHexRGB(rgb) | (0xFF << 24);
                    }
                }
            }
        }

        // draw units
        for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
            int xc = entity.getOnPos().getX();
            int zc = entity.getOnPos().getZ();

            for (int x = xc - UNIT_RADIUS; x < xc + UNIT_RADIUS; x++) {
                for (int z = zc - UNIT_RADIUS; z < zc + UNIT_RADIUS; z++) {
                    if (isXZinsideMap(x,z)) {
                        int x0 = x - xc + UNIT_RADIUS;
                        int z0 = z - zc + UNIT_RADIUS;
                        int rgb = 0x000000;

                        // if pixel is on the edge of the square keep it coloured black
                        if (!(x0 < UNIT_THICKNESS || x0 >= (UNIT_RADIUS * 2) - UNIT_THICKNESS ||
                              z0 < UNIT_THICKNESS || z0 >= (UNIT_RADIUS * 2) - UNIT_THICKNESS)) {
                            switch (UnitClientEvents.getPlayerToEntityRelationship(entity)) {
                                case OWNED -> rgb = 0x00FF00;
                                case FRIENDLY -> rgb = 0x0000FF;
                                case HOSTILE -> rgb = 0xFF0000;
                                case NEUTRAL -> rgb = 0xFFFF00;
                            }
                        }
                        int xN = x - xc_world + (MAP_RADIUS * 2);
                        int zN = z - zc_world + (MAP_RADIUS * 2);
                        mapColours[xN][zN] = MiscUtil.reverseHexRGB(rgb) | (0xFF << 24);
                    }
                }
            }
        }
    }

    private static boolean isXZinsideMap(int x, int z) {
        return x >= xc_world - WORLD_RADIUS && x < xc_world + WORLD_RADIUS &&
               z >= zc_world - WORLD_RADIUS && z < zc_world + WORLD_RADIUS;
    }

    private static void renderMap(PoseStack stack)
    {
        Matrix4f matrix4f = stack.last().pose();

        // place vertices in a diamond shape - left, centre, right, top, centre, bottom
        // map vertex coordinates (left, centre, right, top, centre, bottom)
        xl = MC.getWindow().getGuiScaledWidth() - (MAP_RADIUS * 2) - CORNER_OFFSET;
        xc = MC.getWindow().getGuiScaledWidth() - MAP_RADIUS - CORNER_OFFSET;
        xr = MC.getWindow().getGuiScaledWidth() - CORNER_OFFSET;
        yt = MC.getWindow().getGuiScaledHeight() - (MAP_RADIUS * 2) - CORNER_OFFSET;
        yc = MC.getWindow().getGuiScaledHeight() - MAP_RADIUS - CORNER_OFFSET;
        yb = MC.getWindow().getGuiScaledHeight() - CORNER_OFFSET;

        // background vertex coords need to be slightly larger
        float xl_bg = xl - BG_OFFSET;
        float xc_bg = xc;
        float xr_bg = xr + BG_OFFSET;
        float yt_bg = yt - BG_OFFSET;
        float yc_bg = yc;
        float yb_bg = yb + BG_OFFSET;

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

        BufferUploader.drawWithShader(bufferbuilder.end());

        // render map itself
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer consumer = buffer.getBuffer(MAP_RENDER_TYPE);
        consumer.vertex(matrix4f, xc, yb, 0.0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(255).endVertex();
        consumer.vertex(matrix4f, xr, yc, 0.0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(255).endVertex();
        consumer.vertex(matrix4f, xc, yt, 0.0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(255).endVertex();
        consumer.vertex(matrix4f, xl, yc, 0.0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(255).endVertex();

        buffer.endBatch();
    }

    // https://stackoverflow.com/questions/27022064/detect-click-in-a-diamond
    public static boolean isPointInsideMinimap(double x, double y) {
        double dx = Math.abs(x - xc);
        double dy = Math.abs(y - yc);
        double d = dx / (MAP_RADIUS * 2) + dy / (MAP_RADIUS * 2);
        return d <= 0.5;
    }

    private static void clickMapToMoveCamera(float x, float y) {
        if (!isPointInsideMinimap(x,y) || CursorClientEvents.isBoxSelecting())
            return;

        float pixelsToBlocks = (float) WORLD_RADIUS / (float) MAP_RADIUS;

        // offset y up so that user clicks the centre of the view quad instead of bottom border
        y += OrthoviewClientEvents.getZoom() * 0.5F / pixelsToBlocks;

        Vec2 clicked = MyMath.rotateCoords(x - xc, y - yc,45);

        double xMoveTo = xc_world + clicked.x * pixelsToBlocks * Math.sqrt(2);
        double zMoveTo = zc_world + clicked.y * pixelsToBlocks * Math.sqrt(2);

        if (MC.player != null)
            PlayerServerboundPacket.teleportPlayer(xMoveTo, MC.player.getY(), zMoveTo);
    }

    // when clicking on map move player there
    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre evt) {
        if (OrthoviewClientEvents.isEnabled() && evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_1)
            clickMapToMoveCamera((float) evt.getMouseX(), (float) evt.getMouseY());
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre evt) {
        if (OrthoviewClientEvents.isEnabled() && evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1)
            clickMapToMoveCamera((float) evt.getMouseX(), (float) evt.getMouseY());
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post evt) {
        if (!OrthoviewClientEvents.isEnabled())
            return;

        refreshTicksCurrent -= 1;
        if (refreshTicksCurrent <= 0) {
            refreshTicksCurrent = REFRESH_TICKS_MAX;
            updateMapColours();
            updateMapTexture();
        }
        renderMap(evt.getPoseStack());

        //MiscUtil.drawDebugStrings(evt.getMatrixStack(), MC.font, new String[] {
        //});
    }
}