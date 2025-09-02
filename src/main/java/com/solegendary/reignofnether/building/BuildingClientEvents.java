package com.solegendary.reignofnether.building;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.buildings.monsters.Laboratory;
import com.solegendary.reignofnether.building.buildings.neutral.NeutralTransportPortal;
import com.solegendary.reignofnether.building.buildings.piglins.CentralPortal;
import com.solegendary.reignofnether.building.buildings.piglins.PortalBasic;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
import com.solegendary.reignofnether.building.buildings.villagers.Castle;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.nether.NetherBlocks;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.solegendary.reignofnether.hud.HudClientEvents.*;
import static com.solegendary.reignofnether.unit.Checkpoint.CHECKPOINT_TICKS_FADE;
import static com.solegendary.reignofnether.unit.UnitClientEvents.getSelectedUnits;

public class BuildingClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    public static int getTotalPopulationSupply(String playerName) {
        if (ResearchClient.hasCheat("foodforthought")) {
            return GameruleClient.maxPopulation;
        }

        int totalPopulationSupply = 0;
        for (BuildingPlacement building : buildings)
            if (building.ownerName.equals(playerName) && building.isBuilt) {
                totalPopulationSupply += building.getBuilding().cost.population;
            }

        return Math.min(GameruleClient.maxPopulation, totalPopulationSupply);
    }

    // clientside buildings used for tracking position (for cursor selection)
    private static final ArrayList<BuildingPlacement> buildings = new ArrayList<>();

    private static final ArrayList<BuildingPlacement> selectedBuildings = new ArrayList<>();
    private static Building buildingToPlace = null;
    private static Building lastBuildingToPlace = null;
    private static ArrayList<BuildingBlock> blocksToDraw = new ArrayList<>();
    private static boolean replacedTexture = false;
    private static Rotation buildingRotation = Rotation.NONE;
    private static Vec3i buildingDimensions = new Vec3i(0, 0, 0);

    public static ArrayList<BuildingBlock> getBlocksToDraw() { return blocksToDraw; }

    private static long lastLeftClickTime = 0; // to track double clicks
    private static final long DOUBLE_CLICK_TIME_MS = 500;

    // minimum % of blocks below a building that need to be supported by a solid block for it to be placeable
    // 1 means you can't have any gaps at all, 0 means you can place buildings in mid-air
    private static final float MIN_SUPPORTED_BLOCKS_PERCENT = 0.6f;

    private static final float MIN_NETHER_BLOCKS_PERCENT = 0.8f; // piglin buildings must be build on at least 80%
    // nether blocks

    private static final int MIN_BRIDGE_SIZE = 10; // a bridge must have at least 10 blocks to be placeable
    private static final float MIN_BRIDGE_LIQUID_BLOCKS_PERCENT = 0.20f; // at least 20% of covered blocks must be
    // liquid
    private static final float MAX_BRIDGE_LIQUID_BLOCKS_PERCENT = 0.95f; // at least 5% of covered blocks must be solid

    // can only be one preselected building as you can't box-select them like units
    public static BuildingPlacement getPreselectedBuilding() {
        for (BuildingPlacement building : buildings)
            if (building.isPosInsideBuilding(CursorClientEvents.getPreselectedBlockPos())) {
                return building;
            }
        return null;
    }

    public static ArrayList<BuildingPlacement> getSelectedBuildings() {
        return selectedBuildings;
    }

    public static List<BuildingPlacement> getBuildings() {
        return buildings;
    }

    public static void clearSelectedBuildings() {
        selectedBuildings.clear();
    }

    public static void addSelectedBuilding(BuildingPlacement building) {
        CursorClientEvents.setLeftClickAction(null);

        if (!FogOfWarClientEvents.isBuildingInBrightChunk(building)) {
            return;
        }

        selectedBuildings.add(building);
        selectedBuildings.sort(Comparator.comparing(b -> ReignOfNetherRegistries.BUILDING.getKey(b.getBuilding()).toString()));
        UnitClientEvents.clearSelectedUnits();
    }

    private static boolean isBuildingToPlaceABridge() {
        return buildingToPlace instanceof AbstractBridge;
    }

    // switch to the building with the least production, so we can spread out production items
    public static void switchHudToIdlestBuilding() {
        if (hudSelectedPlacement == null || MC.player == null)
            return;
        BuildingPlacement idlestBuilding = null;

        List<BuildingPlacement> sameNameBuildings = selectedBuildings.stream().filter(
                b -> b.getBuilding().equals(hudSelectedPlacement.getBuilding()) && b.isBuilt && b.ownerName.equals(MC.player.getName().getString())
        ).toList();

        float prodTicksLeftMax = Float.MAX_VALUE;
        for (BuildingPlacement building : sameNameBuildings) {
            if (building instanceof ProductionPlacement prodB) {
                Float prodTicksLeft = prodB.productionQueue.stream().map(p -> p.ticksLeft).reduce(0F, Float::sum);
                if (prodTicksLeft < prodTicksLeftMax) {
                    prodTicksLeftMax = prodTicksLeft;
                    idlestBuilding = building;
                }
            }
        }
        if (idlestBuilding != null)
            hudSelectedPlacement = idlestBuilding;
    }

    public static void setBuildingToPlace(Building building) {
        buildingToPlace = building;

        if ((buildingToPlace != lastBuildingToPlace) && buildingToPlace != null) {
            // load the new buildingToPlace's data
            try {
                if (buildingToPlace instanceof AbstractBridge bridge) {
                    blocksToDraw = bridge.getRelativeBlockData(MC.level, isBridgeDiagonal());
                } else {
                    blocksToDraw = buildingToPlace.getRelativeBlockData(MC.level);
                }
                buildingDimensions = BuildingUtils.getBuildingSize(blocksToDraw);
                buildingRotation = Rotation.NONE;
            } catch (Exception e) {
                e.printStackTrace();
            }
            lastBuildingToPlace = buildingToPlace; // avoid loading the same data twice unnecessarily
        }
    }

    public static Building getBuildingToPlace() {
        return buildingToPlace;
    }

    // adds a green overlay option to OverlayTexture at (0,0)
    public static void replaceOverlayTexture() {
        NativeImage nativeimage = MC.gameRenderer.overlayTexture.texture.getPixels();
        int bgr = MiscUtil.reverseHexRGB(0x00FF00); // for some reason setPixelRGBA reads it as ABGR with A inversed
        if (nativeimage != null) {
            nativeimage.setPixelRGBA(0, 0, bgr | (0xB2 << 24));
            RenderSystem.activeTexture(33985);
            MC.gameRenderer.overlayTexture.texture.bind();
            nativeimage.upload(0,
                0,
                0,
                0,
                0,
                nativeimage.getWidth(),
                nativeimage.getHeight(),
                false,
                true,
                false,
                false
            );
            RenderSystem.activeTexture(33984);
        }
    }

    // draws the building with a green/red overlay (based on placement validity) at the target position
    // based on whether the location is valid or not
    // location should be 1 space above the selected spot
    // forceColour == 0 (none), 1 (green), 2 (red)
    public static void drawBuildingToPlace(PoseStack matrix, BlockPos originPos, int forceColour) {
        if (buildingToPlace == null)
            return;

        boolean valid = isBuildingPlacementValid(originPos);

        int minX = 999999;
        int minY = 999999;
        int minZ = 999999;
        int maxX = -999999;
        int maxY = -999999;
        int maxZ = -999999;

        for (BuildingBlock block : blocksToDraw) {
            if (buildingToPlace != null && isBuildingToPlaceABridge()
                && MC.level != null && AbstractBridge.shouldCullBlock(originPos.offset(0, 1, 0), block, MC.level)) {
                continue;
            }

            BlockRenderDispatcher renderer = MC.getBlockRenderer();
            BlockState bs = block.getBlockState();
            BlockPos bp = block.getBlockPos().offset(originPos);
            // ModelData modelData = renderer.getBlockModel(bs).getModelData(MC.level, bp, bs, ModelDataManager
            // .getModelData(MC.level, bp));

            matrix.pushPose();
            Entity cam = MC.cameraEntity;
            matrix.translate( // bp is center of block whereas render is corner, so offset by 0.5
                bp.getX() - cam.getX(), bp.getY() - cam.getY() - 0.6, bp.getZ() - cam.getZ());

            int overlayColour = valid ? OverlayTexture.pack(0, 0) : OverlayTexture.pack(0, 3);
            if (forceColour == 1) {
                overlayColour = OverlayTexture.pack(0, 0);
            } else if (forceColour == 2) {
                overlayColour = OverlayTexture.pack(0, 3);
            }
            renderer.renderSingleBlock(bs,
                matrix,
                MC.renderBuffers().crumblingBufferSource(),
                // don't render over other stuff
                15728880,
                // red if invalid, else green
                overlayColour,
                net.minecraftforge.client.model.data.ModelData.EMPTY,
                null
            );

            matrix.popPose();

            if (bp.getX() < minX) {
                minX = bp.getX();
            }
            if (bp.getY() < minY) {
                minY = bp.getY();
            }
            if (bp.getZ() < minZ) {
                minZ = bp.getZ();
            }
            if (bp.getX() > maxX) {
                maxX = bp.getX();
            }
            if (bp.getY() > maxY) {
                maxY = bp.getY();
            }
            if (bp.getZ() > maxZ) {
                maxZ = bp.getZ();
            }
        }
        // draw placement outline below
        maxX += 1;
        minY += 1.05f;
        maxZ += 1;

        float r = valid ? 0 : 1.0f;
        float g = valid ? 1.0f : 0;
        // highlight yellow if we are placing a portal on overworld terrain
        if (valid) {
            if (buildingToPlace instanceof PortalBasic &&
                    !isOnNetherBlocks(blocksToDraw, originPos)) {
                r = 0.5f;
                g = 0.5f;
            }
        }
        if (forceColour == 1) {
            r = 0;
            g = 1;
        } else if (forceColour == 2) {
            r = 1;
            g = 0;
        }
        if (minY < 0) {
            minY -= 1;
        }
        ResourceLocation rl = new ResourceLocation("forge:textures/white.png");
        AABB aabb = new AABB(minX, minY, minZ, maxX, minY, maxZ);
        MyRenderer.drawLineBox(matrix, aabb, r, g, 0, 0.5f);
        MyRenderer.drawSolidBox(matrix, aabb, Direction.UP, r, g, 0, 0.5f, rl);
        AABB aabb2 = new AABB(minX, -64, minZ, maxX, minY, maxZ);
        MyRenderer.drawLineBox(matrix, aabb2, r, g, 0, 0.25f);
    }

    public static boolean isBuildingPlacementValid(BlockPos originPos) {
        return !isBuildingPlacementInAir(originPos) && !isBuildingPlacementClipping(originPos)
            && !isOverlappingAnyOtherBuilding() && isNonPiglinOrOnNetherBlocks(originPos) && isNonBridgeOrValidBridge(
            originPos) && FogOfWarClientEvents.isInBrightChunk(originPos) && isBuildingPlacementWithinWorldBorder(
            originPos) && isNotTutorialOrNearValidCapitolPosition(originPos);
    }

    public static void checkBuildingPlacementValidityWithMessages(BlockPos originPos) {
        if (!isBuildingPlacementWithinWorldBorder(originPos)) {
            showTemporaryMessage(I18n.get("building.reignofnether.outside_map"));
        } else if (isBuildingPlacementInAir(originPos)) {
            showTemporaryMessage(I18n.get("building.reignofnether.ground_not_flat"));
        } else if (isBuildingPlacementClipping(originPos)) {
            showTemporaryMessage(I18n.get("building.reignofnether.ground_not_flat"));
        } else if (isOverlappingAnyOtherBuilding()) {
            showTemporaryMessage(I18n.get("building.reignofnether.too_close"));
        } else if (!isNonPiglinOrOnNetherBlocks(originPos)) {
            showTemporaryMessage(I18n.get("building.reignofnether.must_be_nether"));
        } else if (!isNonBridgeOrValidBridge(originPos)) {
            showTemporaryMessage(I18n.get("building.reignofnether.must_be_liquid"));
        } else if (!FogOfWarClientEvents.isInBrightChunk(originPos)) {
            showTemporaryMessage(I18n.get("building.reignofnether.unexplored"));
        } else if (!isNotTutorialOrNearValidCapitolPosition(originPos)) {
            showTemporaryMessage(I18n.get("building.reignofnether.build_centre_here"));
            OrthoviewClientEvents.forceMoveCam(TutorialClientEvents.BUILD_CAM_POS, 50);
        }

    }

    // disallow any building block from clipping into any other existing blocks
    private static boolean isBuildingPlacementClipping(BlockPos originPos) {
        if (MC.level == null) {
            return false;
        }
        if (isBuildingToPlaceABridge() || GameruleClient.slantedBuilding) {
            return false;
        }

        for (BuildingBlock block : blocksToDraw) {
            BlockPos bp = block.getBlockPos().offset(originPos).offset(0, 1, 0);
            if ((MC.level.getBlockState(bp).isSolid() || !MC.level.getBlockState(bp).getFluidState().isEmpty()) && (block.getBlockState().isSolid() || !block.getBlockState().getFluidState().isEmpty())) {
                return true;
            }
        }
        return false;
    }

    // 90% all solid blocks at the base of the building must be on top of solid blocks to be placeable
    // excluding those under blocks which aren't solid anyway
    private static boolean isBuildingPlacementInAir(BlockPos originPos) {
        if (isBuildingToPlaceABridge() || GameruleClient.slantedBuilding) {
            return false;
        }
        int solidBlocksBelow = 0;
        int blocksBelow = 0;
        for (BuildingBlock block : blocksToDraw) {
            if (block.getBlockPos().getY() == 0 && MC.level != null) {
                BlockPos bp = block.getBlockPos().offset(originPos).offset(0, 1, 0);
                BlockState bs = block.getBlockState(); // building block
                BlockState bsBelow = MC.level.getBlockState(bp.below()); // world block

                if (bs.isSolid() && !(bsBelow.getBlock() instanceof IceBlock)) {
                    blocksBelow += 1;
                    if (bsBelow.isSolid() && !(bsBelow.getBlock() instanceof LeavesBlock)) {
                        solidBlocksBelow += 1;
                    }
                }
            }
        }
        if (blocksBelow <= 0) {
            return false; // avoid division by 0
        }
        return ((float) solidBlocksBelow / (float) blocksBelow) < MIN_SUPPORTED_BLOCKS_PERCENT;
    }

    // disallow the building borders from overlapping any other's, even if they don't collide physical blocks
    // also allow for a 1 block gap between buildings so units can spawn and stairs don't have their blockstates
    // messed up
    private static boolean isOverlappingAnyOtherBuilding() {

        BlockPos origin = getBuildingOriginPos(CursorClientEvents.getPreselectedBlockPos());
        Vec3i originOffset = new Vec3i(origin.getX(), origin.getY(), origin.getZ());
        BlockPos minPos = BuildingUtils.getMinCorner(blocksToDraw).offset(originOffset);//.offset(-1, -1, -1);
        BlockPos maxPos = BuildingUtils.getMaxCorner(blocksToDraw).offset(originOffset);//.offset(1, 1, 1);

        for (BuildingPlacement building : buildings) {
            for (BuildingBlock block : building.blocks) {
                if (isBuildingToPlaceABridge() && building.getBuilding() instanceof AbstractBridge) {
                    continue;
                }
                BlockPos bp = block.getBlockPos();
                if (bp.getX() >= minPos.getX() && bp.getX() <= maxPos.getX() && bp.getY() >= minPos.getY()
                    && bp.getY() <= maxPos.getY() && bp.getZ() >= minPos.getZ() && bp.getZ() <= maxPos.getZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNonPiglinOrOnNetherBlocks(BlockPos originPos) {
        if (isBuildingToPlaceABridge()) {
            return true;
        }
        if (buildingToPlace.getFaction() != Faction.PIGLINS || buildingToPlace instanceof CentralPortal) {
            return true;
        }
        if (buildingToPlace instanceof PortalBasic) {
            return true;
        }

        return isOnNetherBlocks(blocksToDraw, originPos);
    }

    public static boolean isOnNetherBlocks(List<BuildingBlock> blocks, BlockPos originPos) {
        int netherBlocksBelow = 0;
        int blocksBelow = 0;
        for (BuildingBlock block : blocks) {
            if (block.getBlockPos().getY() == 0 && MC.level != null) {
                BlockPos bp = block.getBlockPos().offset(originPos).offset(0, 1, 0);
                BlockState bs = block.getBlockState(); // building block
                BlockState bsBelow = MC.level.getBlockState(bp.below()); // world block

                if (bs.isSolid()) {
                    blocksBelow += 1;
                    if (NetherBlocks.isNetherBlock(MC.level, bp.below())) {
                        netherBlocksBelow += 1;
                    }
                }
            }
        }
        if (blocksBelow <= 0) {
            return false; // avoid division by 0
        }
        return ((float) netherBlocksBelow / (float) blocksBelow) > MIN_NETHER_BLOCKS_PERCENT;
    }

    // bridges should be connected to land or another bridge and be touching water
    private static boolean isNonBridgeOrValidBridge(BlockPos originPos) {
        if (!isBuildingToPlaceABridge()) {
            return true;
        }

        int placeableBlocks = 0;
        for (BuildingBlock block : blocksToDraw)
            if (!AbstractBridge.shouldCullBlock(originPos.offset(0, 1, 0), block, MC.level) && !block.getBlockState()
                .isAir()) {
                placeableBlocks += 1;
            }
        if (placeableBlocks < MIN_BRIDGE_SIZE) {
            return false;
        }

        int bridgeBlocks = 0;
        int waterBlocksClipping = 0;
        for (BuildingBlock block : blocksToDraw) {
            if (block.getBlockState().isAir()) {
                continue;
            }
            if (MC.level != null) {
                BlockPos bp = block.getBlockPos().offset(originPos).offset(0, 1, 0);
                BlockState bs = block.getBlockState(); // building block
                BlockState bsWorld = MC.level.getBlockState(bp); // world block

                // top y level should not be touching any water at all
                if (block.getBlockPos().getY() == 1) {
                    if ((bs.getBlock() instanceof FenceBlock) && !bsWorld.getFluidState().isEmpty()) {
                        return false;
                    }
                }

                if (block.getBlockPos().getY() == 0) {
                    bridgeBlocks += 1;
                    if (!bsWorld.getFluidState().isEmpty() || bsWorld.getBlock() instanceof SeagrassBlock
                        || bsWorld.getBlock() instanceof KelpBlock) {
                        waterBlocksClipping += 1;
                    }
                }
            }
        }
        if (bridgeBlocks <= 0) {
            return false; // avoid division by 0
        }
        float percentWater = (float) waterBlocksClipping / (float) bridgeBlocks;
        return percentWater > MIN_BRIDGE_LIQUID_BLOCKS_PERCENT && percentWater < MAX_BRIDGE_LIQUID_BLOCKS_PERCENT;
    }

    private static boolean isNotTutorialOrNearValidCapitolPosition(BlockPos originPos) {
        if (!TutorialClientEvents.isEnabled()) {
            return true;
        }

        if (!(buildingToPlace instanceof TownCentre)) {
            return true;
        }

        return TutorialClientEvents.BUILD_CAPITOL_POS.distSqr(originPos) < 625; // 25 block range
    }

    private static boolean isBuildingPlacementWithinWorldBorder(BlockPos originPos) {
        if (MC.level == null || buildingToPlace == null) {
            return false;
        }

        int minX = 999999;
        int minZ = 999999;
        int maxX = -999999;
        int maxZ = -999999;

        for (BlockPos bp : blocksToDraw.stream().map(BuildingBlock::getBlockPos).toList()) {
            if (bp.getX() < minX) {
                minX = bp.getX();
            }
            if (bp.getZ() < minZ) {
                minZ = bp.getZ();
            }
            if (bp.getX() > maxX) {
                maxX = bp.getX();
            }
            if (bp.getZ() > maxZ) {
                maxZ = bp.getZ();
            }
        }
        int buildingRadius = Math.max(maxZ - minZ, maxX - minX) / 2;

        BlockPos cursorPos = CursorClientEvents.getPreselectedBlockPos();
        return MC.level.getWorldBorder().getDistanceToBorder(cursorPos.getX(), cursorPos.getZ()) > buildingRadius;
    }

    /*
    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        if (MC.level == null)
            return;

        MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
                "dist to border: " + MC.level.getWorldBorder().getDistanceToBorder(cursorPos.getX(), cursorPos.getZ()),
        });
    }
     */

    // gets the cursor position rotated according to the preselected building
    public static BlockPos getBuildingOriginPos(BlockPos bp) {
        int xAdj = 0;
        int zAdj = 0;
        int xRadius = buildingDimensions.getX() / 2;
        int zRadius = buildingDimensions.getZ() / 2;

        switch (buildingRotation) {
            case NONE -> {
                xAdj = -xRadius;
                zAdj = -zRadius;
            }
            case CLOCKWISE_90 -> {
                xAdj = xRadius;
                zAdj = -zRadius;
            }
            case CLOCKWISE_180 -> {
                xAdj = xRadius;
                zAdj = zRadius;
            }
            case COUNTERCLOCKWISE_90 -> {
                xAdj = -xRadius;
                zAdj = zRadius;
            }
        }
        if (isBuildingToPlaceABridge()) {
            bp = bp.offset(0, -1, 0);
        }

        return bp.offset(xAdj, 0, zAdj);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) throws NoSuchFieldException {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!OrthoviewClientEvents.isEnabled()) {
            return;
        }

        drawBuildingToPlace(evt.getPoseStack(), getBuildingOriginPos(CursorClientEvents.getPreselectedBlockPos()), 0);

        BuildingPlacement preselectedBuilding = getPreselectedBuilding();

        for (BuildingPlacement building : buildings) {

            boolean isInBrightChunk = FogOfWarClientEvents.isBuildingInBrightChunk(building);

            AABB aabb = new AABB(building.minCorner, building.maxCorner.offset(1, 1, 1));

            var colorHex = new Color(PlayerColors.getPlayerDisplayColor(building.ownerName));
            float r = colorHex.getRed() / 255.0f;
            float g = colorHex.getGreen() / 255.0f;
            float b = colorHex.getBlue() / 255.0f;

            if (isInBrightChunk) {
                if (selectedBuildings.contains(building)) {
                    MyRenderer.drawLineBox(evt.getPoseStack(), aabb, r, g, b, 1.0f);
                } else if (building.equals(preselectedBuilding) && !HudClientEvents.isMouseOverAnyButtonOrHud()) {
                    if (hudSelectedEntity instanceof WorkerUnit && MiscUtil.isRightClickDown(MC)) {
                        MyRenderer.drawLineBox(evt.getPoseStack(), aabb, r, g, b, 1.0f);
                    } else {
                        MyRenderer.drawLineBox(evt.getPoseStack(),
                                aabb,
                                r,
                                g,
                                b,
                                MiscUtil.isRightClickDown(MC) ? 1.0f : 0.5f
                        );
                    }
                }
            }

            MyRenderer.drawBoxBottom(evt.getPoseStack(), aabb, r, g, b, 0.2f);
        }

        // draw rally points and lines
        for (BuildingPlacement selBuilding : selectedBuildings) {
            if (selBuilding instanceof ProductionPlacement selProdBuilding) {
                float a = MiscUtil.getOscillatingFloat(0.25f, 0.75f);

                if (!selProdBuilding.getRallyPoints().isEmpty() && MC.level != null) {
                    Vec3 lastPos = Vec3.atBottomCenterOf(selProdBuilding.centrePos.above());
                    for (BlockPos bp : selProdBuilding.getRallyPoints()) {
                        MyRenderer.drawLine(evt.getPoseStack(), lastPos, Vec3.atBottomCenterOf(bp.above()), 0, 1, 0, a);
                        if (MC.level.getBlockState(bp.offset(0,1,0)).getBlock() instanceof SnowLayerBlock) {
                            AABB aabb = new AABB(bp);
                            aabb = aabb.setMaxY(aabb.maxY + 0.13f);
                            MyRenderer.drawSolidBox(evt.getPoseStack(), aabb, Direction.UP, 0, 1, 0, a,
                                    new ResourceLocation("forge:textures/white.png"));
                        } else {
                            MyRenderer.drawBlockFace(evt.getPoseStack(), Direction.UP, bp, 0, 1, 0, a);
                        }
                        lastPos = Vec3.atBottomCenterOf(bp.above());
                    }
                } else if (selProdBuilding.getRallyPointEntity() != null) {
                    LivingEntity le = selProdBuilding.getRallyPointEntity();
                    MyRenderer.drawLine(evt.getPoseStack(), new Vec3(selBuilding.centrePos.getX(),
                        selBuilding.centrePos.getY(),
                        selBuilding.centrePos.getZ()
                    ), new Vec3(le.getX(), le.getEyeY(), le.getZ()), 0, 1, 0, a);
                    MyRenderer.drawLineBoxOutlineOnly(evt.getPoseStack(), le.getBoundingBox(), 0, 1.0f, 0, a, false);
                }
            }
            if (selBuilding instanceof PortalPlacement portal && portal.hasDestination()) {
                float a = MiscUtil.getOscillatingFloat(0.25f, 0.75f);
                MyRenderer.drawLine(evt.getPoseStack(), selBuilding.centrePos, portal.destination, 0, 1, 0, a);
            }
        }


    }

    // on scroll rotate the building placement by 90deg by resorting the blocks list
    // 0 - Orthogonal, 0 deg
    // 1 - Diagonal,   0 deg
    // 2 - Orthogonal, 90 deg
    // 3 - Diagonal,   90 deg
    private static int bridgePlaceState = 0;

    public static boolean isBridgeDiagonal() {
        return bridgePlaceState % 2 != 0;
    }

    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrolled.Post evt) {
        if (buildingToPlace != null) {
            if (buildingToPlace instanceof AbstractBridge bridge) {
                bridgePlaceState += evt.getScrollDelta() > 0 ? 1 : -1;
                if (bridgePlaceState < 0) {
                    bridgePlaceState = 3;
                } else if (bridgePlaceState > 3) {
                    bridgePlaceState = 0;
                }
                try {
                    blocksToDraw = bridge.getRelativeBlockData(MC.level, isBridgeDiagonal());
                    buildingDimensions = BuildingUtils.getBuildingSize(blocksToDraw);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Rotation rotationDelta = List.of(0, 1).contains(bridgePlaceState)
                                         ? Rotation.NONE
                                         : Rotation.CLOCKWISE_90;
                buildingRotation = rotationDelta;
                if (bridgePlaceState == 2) {
                    blocksToDraw.replaceAll(buildingBlock -> buildingBlock.move(MC.level, new BlockPos(-5, 0, 5)));
                }
                blocksToDraw.replaceAll(buildingBlock -> buildingBlock.rotate(MC.level, rotationDelta));
            } else {
                Rotation rotationDelta =
                    evt.getScrollDelta() > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
                buildingRotation = buildingRotation.getRotated(rotationDelta);
                blocksToDraw.replaceAll(buildingBlock -> buildingBlock.rotate(MC.level, rotationDelta));
            }
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre evt) throws NoSuchFieldException,
        IllegalAccessException {
        if (!OrthoviewClientEvents.isEnabled()) {
            return;
        }

        // prevent clicking behind HUDs
        if (HudClientEvents.isMouseOverAnyButtonOrHud()) {
            setBuildingToPlace(null);
            return;
        }

        BlockPos pos = getBuildingOriginPos(CursorClientEvents.getPreselectedBlockPos());

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            BuildingPlacement preSelBuilding = getPreselectedBuilding();

            // place a new building
            if (buildingToPlace != null && isBuildingPlacementValid(pos) && MC.player != null) {
                Building building = buildingToPlace;

                ArrayList<Integer> builderIds = new ArrayList<>();
                for (LivingEntity builderEntity : getSelectedUnits())
                    if (builderEntity instanceof WorkerUnit) {
                        builderIds.add(builderEntity.getId());
                    }

                if (Keybindings.shiftMod.isDown()) {
                    BuildingServerboundPacket.placeAndQueueBuilding(building,
                        isBuildingToPlaceABridge() && bridgePlaceState == 2 ? pos.offset(-5, 0, -5) : pos,
                        buildingRotation,
                        hudSelectedEntity instanceof Unit unit ? unit.getOwnerName() : MC.player.getName().getString(),
                        builderIds.stream().mapToInt(i -> i).toArray(),
                        isBridgeDiagonal()
                    );

                    for (LivingEntity entity : getSelectedUnits()) {
                        if (entity instanceof Unit unit) {
                            unit.getCheckpoints().removeIf(c -> c.bp == null || !BuildingUtils.isPosInsideAnyBuilding(true, c.bp));
                            MiscUtil.addUnitCheckpoint(unit,
                                CursorClientEvents.getPreselectedBlockPos().above(),
                                false
                            );
                            if (unit instanceof WorkerUnit workerUnit) {
                                workerUnit.getBuildRepairGoal().ignoreNextCheckpoint = true;
                            }
                        }
                    }
                } else {
                    boolean hasSelectedWorkers = false;
                    for (LivingEntity entity : getSelectedUnits()) {
                        if (entity instanceof WorkerUnit) {
                            hasSelectedWorkers = true;
                            break;
                        }
                    }
                    String ownerName = MC.player.getName().getString();
                    if (SandboxClientEvents.isSandboxPlayer(ownerName) && !hasSelectedWorkers &&
                        !(buildingToPlace instanceof AbstractBridge)) {
                        if (SandboxClientEvents.relationship == Relationship.NEUTRAL)
                            ownerName = "";
                        else if (SandboxClientEvents.relationship == Relationship.HOSTILE)
                            ownerName = "Enemy";
                    }
                    BuildingServerboundPacket.placeBuilding(buildingToPlace,
                        isBuildingToPlaceABridge() && bridgePlaceState == 2 ? pos.offset(-5, 0, -5) : pos,
                        buildingRotation,
                        hudSelectedEntity instanceof Unit unit ? unit.getOwnerName() : ownerName,
                        builderIds.stream().mapToInt(i -> i).toArray(),
                        isBridgeDiagonal()
                    );
                    setBuildingToPlace(null);

                    if (hasSelectedWorkers) {
                        for (LivingEntity entity : getSelectedUnits()) {
                            if (entity instanceof Unit unit) {
                                MiscUtil.addUnitCheckpoint(unit, CursorClientEvents.getPreselectedBlockPos().above(), true);
                                if (unit instanceof WorkerUnit workerUnit) {
                                    workerUnit.getBuildRepairGoal().ignoreNextCheckpoint = true;
                                }
                            }
                        }
                    }
                }
            }
            // equivalent of UnitClientEvents.onMouseClick()
            else if (buildingToPlace == null) {

                // select all nearby buildings of the same type when the same building is double-clicked
                if (selectedBuildings.size() == 1 && MC.level != null && !Keybindings.shiftMod.isDown() &&
                    ((System.currentTimeMillis() - lastLeftClickTime) < DOUBLE_CLICK_TIME_MS || Keybindings.ctrlMod.isDown()) &&
                    preSelBuilding != null && selectedBuildings.contains(preSelBuilding)) {

                    lastLeftClickTime = 0;
                    BuildingPlacement selBuilding = selectedBuildings.get(0);
                    BlockPos centre = selBuilding.centrePos;
                    ArrayList<BuildingPlacement> nearbyBuildings = getBuildingsWithinRange(new Vec3(centre.getX(),
                            centre.getY(),
                            centre.getZ()
                        ),
                        OrthoviewClientEvents.getZoom() * 2,
                        selBuilding.getBuilding()
                    );
                    clearSelectedBuildings();
                    for (BuildingPlacement building : nearbyBuildings)
                        if (getPlayerToBuildingRelationship(building) == Relationship.OWNED) {
                            addSelectedBuilding(building);
                        }
                }

                // left click -> select a single building
                // if shift is held, deselect a building or add it to the selected group
                else if (preSelBuilding != null && CursorClientEvents.getLeftClickAction() == null) {
                    boolean deselected = false;

                    if (Keybindings.shiftMod.isDown()) {
                        deselected = selectedBuildings.remove(preSelBuilding);
                    }

                    if (Keybindings.shiftMod.isDown() && !deselected
                        && getPlayerToBuildingRelationship(preSelBuilding) == Relationship.OWNED) {
                        addSelectedBuilding(preSelBuilding);
                    } else if (!deselected && UnitClientEvents.getPreselectedUnits().size()
                        == 0) { // select a single building - this should be the only code path that allows you to
                        // select a non-owned building
                        clearSelectedBuildings();
                        addSelectedBuilding(preSelBuilding);
                    }
                }
            } else {
                checkBuildingPlacementValidityWithMessages(pos);
            }

            // deselect any non-owned buildings if we managed to select them with owned buildings
            // and disallow selecting > 1 non-owned building
            if (selectedBuildings.size() > 1) {
                selectedBuildings.removeIf(b -> getPlayerToBuildingRelationship(b) != Relationship.OWNED);
            }

            lastLeftClickTime = System.currentTimeMillis();
        } else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            // set rally points
            if (!Keybindings.altMod.isDown()) {
                for (BuildingPlacement selBuilding : selectedBuildings) {
                    if (selBuilding instanceof ProductionPlacement selProdBuilding
                        && getPlayerToBuildingRelationship(selBuilding) == Relationship.OWNED) {
                        if (!UnitClientEvents.getPreselectedUnits().isEmpty()) {
                            LivingEntity rallyPointEntity = UnitClientEvents.getPreselectedUnits().get(0);
                            selProdBuilding.setRallyPointEntity(rallyPointEntity);
                            BuildingServerboundPacket.setRallyPointEntity(selBuilding.originPos,
                                rallyPointEntity.getId()
                            );
                        } else {
                            BlockPos rallyPoint = CursorClientEvents.getPreselectedBlockPos();
                            if (Keybindings.shiftMod.isDown()) {
                                selProdBuilding.addRallyPoint(rallyPoint);
                                BuildingServerboundPacket.addRallyPoint(selBuilding.originPos, rallyPoint);
                            } else {
                                selProdBuilding.setRallyPoint(rallyPoint);
                                BuildingServerboundPacket.setRallyPoint(selBuilding.originPos, rallyPoint);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onButtonPress(ScreenEvent.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT_ALT) {
            buildingToPlace = null;
        }
        if (evt.getKeyCode() == GLFW.GLFW_KEY_DELETE) {
            boolean isSandboxPlayer = MC.player != null && SandboxClientEvents.isSandboxPlayer(MC.player.getName().getString());
            BuildingPlacement building = HudClientEvents.hudSelectedPlacement;
            if (building != null &&
                ((building.isBuilt && getPlayerToBuildingRelationship(building) == Relationship.OWNED) || isSandboxPlayer)) {
                HudClientEvents.hudSelectedPlacement = null;
                BuildingServerboundPacket.cancelBuilding(building.minCorner, MC.player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onButtonPress(ScreenEvent.KeyReleased.Post evt) {
        if (MC.level != null && MC.player != null) {
            if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT_SHIFT) {
                setBuildingToPlace(null);
            }
        }
    }

    // prevent selection of buildings out of view
    private static final int VIS_CHECK_TICKS_MAX = 10;
    private static int ticksToNextVisCheck = VIS_CHECK_TICKS_MAX;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) {
            return;
        }

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
            for (BuildingPlacement building : buildings)
                if (!MC.isPaused())
                    building.tick(MC.level);

            // cleanup destroyed buildings
            selectedBuildings.removeIf(BuildingPlacement::shouldBeDestroyed);
            buildings.removeIf(b -> {
                if (b.shouldBeDestroyed()) {
                    b.unFreezeChunks();
                    return true;
                }
                return false;
            });
        }
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening evt) {
        if (evt.getScreen() instanceof BeaconScreen) {
            BlockPos bp = Item.getPlayerPOVHitResult(MC.level, MC.player, ClipContext.Fluid.NONE).getBlockPos();
            if (BuildingUtils.findBuilding(true, bp) instanceof BeaconPlacement)
                evt.setCanceled(true);
        }
    }

    // on closing a chest screen check that it could be a stockpile chest so they can be consumed for resources
    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing evt) {
        if (evt.getScreen() instanceof ContainerScreen && MC.level != null && MC.player != null) {
            BlockPos bp = Item.getPlayerPOVHitResult(MC.level, MC.player, ClipContext.Fluid.NONE).getBlockPos();
            BuildingServerboundPacket.checkStockpileChests(bp);
        }
    }

    public static ArrayList<BuildingPlacement> getBuildingsWithinRange(Vec3 pos, float range, Building building) {
        ArrayList<BuildingPlacement> retBuildings = new ArrayList<>();
        for (BuildingPlacement placement : buildings) {
            if (placement.getBuilding().equals(building)) {
                BlockPos centre = placement.centrePos;
                Vec3 centreVec3 = new Vec3(centre.getX(), centre.getY(), centre.getZ());
                if (pos.distanceTo(centreVec3) <= range) {
                    retBuildings.add(placement);
                }
            }
        }
        return retBuildings;
    }

    // place a building clientside that has already been registered on serverside
    public static void placeBuilding(
        Building building,
        BlockPos pos,
        Rotation rotation,
        String ownerName,
        int numBlocksToPlace,
        boolean isDiagonalBridge,
        int upgradeLevel,
        boolean isBuilt,
        PortalPlacement.PortalType portalType,
        BlockPos portalDestination,
        boolean forPlayerLoggingIn
    ) {

        BuildingPlacement newBuilding = BuildingUtils.getNewBuilding(building,
            MC.level,
            pos,
            rotation,
            ownerName,
            isDiagonalBridge
        );

        for (BuildingPlacement placement : buildings)
            if (newBuilding.originPos.equals(placement.originPos)) {
                return; // skip, building already exists clientside
            }

        // add a bunch of dummy blocks so clients know not to remove buildings before the first blocks get placed
        while (numBlocksToPlace > 0) {
            newBuilding.addToBlockPlaceQueue(new BuildingBlock(new BlockPos(0, 0, 0), Blocks.AIR.defaultBlockState()));
            numBlocksToPlace -= 1;
        }
        if (newBuilding != null && MC.player != null) {
            newBuilding.isBuilt = isBuilt;

            if (isBuilt && forPlayerLoggingIn) {
                newBuilding.highestBlockCountReached = newBuilding.getBlocksTotal();
            }

            if (upgradeLevel > 0) {
                if (newBuilding.getBuilding() instanceof Castle castle) {
                    newBuilding.changeStructure(Castle.upgradedStructureName);
                } else if (newBuilding.getBuilding() instanceof Laboratory lab) {
                    newBuilding.changeStructure(Laboratory.upgradedStructureName);
                } else if (newBuilding instanceof PortalPlacement portal) {
                    if (!(newBuilding.getBuilding() instanceof NeutralTransportPortal)) {
                        portal.changeStructure(portalType);
                    }
                    if (portalType == PortalPlacement.PortalType.TRANSPORT)
                        portal.destination = portalDestination;
                } else if (newBuilding.getBuilding() instanceof Library library) {
                    newBuilding.changeStructure(Library.upgradedStructureName);
                } else if (newBuilding instanceof BeaconPlacement beacon) {
                    beacon.changeStructure(upgradeLevel);
                }
            }
            buildings.add(newBuilding);

            if (FogOfWarClientEvents.isEnabled()) {
                newBuilding.freezeChunks(MC.player.getName().getString(), forPlayerLoggingIn);
            }

            // if a player is looking directly at a frozenchunk on login, they may load in the real blocks before
            // they are frozen so move them to their capitol (or any of their buildings if they don't have one)
            /*
            if (MC.player != null && forPlayerLoggingIn && ownerName.equals(MC.player.getName().getString()) && FogOfWarClientEvents.isEnabled()) {
                if (!FogOfWarClientEvents.movedToCapitol) {
                    OrthoviewClientEvents.centreCameraOnPos(newBuilding.originPos);
                    if (newBuilding.isCapitol) {
                        FogOfWarClientEvents.movedToCapitol = true;  // Set the AtomicBoolean to true
                    }
                }
            }
             */
        }
        // sync the goal so we can display the correct animations
        Entity entity = hudSelectedEntity;
        if (entity instanceof WorkerUnit workerUnit && entity instanceof Unit unit &&
            unit.getOwnerName().equals(ownerName)) {
            ((Unit) entity).resetBehaviours();
            workerUnit.getBuildRepairGoal().setBuildingTarget(newBuilding);
        }
    }

    public static void syncBuilding(BuildingPlacement serverBuilding, int blocksPlaced, String ownerName) {
        for (BuildingPlacement building : buildings)
            if (building.originPos.equals(serverBuilding.originPos)) {
                building.setServerBlocksPlaced(blocksPlaced);
                building.ownerName = ownerName;
            }
    }

    public static Relationship getPlayerToBuildingRelationship(BuildingPlacement building) {
        if (MC.player != null) {
            String playerName = MC.player.getName().getString();
            String buildingOwnerName = building.ownerName;

            if (playerName.equals(buildingOwnerName)) {
                return Relationship.OWNED;
            } else if (AlliancesClient.isAllied(playerName, buildingOwnerName)) {
                return Relationship.FRIENDLY;
            } else if (buildingOwnerName.isBlank()) {
                return Relationship.NEUTRAL;
            } else {
                return Relationship.HOSTILE;
            }
        }

        // If MC.player is null, we can't determine the relationship, so return NEUTRAL.
        return Relationship.NEUTRAL;
    }

    // does the player own one of these buildings?
    public static boolean hasFinishedBuilding(Building building) {
        for (BuildingPlacement bpl : buildings) {
            if (bpl.getBuilding().isTypeOf(building) && bpl.isBuilt &&
                    ((MC.player != null && bpl.ownerName.equals(MC.player.getName().getString())) ||
                    allyHasFinishedBuilding(building))) {
                return true;
            }
        }
        return false;
    }

    // does the selected ally's unit own one of these buildings?
    public static boolean allyHasFinishedBuilding(Building building) {
        if (!AlliancesClient.canControlAlly(hudSelectedEntity))
            return false;

        String allyName = "";
        if (hudSelectedEntity instanceof Unit unit)
            allyName = unit.getOwnerName();

        for (BuildingPlacement bpl : buildings) {
            if (bpl.getBuilding().isTypeOf(building) && bpl.isBuilt &&
                    MC.player != null && bpl.ownerName.equals(allyName)) {
                return true;
            }
        }
        return false;
    }

    public static void syncBeacon(UnitAction action, BlockPos beaconPos, boolean activate) {
        BeaconPlacement beacon = BuildingUtils.getBeacon(true);
        if (beacon == null)
            return;

        if (activate) {
            MobEffect effect = BeaconPlacement.getMobEffectForAction(action);
            if (effect != null)
                beacon.activate(effect);
        } else {
            beacon.deactivate();
        }
    }
}
