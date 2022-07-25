package com.solegendary.reignofnether.building;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class BuildingClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    public static Class<? extends Building> buildingToPlace = null;
    private static Class<? extends Building> lastBuildingToPlace = null;
    private static ArrayList<BuildingBlock> blocksToPlace = new ArrayList<>();
    private static boolean replacedTexture = false;
    private static Rotation buildingRotation = Rotation.NONE;
    private static Vec3i buildingDimensions = new Vec3i(0,0,0);

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
            BlockState bs = block.getBlockState();
            BlockPos bp = new BlockPos(
                    originPos.getX() + block.getBlockPos().getX(),
                    originPos.getY() + block.getBlockPos().getY(),
                    originPos.getZ() + block.getBlockPos().getZ()
            );
            IModelData modelData = renderer.getBlockModel(bs).getModelData(MC.level, bp, bs, ModelDataManager.getModelData(MC.level, bp));

            matrix.pushPose();
            Entity cam = MC.cameraEntity;
            matrix.translate( // bp is center of block whereas render is corner, so offset by 0.5
                    bp.getX() - cam.getX(),
                    bp.getY() - cam.getY() - 0.6,
                    bp.getZ() - cam.getZ());

            // show red overlay if invalid, else show green
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
        AABB aabb2 = new AABB(minX, 0, minZ, maxX, minY, maxZ);
        MyRenderer.drawLineBox(matrix, aabb2, r, g, 0,0.25f);
    }

    public static boolean isBuildingPlacementClipping(BlockPos originPos) {
        for (BuildingBlock block : blocksToPlace) {
            Material bm = block.getBlockState().getMaterial();
            BlockPos bp = new BlockPos(
                    originPos.getX() + block.getBlockPos().getX(),
                    originPos.getY() + block.getBlockPos().getY() + 1,
                    originPos.getZ() + block.getBlockPos().getZ()
            );
            Material bmWorld = MC.level.getBlockState(bp).getMaterial();
            if ((bmWorld.isSolid() || bmWorld.isLiquid()) && (bm.isSolid() || bm.isLiquid()))
                return true;
        }
        return false;
    }

    // 90% all solid blocks at the base of the building must be on top of solid blocks to be placeable
    // excluding those under blocks which aren't solid anyway
    public static boolean isBuildingPlacementInAir(BlockPos originPos) {
        int solidBlocksBelow = 0;
        int blocksBelow = 0;
        for (BuildingBlock block : blocksToPlace) {
            if (block.getBlockPos().getY() == 0) {
                BlockPos bp = new BlockPos(
                        originPos.getX() + block.getBlockPos().getX(),
                        originPos.getY() + block.getBlockPos().getY() + 1,
                        originPos.getZ() + block.getBlockPos().getZ()
                );
                BlockState bs = block.getBlockState(); // building block
                BlockState bsBelow = MC.level.getBlockState(bp.below()); // world block

                if (bs.getMaterial().isSolid()) {
                    blocksBelow += 1;
                    if (bsBelow.getMaterial().isSolid())
                        solidBlocksBelow += 1;
                }
            }
        }
        if (blocksBelow <= 0) return false; // avoid division by 0
        return ((float) solidBlocksBelow / (float) blocksBelow) < 0.9f;
    }

    // sends off the packet to server to create the Building object and start construction
    public static void placeBuilding() {

    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelLastEvent evt) {
        if (!OrthoviewClientEvents.isEnabled())
            return;

        if (buildingToPlace != null) {
            int xAdj = 0;
            int zAdj = 0;
            int xRadius = buildingDimensions.getX() / 2;
            int zRadius = buildingDimensions.getZ() / 2;

            switch(buildingRotation) {
                case NONE:                xAdj = -xRadius; zAdj = -zRadius; break;
                case CLOCKWISE_90:        xAdj =  xRadius; zAdj = -zRadius; break;
                case CLOCKWISE_180:       xAdj =  xRadius; zAdj =  zRadius; break;
                case COUNTERCLOCKWISE_90: xAdj = -xRadius; zAdj =  zRadius; break;
            }
            BlockPos centredBp = CursorClientEvents.getPreselectedBlockPos().offset(xAdj, 0, zAdj);
            drawBuildingToPlace(evt.getPoseStack(), centredBp);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (!replacedTexture) {
            replaceOverlayTexture();
            replacedTexture = true;
        }
    }

    @SubscribeEvent
    public static void onInput(InputEvent.KeyInputEvent evt) {
        if (!OrthoviewClientEvents.isEnabled())
            return;

        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions

            if (evt.getKey() == Keybinds.fnums[6].getKey().getValue())
                buildingToPlace = VillagerHouse.class;
            else if (evt.getKey() == Keybinds.fnums[7].getKey().getValue())
                buildingToPlace = VillagerTower.class;
            else if (evt.getKey() == Keybinds.fnums[8].getKey().getValue())
                buildingToPlace = null;

            if (buildingToPlace != lastBuildingToPlace && buildingToPlace != null) {
                // load the new buildingToPlace's data
                try {
                    Method getStaticBlockData = buildingToPlace.getMethod("getStaticBlockData");
                    blocksToPlace = (ArrayList<BuildingBlock>) getStaticBlockData.invoke(null);
                    buildingDimensions = Building.getBuildingSize(blocksToPlace);
                    System.out.println(buildingDimensions);
                    buildingRotation = Rotation.NONE;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            lastBuildingToPlace = buildingToPlace;
        }
        if (evt.getAction() == GLFW.GLFW_MOUSE_BUTTON_1 && buildingToPlace != null) {

        }
    }

    // on scroll rotate the building placement by 90deg by resorting the blocks list
    // for some reason this event is run twice every scroll
    private static boolean secondScrollEvt = true;
    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrollEvent evt) {
        secondScrollEvt = !secondScrollEvt;
        if (!secondScrollEvt) return;

        if (buildingToPlace != null) {
            Rotation rotation = evt.getScrollDelta() > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
            buildingRotation = buildingRotation.getRotated(rotation);
            for (int i = 0; i < blocksToPlace.size(); i++)
                blocksToPlace.set(i, blocksToPlace.get(i).rotate(MC.level, rotation));
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseClickedEvent.Pre evt) throws NoSuchFieldException, IllegalAccessException {
        if (!OrthoviewClientEvents.isEnabled())
            return;

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1 && buildingToPlace != null) {
            String buildingName = (String) buildingToPlace.getField("buildingName").get(null);
            BuildingServerboundPacket.placeBuilding(buildingName, CursorClientEvents.getPreselectedBlockPos(), buildingRotation);
            buildingToPlace = null;
        }
    }

    @SubscribeEvent
    public static void onRenderOverLay(RenderGameOverlayEvent.Pre evt) {
        /*
        MiscUtil.drawDebugStrings(evt.getMatrixStack(), MC.font, new String[] {
                "solidBlocksBelow: " + solidBlocksBelow1,
                "blocksBelow: " + blocksBelow1
        });*/
    }
}
