package com.solegendary.reignofnether.building;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.buildings.VillagerHouse;
import com.solegendary.reignofnether.building.buildings.VillagerTower;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class BuildingClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    public static Class<? extends Building> buildingToPlace = null;
    private static Class<? extends Building> lastBuildingToPlace = null;
    private static Vec3i sizeToPlace = new Vec3i(0,0,0);
    private static ArrayList<BuildingBlock> blocksToPlace = new ArrayList<>();
    private static ArrayList<BlockState> paletteToPlace = new ArrayList<>();
    private static ArrayList<Pair<BlockPos, Direction>> facesToHighlight = new ArrayList<>();
    private static boolean replacedTexture = false;
    public static boolean initedStructures = false;

    private static float solidBasePercent = 0;

    // TODO: add an option for green overlay
    // adds a green overlay option to OverlayTexture at (0,0)
    public static void replaceOverlayTexture() {
        NativeImage nativeimage = MC.gameRenderer.overlayTexture.texture.getPixels();
        int bgr = MiscUtil.reverseHexRGB(0x00FF00); // for some reason setPixelRGBA reads it as ABGR with A inversed
        nativeimage.setPixelRGBA(0,0, bgr | (0xB2 << 24));
        RenderSystem.activeTexture(33985);
        MC.gameRenderer.overlayTexture.texture.bind();
        nativeimage.upload(0, 0, 0, 0, 0, nativeimage.getWidth(), nativeimage.getHeight(), false, true, false, false);
        RenderSystem.activeTexture(33984);
    }

    // draws the building with a green/red overlay (based on placement validity) at the target position
    // based on whether the location is valid or not
    // location should be 1 space above the selected spot
    public static void drawBuildingToPlace(PoseStack matrix, BlockPos originPos) {
        boolean inAir = isBuildingPlacementInAir(originPos);
        boolean clipping = isBuildingPlacementClipping(originPos);
        boolean invalid = inAir || clipping;

        int minX = 999999;
        int minY = 999999;
        int minZ = 999999;
        int maxX = -999999;
        int maxY = -999999;
        int maxZ = -999999;

        for (BuildingBlock block : blocksToPlace) {
            BlockRenderDispatcher renderer = MC.getBlockRenderer();
            BlockState bs = block.getBlockState(paletteToPlace);
            BlockPos bp = new BlockPos(
                    originPos.getX() + block.blockPos.getX(),
                    originPos.getY() + block.blockPos.getY(),
                    originPos.getZ() + block.blockPos.getZ()
            );
            IModelData modelData = renderer.getBlockModel(bs).getModelData(MC.level, bp, bs, ModelDataManager.getModelData(MC.level, bp));

            matrix.pushPose();
            Entity cam = MC.cameraEntity;
            matrix.translate( // bp is center of block whereas render is corner, so offset by 0.5
                    bp.getX() - cam.getX(),
                    bp.getY() - cam.getY() - 0.6,
                    bp.getZ() - cam.getZ());

            // show red overlay if invalid, else show TODO: green
            renderer.renderSingleBlock(
                    bs, matrix,
                    MC.renderBuffers().crumblingBufferSource(), // don't render over other stuff
                    15728880,
                    // red if invalid, else green
                    invalid ? OverlayTexture.pack(0,3) : OverlayTexture.pack(0,0),
                    modelData);

            matrix.popPose();

            if (bp.getX() < minX) minX = bp.getX();
            if (bp.getY() < minY) minY = bp.getY();
            if (bp.getZ() < minZ) minZ = bp.getZ();
            if (bp.getX() > maxX) maxX = bp.getX();
            if (bp.getY() > maxY) maxY = bp.getY();
            if (bp.getZ() > maxZ) maxZ = bp.getZ();
        }
        // draw placement outline below
        maxX += 1;
        minY += 1.05f;
        maxZ += 1;

        float r = invalid ? 1.0f : 0;
        float g = invalid ? 0 : 1.0f;
        ResourceLocation rl = new ResourceLocation("forge:textures/white.png");
        AABB aabb = new AABB(minX, minY, minZ, maxX, minY, maxZ);
        MyRenderer.drawLineBox(matrix, aabb, r, g, 0,0.5f);
        MyRenderer.drawSolidBox(matrix, aabb, Direction.UP, r, g, 0, 0.5f, rl);
    }

    public static boolean isBuildingPlacementClipping(BlockPos originPos) {
        for (BuildingBlock block : blocksToPlace) {
            Material bm = block.getBlockState(paletteToPlace).getMaterial();
            BlockPos bp = new BlockPos(
                    originPos.getX() + block.blockPos.getX(),
                    originPos.getY() + block.blockPos.getY() + 1,
                    originPos.getZ() + block.blockPos.getZ()
            );
            Material bmWorld = MC.level.getBlockState(bp).getMaterial();
            if ((bmWorld.isSolid() || bmWorld.isLiquid()) && (bm.isSolid() || bm.isLiquid()))
                return true;
        }
        return false;
    }

    // considered to be in the air if < 75% of blocks below placement are solid,
    // excluding those under blocks which aren't solid anyway
    public static boolean isBuildingPlacementInAir(BlockPos originPos) {
        int solidBlocksBelow = 0;
        int blocksBelow = 0;
        for (BuildingBlock block : blocksToPlace) {
            if (block.blockPos.getY() == 0) {
                BlockPos bp = new BlockPos(
                        originPos.getX() + block.blockPos.getX(),
                        originPos.getY() + block.blockPos.getY() + 1,
                        originPos.getZ() + block.blockPos.getZ()
                );
                Material bm = MC.level.getBlockState(bp).getMaterial();
                Material bmBelow = MC.level.getBlockState(bp.below()).getMaterial();
                if (bm.isSolid()) {
                    blocksBelow += 1;
                    if (bmBelow.isSolid())
                        solidBlocksBelow += 1;
                }

            }
        }
        solidBasePercent = (float) solidBlocksBelow / (float) blocksBelow;
        return ((float) solidBlocksBelow / (float) blocksBelow) < 0.75f;
    }

    public static void placeBuilding() {

    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelLastEvent evt) {


        if (!OrthoviewClientEvents.isEnabled())
            return;

        if (buildingToPlace != null && initedStructures) {
            drawBuildingToPlace(evt.getPoseStack(), CursorClientEvents.getPreselectedBlockPos());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (!replacedTexture) {
            replaceOverlayTexture();
            replacedTexture = true;
        }
        if (!initedStructures) {
            BuildingBlockData.initBlockData(MC);
            initedStructures = true;
        }
    }

    @SubscribeEvent
    public static void onInput(InputEvent.KeyInputEvent evt) {

        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions

            if (evt.getKey() == Keybinds.fnums[6].getKey().getValue()) {
                buildingToPlace = VillagerHouse.class;
            }
            else if (evt.getKey() == Keybinds.fnums[7].getKey().getValue()) {
                buildingToPlace = VillagerTower.class;
            }
            else if (evt.getKey() == Keybinds.fnums[8].getKey().getValue()) {
                buildingToPlace = null;
            }

            if (buildingToPlace != lastBuildingToPlace && buildingToPlace != null) {
                // load the new buildingToPlace's data
                try {
                    Method getBlockData = buildingToPlace.getMethod("getBlockData");
                    blocksToPlace = (ArrayList<BuildingBlock>) getBlockData.invoke(null);
                    Method getPaletteData = buildingToPlace.getMethod("getPaletteData");
                    paletteToPlace = (ArrayList<BlockState>) getPaletteData.invoke(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sizeToPlace = Building.getBuildingSize(blocksToPlace);
                System.out.println(sizeToPlace.getX() + "|" + sizeToPlace.getY() + "|" + sizeToPlace.getZ());


                // load faces to highlight
                facesToHighlight = new ArrayList<>();
                for (BuildingBlock block : blocksToPlace) {

                    // TODO: only highlight faces on the outside of the building
                    // maybe we can do this by getting a list of BPs for each of the 5 directions
                    // where each list contains only the first block found as each side is scanned inwards

                    if (!block.getBlockState(paletteToPlace).isAir()) {

                        BlockPos bp = block.blockPos;
                        BuildingBlock west = BuildingBlockData.getBuildingBlockByPos(blocksToPlace, bp.west());
                        BuildingBlock north = BuildingBlockData.getBuildingBlockByPos(blocksToPlace, bp.north());
                        BuildingBlock east = BuildingBlockData.getBuildingBlockByPos(blocksToPlace, bp.east());
                        BuildingBlock above = BuildingBlockData.getBuildingBlockByPos(blocksToPlace, bp.above());
                        BuildingBlock south = BuildingBlockData.getBuildingBlockByPos(blocksToPlace, bp.south());

                        if (west == null || west.getBlockState(paletteToPlace).isAir())
                            facesToHighlight.add(new Pair(bp, Direction.WEST));
                        if (north == null || north.getBlockState(paletteToPlace).isAir())
                            facesToHighlight.add(new Pair(bp, Direction.NORTH));
                        if (east == null || east.getBlockState(paletteToPlace).isAir())
                            facesToHighlight.add(new Pair(bp, Direction.EAST));
                        if (above == null || above.getBlockState(paletteToPlace).isAir())
                            facesToHighlight.add(new Pair(bp, Direction.UP));
                        if (south == null || south.getBlockState(paletteToPlace).isAir())
                            facesToHighlight.add(new Pair(bp, Direction.SOUTH));
                    }
                }

            }
            lastBuildingToPlace = buildingToPlace;
        }
        if (evt.getAction() == GLFW.GLFW_MOUSE_BUTTON_1 && buildingToPlace != null) {

        }
    }

    @SubscribeEvent
    public static void onRenderOverLay(RenderGameOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getMatrixStack(), MC.font, new String[] {
                "solidBasePercent: " + solidBasePercent
        });
    }
}
