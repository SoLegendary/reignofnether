package com.solegendary.reignofnether.building;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.solegendary.reignofnether.building.buildings.piglins.Portal;
import com.solegendary.reignofnether.building.buildings.shared.Stockpile;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.nether.NetherBlocks;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.researchItems.ResearchAdvancedPortals;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.goals.BuildRepairGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.Faction;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.solegendary.reignofnether.hud.HudClientEvents.hudSelectedBuilding;
import static com.solegendary.reignofnether.hud.HudClientEvents.hudSelectedEntity;
import static com.solegendary.reignofnether.unit.UnitClientEvents.getSelectedUnits;

public class BuildingClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    public static int getTotalPopulationSupply(String playerName) {
        int totalPopulationSupply = 0;
        for (Building building : buildings)
            if (building.ownerName.equals(playerName) && building.isBuilt)
                totalPopulationSupply += building.popSupply;

        return Math.min(ResourceCosts.MAX_POPULATION, totalPopulationSupply);
    }
    // clientside buildings used for tracking position (for cursor selection)
    private static final ArrayList<Building> buildings = new ArrayList<>();

    private static ArrayList<Building> selectedBuildings = new ArrayList<>();
    private static Class<? extends Building> buildingToPlace = null;
    private static Class<? extends Building> lastBuildingToPlace = null;
    private static ArrayList<BuildingBlock> blocksToDraw = new ArrayList<>();
    private static boolean replacedTexture = false;
    private static Rotation buildingRotation = Rotation.NONE;
    private static Vec3i buildingDimensions = new Vec3i(0,0,0);

    private static long lastLeftClickTime = 0; // to track double clicks
    private static final long DOUBLE_CLICK_TIME_MS = 500;

    // minimum % of blocks below a building that need to be supported by a solid block for it to be placeable
    // 1 means you can't have any gaps at all, 0 means you can place buildings in mid-air
    private static final float MIN_SUPPORTED_BLOCKS_PERCENT = 0.6f;

    private static final float MIN_NETHER_BLOCKS_PERCENT = 0.8f;

    // can only be one preselected building as you can't box-select them like units
    public static Building getPreselectedBuilding() {
        for (Building building: buildings)
            if (building.isPosInsideBuilding(CursorClientEvents.getPreselectedBlockPos()))
                return building;
        return null;
    }
    public static ArrayList<Building> getSelectedBuildings() { return selectedBuildings; }
    public static List<Building> getBuildings() {
        return buildings;
    }

    public static void clearSelectedBuildings() {
        selectedBuildings.clear();
    }
    public static void addSelectedBuilding(Building building) {
        CursorClientEvents.setLeftClickAction(null);

        if (!FogOfWarClientEvents.isBuildingInBrightChunk(building))
            return;

        selectedBuildings.add(building);
        selectedBuildings.sort(Comparator.comparing(b -> b.name));
        UnitClientEvents.clearSelectedUnits();
    }

    // switch to the building with the least production, so we can spread out production items
    public static void switchHudToIdlestBuilding() {
        Building idlestBuilding = null;
        int prodTicksLeftMax = Integer.MAX_VALUE;
        if (selectedBuildings.size() > 0) {
            for (Building building : selectedBuildings) {
                if (building instanceof ProductionBuilding prodB) {
                    int prodTicksLeft = prodB.productionQueue.stream().map(p -> p.ticksLeft).reduce(0, Integer::sum);
                    if (prodTicksLeft < prodTicksLeftMax) {
                        prodTicksLeftMax = prodTicksLeft;
                        idlestBuilding = building;
                    }
                }
            }
        }
        if (idlestBuilding != null)
            hudSelectedBuilding = idlestBuilding;
    }

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

    // draws the building with a green/red overlay (based on placement validity) at the target position
    // based on whether the location is valid or not
    // location should be 1 space above the selected spot
    public static void drawBuildingToPlace(PoseStack matrix, BlockPos originPos) throws NoSuchFieldException {
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

            renderer.renderSingleBlock(
                    bs, matrix,
                    MC.renderBuffers().crumblingBufferSource(), // don't render over other stuff
                    15728880,
                    // red if invalid, else green
                    valid ? OverlayTexture.pack(0,0) : OverlayTexture.pack(0,3),
                    net.minecraftforge.client.model.data.ModelData.EMPTY, null
            );

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
        return !isBuildingPlacementInAir(originPos) &&
               !isBuildingPlacementClipping(originPos) &&
               !isOverlappingAnyOtherBuilding() &&
               isNonPiglinOrOnNetherBlocks(originPos) &&
                FogOfWarClientEvents.isInBrightChunk(originPos);
    }

    // disallow any building block from clipping into any other existing blocks
    private static boolean isBuildingPlacementClipping(BlockPos originPos) {
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
    private static boolean isBuildingPlacementInAir(BlockPos originPos) {
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

                if (bs.getMaterial().isSolid() && !(bsBelow.getBlock() instanceof IceBlock)) {
                    blocksBelow += 1;
                    if (bsBelow.getMaterial().isSolid())
                        solidBlocksBelow += 1;
                }
            }
        }
        if (blocksBelow <= 0) return false; // avoid division by 0
        return ((float) solidBlocksBelow / (float) blocksBelow) < MIN_SUPPORTED_BLOCKS_PERCENT;
    }

    // disallow the building borders from overlapping any other's, even if they don't collide physical blocks
    // also allow for a 1 block gap between buildings so units can spawn and stairs don't have their blockstates messed up
    private static boolean isOverlappingAnyOtherBuilding() {

        BlockPos origin = getOriginPos();
        Vec3i originOffset = new Vec3i(origin.getX(), origin.getY(), origin.getZ());
        BlockPos minPos = BuildingUtils.getMinCorner(blocksToDraw).offset(originOffset).offset(-1,-1,-1);
        BlockPos maxPos = BuildingUtils.getMaxCorner(blocksToDraw).offset(originOffset).offset(1,1,1);

        for (Building building : buildings) {
            for (BuildingBlock block : building.blocks) {
                BlockPos bp = block.getBlockPos();
                if (bp.getX() >= minPos.getX() && bp.getX() <= maxPos.getX() &&
                    bp.getY() >= minPos.getY() && bp.getY() <= maxPos.getY() &&
                    bp.getZ() >= minPos.getZ() && bp.getZ() <= maxPos.getZ())
                    return true;
            }
        }
        return false;
    }

    private static boolean isNonPiglinOrOnNetherBlocks(BlockPos originPos) {
        String buildingName = buildingToPlace.getName().toLowerCase();
        if (!buildingName.contains("buildings.piglins.") || buildingName.contains("centralportal"))
            return true;
        if (buildingName.contains("portal") && ResearchClient.hasResearch(ResearchAdvancedPortals.itemName))
            return true;

        int netherBlocksBelow = 0;
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
                    if (NetherBlocks.isNetherBlock(MC.level, bp.below()))
                        netherBlocksBelow += 1;
                }
            }
        }
        if (blocksBelow <= 0) return false; // avoid division by 0
        return ((float) netherBlocksBelow / (float) blocksBelow) > MIN_NETHER_BLOCKS_PERCENT;
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
    public static void onRenderLevel(RenderLevelStageEvent evt) throws NoSuchFieldException {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        if (!OrthoviewClientEvents.isEnabled())
            return;

        if (buildingToPlace != null)
            drawBuildingToPlace(evt.getPoseStack(), getOriginPos());

        Building preselectedBuilding = getPreselectedBuilding();

        for (Building building : buildings) {

            boolean isInBrightChunk = FogOfWarClientEvents.isBuildingInBrightChunk(building);

            AABB aabb = new AABB(
                building.minCorner,
                building.maxCorner.offset(1,1,1)
            );

            if (isInBrightChunk) {
                if (selectedBuildings.contains(building))
                    MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 1.0f, 1.0f, 1.0f, 1.0f);
                else if (building.equals(preselectedBuilding) && !HudClientEvents.isMouseOverAnyButtonOrHud()) {
                    if (hudSelectedEntity instanceof WorkerUnit &&
                            MiscUtil.isRightClickDown(MC))
                        MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 1.0f, 1.0f, 1.0f, 1.0f);
                    else
                        MyRenderer.drawLineBox(evt.getPoseStack(), aabb, 1.0f, 1.0f, 1.0f, MiscUtil.isRightClickDown(MC) ? 1.0f : 0.5f);
                }
            }

            Relationship buildingRs = getPlayerToBuildingRelationship(building);



            if (isInBrightChunk) {
                switch (buildingRs) {
                    case OWNED -> MyRenderer.drawBoxBottom(evt.getPoseStack(), aabb, 0.3f, 1.0f, 0.3f, 0.2f);
                    case FRIENDLY -> MyRenderer.drawBoxBottom(evt.getPoseStack(), aabb, 0.3f, 0.3f, 1.0f, 0.2f);
                    case HOSTILE -> MyRenderer.drawBoxBottom(evt.getPoseStack(), aabb, 1.0f, 0.3f, 0.3f, 0.2f);
                }
            }
        }

        // draw rally point and line
        for (Building selBuilding : selectedBuildings) {
            if (selBuilding instanceof ProductionBuilding selProdBuilding && selProdBuilding.getRallyPoint() != null) {
                float a = MiscUtil.getOscillatingFloat(0.25f,0.75f);
                MyRenderer.drawBlockFace(evt.getPoseStack(),
                        Direction.UP,
                        selProdBuilding.getRallyPoint(),
                        0, 1, 0, a);
                MyRenderer.drawLine(evt.getPoseStack(),
                        selProdBuilding.centrePos,
                        selProdBuilding.getRallyPoint(),
                        0, 1, 0, a);
            }
            if (selBuilding instanceof Portal portal && portal.destination != null) {
                float a = MiscUtil.getOscillatingFloat(0.25f,0.75f);
                MyRenderer.drawLine(evt.getPoseStack(),
                        portal.centrePos,
                        portal.destination,
                        0, 1, 0, a);
            }
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
            blocksToDraw.replaceAll(buildingBlock -> buildingBlock.rotate(MC.level, rotation));
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
            Building preSelBuilding = getPreselectedBuilding();

            // place a new building
            if (buildingToPlace != null && isBuildingPlacementValid(pos) && MC.player != null) {
                String buildingName = (String) buildingToPlace.getField("buildingName").get(null);

                ArrayList<Integer> builderIds = new ArrayList<>();
                for (LivingEntity builderEntity : getSelectedUnits())
                    if (builderEntity instanceof WorkerUnit)
                        builderIds.add(builderEntity.getId());

                if (Keybindings.shiftMod.isDown()) {
                    BuildingServerboundPacket.placeAndQueueBuilding(buildingName, pos, buildingRotation, MC.player.getName().getString(),
                            builderIds.stream().mapToInt(i -> i).toArray());

                    for (LivingEntity entity : getSelectedUnits()) {
                        if (entity instanceof Unit unit) {
                            unit.getCheckpoints().removeIf(bp -> !BuildingUtils.isPosInsideAnyBuilding(true, bp));
                            MiscUtil.addUnitCheckpoint(unit, CursorClientEvents.getPreselectedBlockPos().above(), false);
                            if (unit instanceof WorkerUnit workerUnit)
                                workerUnit.getBuildRepairGoal().ignoreNextCheckpoint = true;
                        }
                    }
                } else {
                    BuildingServerboundPacket.placeBuilding(buildingName, pos, buildingRotation, MC.player.getName().getString(),
                            builderIds.stream().mapToInt(i -> i).toArray());
                    setBuildingToPlace(null);

                    for (LivingEntity entity : getSelectedUnits()) {
                        if (entity instanceof Unit unit) {
                            MiscUtil.addUnitCheckpoint(unit, CursorClientEvents.getPreselectedBlockPos().above());
                            if (unit instanceof WorkerUnit workerUnit)
                                workerUnit.getBuildRepairGoal().ignoreNextCheckpoint = true;
                        }
                    }
                }
            }
            // equivalent of UnitClientEvents.onMouseClick()
            else if (buildingToPlace == null) {

                // select all nearby buildings of the same type when the same building is double-clicked
                if (selectedBuildings.size() == 1 && MC.level != null && !Keybindings.shiftMod.isDown() &&
                    (System.currentTimeMillis() - lastLeftClickTime) < DOUBLE_CLICK_TIME_MS &&
                    preSelBuilding != null && selectedBuildings.contains(preSelBuilding)) {

                    lastLeftClickTime = 0;
                    Building selBuilding = selectedBuildings.get(0);
                    BlockPos centre = selBuilding.centrePos;
                    ArrayList<Building> nearbyBuildings = getBuildingsWithinRange(
                            new Vec3(centre.getX(), centre.getY(), centre.getZ()),
                            OrthoviewClientEvents.getZoom() * 2,
                            selBuilding.name
                    );
                    clearSelectedBuildings();
                    for (Building building : nearbyBuildings)
                        if (getPlayerToBuildingRelationship(building) == Relationship.OWNED)
                            addSelectedBuilding(building);
                }

                // left click -> select a single building
                // if shift is held, deselect a building or add it to the selected group
                else if (preSelBuilding != null && CursorClientEvents.getLeftClickAction() == null) {
                    boolean deselected = false;

                    if (Keybindings.shiftMod.isDown())
                        deselected = selectedBuildings.remove(preSelBuilding);

                    if (Keybindings.shiftMod.isDown() && !deselected &&
                            getPlayerToBuildingRelationship(preSelBuilding) == Relationship.OWNED) {
                        addSelectedBuilding(preSelBuilding);
                    }
                    else if (!deselected && UnitClientEvents.getPreselectedUnits().size() == 0) { // select a single building - this should be the only code path that allows you to select a non-owned building
                        clearSelectedBuildings();
                        addSelectedBuilding(preSelBuilding);
                    }
                }
            }

            // deselect any non-owned buildings if we managed to select them with owned buildings
            // and disallow selecting > 1 non-owned building
            if (selectedBuildings.size() > 1)
                selectedBuildings.removeIf(b -> getPlayerToBuildingRelationship(b) != Relationship.OWNED);

            lastLeftClickTime = System.currentTimeMillis();
        }
        else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            // set rally points
            if (!Keybindings.altMod.isDown()) {
                for (Building selBuilding : selectedBuildings) {
                    if (selBuilding instanceof ProductionBuilding selProdBuilding && getPlayerToBuildingRelationship(selBuilding) == Relationship.OWNED) {
                        BlockPos rallyPoint = CursorClientEvents.getPreselectedBlockPos();
                        selProdBuilding.setRallyPoint(rallyPoint);
                        BuildingServerboundPacket.setRallyPoint(
                                selBuilding.originPos,
                                rallyPoint
                        );
                    }
                }
            }
            setBuildingToPlace(null);
        }
    }

    @SubscribeEvent
    public static void onButtonPress(ScreenEvent.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == GLFW.GLFW_KEY_DELETE) {
            Building building = HudClientEvents.hudSelectedBuilding;
            if (building != null && building.isBuilt && getPlayerToBuildingRelationship(building) == Relationship.OWNED) {
                HudClientEvents.hudSelectedBuilding = null;
                BuildingServerboundPacket.cancelBuilding(building.minCorner);
            }
        }
    }

    @SubscribeEvent
    public static void onButtonPress(ScreenEvent.KeyReleased.Post evt) {
        if (MC.level != null && MC.player != null)
            if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT_SHIFT)
                setBuildingToPlace(null);
    }

    // prevent selection of buildings out of view
    private static final int VIS_CHECK_TICKS_MAX = 10;
    private static int ticksToNextVisCheck = VIS_CHECK_TICKS_MAX;
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        ticksToNextVisCheck -= 1;
        if (ticksToNextVisCheck <= 0) {
            ticksToNextVisCheck = VIS_CHECK_TICKS_MAX;
            selectedBuildings.removeIf(b -> !FogOfWarClientEvents.isBuildingInBrightChunk(b));
        }

        if (!replacedTexture) {
            replaceOverlayTexture();
            replacedTexture = true;
        }
        if (MC.level != null && MC.level.dimension() == Level.OVERWORLD) {
            for (Building building : buildings)
                building.tick(MC.level);

            // cleanup destroyed buildings
            selectedBuildings.removeIf(Building::shouldBeDestroyed);
            buildings.removeIf(Building::shouldBeDestroyed);
        }
    }

    // on closing a chest screen check that it could be a stockpile chest so they can be consumed for resources
    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing evt) {
        String screenName = evt.getScreen().getTitle().getString();
        if ((screenName.equals("Chest") || screenName.equals("Large Chest")) && MC.level != null && MC.player != null) {
            BlockPos bp = Item.getPlayerPOVHitResult(MC.level, MC.player, ClipContext.Fluid.NONE).getBlockPos();
            BuildingServerboundPacket.checkStockpileChests(bp);
        }
    }

    public static ArrayList<Building> getBuildingsWithinRange(Vec3 pos, float range, String buildingName) {
        ArrayList<Building> retBuildings = new ArrayList<>();
        for (Building building : buildings) {
            if (building.name.equals(buildingName)) {
                BlockPos centre = building.centrePos;
                Vec3 centreVec3 = new Vec3(centre.getX(), centre.getY(), centre.getZ());
                if (pos.distanceTo(centreVec3) <= range)
                    retBuildings.add(building);
            }
        }
        return retBuildings;
    }

    // place a building clientside that has already been registered on serverside
    public static void placeBuilding(String buildingName, BlockPos pos, Rotation rotation, String ownerName, int numBlocksToPlace) {
        for (Building building : buildings)
            if (BuildingUtils.isPosPartOfAnyBuilding(true, pos, false))
                return; // building already exists clientside

        Building newBuilding = BuildingUtils.getNewBuilding(buildingName, MC.level, pos, rotation, ownerName);

        // add a bunch of dummy blocks so clients know not to remove buildings before the first blocks get placed
        while (numBlocksToPlace > 0) {
            newBuilding.addToBlockPlaceQueue(new BuildingBlock(new BlockPos(0,0,0), Blocks.AIR.defaultBlockState()));
            numBlocksToPlace -= 1;
        }

        if (newBuilding != null) {
            boolean buildingExists = false;
            for (Building building : buildings)
                if (building.originPos == pos) {
                    buildingExists = true;
                    break;
                }
            if (!buildingExists)
                buildings.add(newBuilding);
        }

        // sync the goal so we can display the correct animations
        Entity entity = hudSelectedEntity;
        if (entity instanceof WorkerUnit workerUnit) {
            ((Unit) entity).resetBehaviours();
            workerUnit.getBuildRepairGoal().setBuildingTarget(newBuilding);
        }
    }

    public static void destroyBuilding(BlockPos pos) {
        buildings.removeIf(b -> b.originPos == pos);
    }

    public static void syncBuildingBlocks(Building serverBuilding, int blocksPlaced) {
        for (Building building : buildings)
            if (building.originPos.equals(serverBuilding.originPos))
                building.setServerBlocksPlaced(blocksPlaced);
    }

    public static Relationship getPlayerToBuildingRelationship(Building building) {
        if (MC.player != null && building.ownerName.equals(MC.player.getName().getString()))
            return Relationship.OWNED;
        else
            return Relationship.HOSTILE;
    }

    // does the player own one of these buildings?
    public static boolean hasFinishedBuilding(String buildingName) {
        for (Building building : buildings)
            if (building.name.equals(buildingName) && building.isBuilt && MC.player != null &&
                building.ownerName.equals(MC.player.getName().getString()))
                return true;
        return false;
    }
}
