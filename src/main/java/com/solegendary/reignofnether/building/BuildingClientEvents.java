package com.solegendary.reignofnether.building;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.building.buildings.VillagerHouse;
import com.solegendary.reignofnether.building.buildings.VillagerTower;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.Unit;
import com.solegendary.reignofnether.unit.UnitClientEvents;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuildingClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    private static int totalPopulationSupply = 0;
    public static int getTotalPopulationSupply() {
        return Math.min(ResourceCosts.MAX_POPULATION, totalPopulationSupply);
    }

    // clientside buildings used for tracking position (for cursor selection)
    private static List<Building> buildings = Collections.synchronizedList(new ArrayList<>());

    private static Building selectedBuilding = null;
    private static Class<? extends Building> buildingToPlace = null;
    private static Class<? extends Building> lastBuildingToPlace = null;
    private static ArrayList<BuildingBlock> blocksToDraw = new ArrayList<>();
    private static boolean replacedTexture = false;
    private static Rotation buildingRotation = Rotation.NONE;
    private static Vec3i buildingDimensions = new Vec3i(0,0,0);

    public static Building getSelectedBuilding() { return selectedBuilding; }
    public static void setSelectedBuilding(Building building) { selectedBuilding = building; }
    public static void setBuildingToPlace(Class<? extends Building> building) {
        buildingToPlace = building;

        if (buildingToPlace != lastBuildingToPlace && buildingToPlace != null) {
            // load the new buildingToPlace's data
            try {
                Class<?>[] paramTypes = { LevelAccessor.class };
                Method getRelativeBlockData = buildingToPlace.getMethod("getRelativeBlockData", paramTypes);
                blocksToDraw = (ArrayList<BuildingBlock>) getRelativeBlockData.invoke(null, MC.level);
                buildingDimensions = BuildingUtils.getBuildingSize(blocksToDraw);
                buildingRotation = Rotation.NONE;
            } catch (Exception e) {
                e.printStackTrace();
            }
            lastBuildingToPlace = buildingToPlace; // avoid loading the same data twice unnecessarily
        }
    }
    public static Class<? extends Building> getBuildingToPlace() { return buildingToPlace; }

    public static List<Building> getBuildings() {
        return buildings;
    }

    // adds a green overlay option to OverlayTexture at (0,0)
    public static void replaceOverlayTexture() {
        NativeImage nativeimage = MC.gameRenderer.overlayTexture.texture.getPixels();
        int bgr = MiscUtil.reverseHexRGB(0x00FF00); // for some reason setPixelRGBA reads it as ABGR with A inversed
        if (nativeimage != null) {
            nativeimage.setPixelRGBA(0,0, bgr | (0xB2 << 24));
            RenderSystem.activeTexture(33985);
            MC.gameRenderer.overlayTexture.texture.bind();
            nativeimage.upload(0, 0, 0, 0, 0, nativeimage.getWidth(), nativeimage.getHeight(), false, true, false, false);
            RenderSystem.activeTexture(33984);
        }
    }

    public static Building getPreselectedBuilding() {
        for (Building building: buildings) {
            if (building.isPosInsideBuilding(CursorClientEvents.getPreselectedBlockPos()))
                return building;
        }
        return null;
    }

    // draws the building with a green/red overlay (based on placement validity) at the target position
    // based on whether the location is valid or not
    // location should be 1 space above the selected spot
    public static void drawBuildingToPlace(PoseStack matrix, BlockPos originPos) {
        boolean valid = isBuildingPlacementValid(originPos);

        int minX = 999999;
        int minY = 999999;
        int minZ = 999999;
        int maxX = -999999;
        int maxY = -999999;
        int maxZ = -999999;

        for (BuildingBlock block : blocksToDraw) {
            BlockRenderDispatcher renderer = MC.getBlockRenderer();
            BlockState bs = block.getBlockState();
            BlockPos bp = new BlockPos(
                    originPos.getX() + block.getBlockPos().getX(),
                    originPos.getY() + block.getBlockPos().getY(),
                    originPos.getZ() + block.getBlockPos().getZ()
            );
            // ModelData modelData = renderer.getBlockModel(bs).getModelData(MC.level, bp, bs, ModelDataManager.getModelData(MC.level, bp));

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
                    valid ? OverlayTexture.pack(0,0) : OverlayTexture.pack(0,3));

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

        float r = valid ? 0 : 1.0f;
        float g = valid ? 1.0f : 0;
        ResourceLocation rl = new ResourceLocation("forge:textures/white.png");
        AABB aabb = new AABB(minX, minY, minZ, maxX, minY, maxZ);
        MyRenderer.drawLineBox(matrix, aabb, r, g, 0,0.5f);
        MyRenderer.drawSolidBox(matrix, aabb, Direction.UP, r, g, 0, 0.5f, rl);
        AABB aabb2 = new AABB(minX, 0, minZ, maxX, minY, maxZ);
        MyRenderer.drawLineBox(matrix, aabb2, r, g, 0,0.25f);
    }

    public static boolean isBuildingPlacementValid(BlockPos originPos) {
        boolean inAir = isBuildingPlacementInAir(originPos);
        boolean clipping = isBuildingPlacementClipping(originPos);
        return !inAir && !clipping;
    }

    public static boolean isBuildingPlacementClipping(BlockPos originPos) {
        for (BuildingBlock block : blocksToDraw) {
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
        for (BuildingBlock block : blocksToDraw) {
            if (block.getBlockPos().getY() == 0 && MC.level != null) {
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

    // gets the cursor position rotated according to the preselected building
    private static BlockPos getOriginPos() {
        int xAdj = 0;
        int zAdj = 0;
        int xRadius = buildingDimensions.getX() / 2;
        int zRadius = buildingDimensions.getZ() / 2;

        switch(buildingRotation) {
            case NONE                -> { xAdj = -xRadius; zAdj = -zRadius; }
            case CLOCKWISE_90        -> { xAdj =  xRadius; zAdj = -zRadius; }
            case CLOCKWISE_180       -> { xAdj =  xRadius; zAdj =  zRadius; }
            case COUNTERCLOCKWISE_90 -> { xAdj = -xRadius; zAdj =  zRadius; }
        }
        return CursorClientEvents.getPreselectedBlockPos().offset(xAdj, 0, zAdj);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        if (!OrthoviewClientEvents.isEnabled())
            return;

        if (buildingToPlace != null)
            drawBuildingToPlace(evt.getPoseStack(), getOriginPos());

        Building preselectedBuilding = getPreselectedBuilding();

        totalPopulationSupply = 0;

        for (Building building : buildings) {
            AABB aabb = new AABB(
                    new BlockPos(BuildingUtils.getMinCorner(building.blocks)),
                    new BlockPos(BuildingUtils.getMaxCorner(building.blocks)).offset(1,1,1)
            );

            if (building.equals(selectedBuilding))
                MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 1.0f, 1.0f, 1.0f, 1.0f);
            else if (building.equals(preselectedBuilding) && !HudClientEvents.isMouseOverAnyButtonOrHud()) {
                if (HudClientEvents.hudSelectedEntity instanceof Unit &&
                    ((Unit) HudClientEvents.hudSelectedEntity).canBuildAndRepair() &&
                    MiscUtil.isRightClickDown(MC))
                    MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 1.0f, 1.0f, 1.0f, 1.0f);
                else
                    MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 1.0f, 1.0f, 1.0f, 0.5f);
            }


            Relationship buildingRs = getPlayerToBuildingRelationship(building);

            if (buildingRs == Relationship.OWNED && building.isBuilt)
                totalPopulationSupply += building.popSupply;

            switch (buildingRs) {
                case OWNED -> MyRenderer.drawOutlineBottom(evt.getPoseStack(), aabb, 0.3f, 1.0f, 0.3f, 0.2f);
                case FRIENDLY -> MyRenderer.drawOutlineBottom(evt.getPoseStack(), aabb, 0.3f, 0.3f, 1.0f, 0.2f);
                case HOSTILE -> MyRenderer.drawOutlineBottom(evt.getPoseStack(), aabb, 1.0f, 0.3f, 0.3f, 0.2f);
            }
        }

        // draw rally point and line
        if (selectedBuilding instanceof ProductionBuilding selProdBuilding && selProdBuilding.getRallyPoint() != null) {
            float a = MiscUtil.getOscillatingFloat(0.25f,0.75f);
            MyRenderer.drawBlockFace(evt.getPoseStack(),
                    Direction.UP,
                    selProdBuilding.getRallyPoint(),
                    0, 1, 0, a);
            MyRenderer.drawLine(evt.getPoseStack(),
                    BuildingUtils.getCentrePos(selProdBuilding.getBlocks()).offset(0,-1,0),
                    selProdBuilding.getRallyPoint(),
                    0, 1, 0, a);
        }
    }

    @SubscribeEvent
    public static void onInput(InputEvent.Key evt) {
        if (!OrthoviewClientEvents.isEnabled())
            return;

        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions

            if (evt.getKey() == Keybinding.getFnum(6).getKey().getValue())
                setBuildingToPlace(VillagerHouse.class);
            else if (evt.getKey() == Keybinding.getFnum(7).getKey().getValue())
                setBuildingToPlace(VillagerTower.class);
            else if (evt.getKey() == Keybinding.getFnum(8).getKey().getValue())
                setBuildingToPlace(null);
        }
    }

    // on scroll rotate the building placement by 90deg by resorting the blocks list
    // for some reason this event is run twice every scroll
    private static boolean secondScrollEvt = true;
    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrolled evt) {
        secondScrollEvt = !secondScrollEvt;
        if (!secondScrollEvt) return;

        if (buildingToPlace != null) {
            Rotation rotation = evt.getScrollDelta() > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
            buildingRotation = buildingRotation.getRotated(rotation);
            for (int i = 0; i < blocksToDraw.size(); i++)
                blocksToDraw.set(i, blocksToDraw.get(i).rotate(MC.level, rotation));
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre evt) throws NoSuchFieldException, IllegalAccessException {
        if (!OrthoviewClientEvents.isEnabled())
            return;

        // prevent clicking behind HUDs
        if (HudClientEvents.isMouseOverAnyButtonOrHud()) {
            setBuildingToPlace(null);
            return;
        }

        BlockPos pos = getOriginPos();
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {

            // place a new building
            if (buildingToPlace != null && isBuildingPlacementValid(pos) && MC.player != null) {
                String buildingName = (String) buildingToPlace.getField("buildingName").get(null);

                ArrayList<Integer> builderIds = new ArrayList<>();
                for (LivingEntity builderEntity : UnitClientEvents.getSelectedUnits())
                    if (builderEntity instanceof Unit && ((Unit) builderEntity).canBuildAndRepair())
                        builderIds.add(builderEntity.getId());

                BuildingServerboundPacket.placeBuilding(buildingName, pos, buildingRotation, MC.player.getName().getString(),
                        builderIds.stream().mapToInt(i -> i).toArray());
                setBuildingToPlace(null);
            }
            else if (buildingToPlace == null) {
                Building building = getPreselectedBuilding();
                if (building != null && CursorClientEvents.getLeftClickAction() == null) {
                    selectedBuilding = building;
                    UnitClientEvents.setSelectedUnitIds(new ArrayList<>());
                }
            }
        }
        else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            // set rally points
            if (!Keybinding.altMod.isDown() &&
                selectedBuilding instanceof ProductionBuilding selProdBuilding) {

                BlockPos rallyPoint = CursorClientEvents.getPreselectedBlockPos();
                selProdBuilding.setRallyPoint(rallyPoint);
                BuildingServerboundPacket.setRallyPoint(
                        BuildingUtils.getMinCorner(selectedBuilding.blocks),
                        rallyPoint
                );
            }
            else {
                setBuildingToPlace(null);
            }
        }
    }

    // place a building clientside that has already been registered on serverside
    public static void placeBuilding(String buildingName, BlockPos pos, Rotation rotation, String ownerName) {
        Building building = BuildingUtils.getNewBuilding(buildingName, MC.level, pos, rotation, ownerName);
        if (building != null)
            buildings.add(building);
    }

    public static void destroyBuilding(BlockPos pos) {
        buildings.removeIf(building -> building.isPosInsideBuilding(pos));
    }

    public static Relationship getPlayerToBuildingRelationship(Building building) {
        if (MC.player != null && building.ownerName.equals(MC.player.getName().getString()))
            return Relationship.OWNED;
        else
            return Relationship.HOSTILE;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (!replacedTexture) {
            replaceOverlayTexture();
            replacedTexture = true;
        }

        if (MC.level != null && MC.level.dimension() == Level.OVERWORLD && evt.phase == TickEvent.Phase.END) {

            for (Building building : buildings)
                building.tick(MC.level);

            // cleanup destroyed buildings
            if (selectedBuilding != null && selectedBuilding.shouldBeDestroyed())
                selectedBuilding = null;
            buildings.removeIf(Building::shouldBeDestroyed);
        }
    }

    /*
    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
                "xn: " + xn,
                "yn: " + yn,
                "zn: " + zn,
        });
    }*/
}
