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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.InputEvent;
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

    public static Boolean initedStructures = false;

    public static Class<? extends Building> lastBuildingToPlace = null;
    public static Class<? extends Building> buildingToPlace = null;
    private static Vec3i sizeToPlace = new Vec3i(0,0,0);
    private static ArrayList<BuildingBlock> blocksToPlace = new ArrayList<>();
    private static ArrayList<BlockState> paletteToPlace = new ArrayList<>();
    private static ArrayList<Pair<BlockPos, Direction>> facesToHighlight = new ArrayList<>();

    private static int overlayU = 0;
    private static int overlayV = 0;

    private static boolean replacedTexture = false;

    // TODO: add an option for green overlay
    public static void replaceOverlayTexture() {
        NativeImage nativeimage = MC.gameRenderer.overlayTexture.texture.getPixels();

        for(int i = 0; i < 16; ++i) {
            for(int j = 0; j < 16; ++j) {
                if (i < 8) {
                    nativeimage.setPixelRGBA(j, i,0);
                } else {
                    int k = (int)((1.0F - (float)j / 15.0F * 0.75F) * 255.0F);
                    nativeimage.setPixelRGBA(j, i,0);
                }
            }
        }
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
            matrix.translate(
                    bp.getX() - cam.getX(),
                    bp.getY() - cam.getY(),
                    bp.getZ() - cam.getZ());

            // show red overlay if invalid, else show TODO: green
            renderer.renderSingleBlock(
                    bs, matrix,
                    MC.renderBuffers().crumblingBufferSource(), // don't render over other stuff
                    15728880,
                    invalid ? OverlayTexture.pack(8, 10) : OverlayTexture.pack(0,3),
                    modelData);

            matrix.popPose();
        }
    }

    public static boolean isBuildingPlacementClipping(BlockPos originPos) {
        for (BuildingBlock block : blocksToPlace) {
            BlockState bs = block.getBlockState(paletteToPlace);
            BlockPos bp = new BlockPos(
                    originPos.getX() + block.blockPos.getX(),
                    originPos.getY() + block.blockPos.getY() + 1,
                    originPos.getZ() + block.blockPos.getZ()
            );
            BlockState bsWorld = MC.level.getBlockState(bp);
            if (!bsWorld.isAir() && !bs.isAir())
                return false;
        }
        return true;
    }

    // TODO: blocks below placement must be > 50% solid
    public static boolean isBuildingPlacementInAir(BlockPos originPos) {
        return false;
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
            else if (evt.getKey() == Keybinds.nums[9].getKey().getValue()) {
                overlayU += 1;
                if (overlayU > 16)
                    overlayU = 0;
                System.out.println(overlayU);
            }
            else if (evt.getKey() == Keybinds.nums[0].getKey().getValue()) {
                overlayV += 1;
                if (overlayV > 16)
                    overlayV = 0;
                System.out.println(overlayV);
            }

            if (lastBuildingToPlace != buildingToPlace) {
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
}
