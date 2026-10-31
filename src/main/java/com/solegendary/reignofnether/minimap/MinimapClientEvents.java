package com.solegendary.reignofnether.minimap;

import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRolesClient;
import com.solegendary.reignofnether.config.ReignOfNetherClientConfigs;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.blocks.NightCircleMode;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.panels.MapZoom;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.matchstart.MatchStartScreen;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.ping.PingType;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.startpos.StartPos;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.unit.*;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitActionServerboundPacket;
import com.solegendary.reignofnether.util.ArrayUtil;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.solegendary.reignofnether.blocks.BlockClientEvents.nightCircleMode;
import static com.solegendary.reignofnether.hud.HudClientEvents.*;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class MinimapClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static int worldRadius = 100; // how many world blocks should be mapped
    private static int mapGuiRadius = 50; // actual size on the screen
    // bottom-right corner of the map in GUI coordinates, set from the HUD layout every frame
    private static float mapRight = 0;
    private static float mapBottom = 0;

    private static boolean largeMap = false;
    public static boolean isLargeMap() { return largeMap; }

    private static final int UNIT_RADIUS = 3;
    private static final int UNIT_THICKNESS = 1;
    private static final int PLAYER_RADIUS = 5;
    private static final int PLAYER_THICKNESS = 1;
    private static final int START_POS_RADIUS = 7;
    private static final int START_POS_THICKNESS = 2;
    private static final int MARKER_PIXEL_OFFSET = 4; // shift rendered marker to better align with cursor

    // rate-limit teleporting from dragging the minimap to prevent being kicked from packet spamming
    private static long lastDragTeleportTimestamp = System.currentTimeMillis();
    private static BlockPos minimapDragStartBp = null;
	public static boolean minimapRightClickDown = false;

    private static DynamicTexture mapTexture = new DynamicTexture(worldRadius * 2, worldRadius * 2, true);
    private static ResourceLocation mapTexLoc = Minecraft.getInstance().textureManager.register(
        ReignOfNether.MOD_ID + "_" + "minimap",
        mapTexture
    );
    private static RenderType mapRenderType = RenderType.textSeeThrough(mapTexLoc);
    private static int[][] mapColoursTerrain = new int[worldRadius * 2][worldRadius * 2];
    private static int[][] mapColoursOverlays = new int[worldRadius * 2][worldRadius * 2]; // view quad, units, buildings

    private static int terrainPartition = 1;
    private static final int TERRAIN_PARTITIONS_MAX = 10;
    private static int darkTerrainPartition = 1;
    private static final int DARK_TERRAIN_PARTITIONS_MAX = 5; // sub-partitions of terrain_partitions - so there will
    // be 5*10 total
    private static boolean forceUpdateAllPartitions = true;

    private static int xc_world = 0; // world pos x centre, maps to xc
    private static int zc_world = 0; // world pos zcentre, maps to yc
    private static float xc, yc;

    public static final ArrayList<VirtualUnit> virtualUnits = new ArrayList<>();
    public static final ArrayList<MapMarker> mapMarkers = new ArrayList<>();

    // last known position of neutral units in fog
    public static final ArrayList<VirtualUnit> neutralFogUnits = new ArrayList<>();

    private static final float DARK = 0.40f;
    private static final float EXTRA_DARK = 0.10f;

    private static boolean lockedMap = false; // does map follow when moving offscreen?
    // apply glow effect (clientside only) to animals
    public static boolean shouldHighlightAnimals() {
        return ReignOfNetherClientConfigs.HIGHLIGHT_ANIMALS.get();
    }

    // underline units and buildings
    public static boolean shouldUnderline() {
        return ReignOfNetherClientConfigs.UNDERLINES.get();
    }

    public static void addNeutralFogUnit(int id, Vector3f vec3fMin, Vector3f vec3fMax) {
        Vec3 vec3Min = new Vec3(vec3fMin.x, vec3fMin.y, vec3fMin.z);
        Vec3 vec3Max = new Vec3(vec3fMax.x, vec3fMax.y, vec3fMax.z);
        virtualUnits.removeIf(mu -> mu.id == id);
        neutralFogUnits.removeIf(mu -> mu.id == id);
        neutralFogUnits.add(new VirtualUnit(id, new AABB(vec3Min, vec3Max)));
    }

    public static void removeNeutralFogUnit(int id) {
        neutralFogUnits.removeIf(virtualUnit -> virtualUnit.id == id);
    }

    public static void highlightNeutralFogUnits(PoseStack pose, VertexConsumer vertexConsumer) {
        if (!FogOfWarClientEvents.isEnabled())
            return;
        for (VirtualUnit mu : neutralFogUnits) {
            if (MC.level != null) {
                Entity entity = MC.level.getEntity(mu.id);
                if (!FogOfWarClientEvents.isBlockVisible(mu.pos) && (entity == null || !FogOfWarClientEvents.isBlockVisible(entity.getOnPos()))) {
                    Color color = new Color(PlayerColors.getPlayerDisplayColorHex(""));
                    float r = color.getRed() / 255.0f;
                    float g = color.getGreen() / 255.0f;
                    float b = color.getBlue() / 255.0f;
                    MyRenderer.drawBoxBottom(pose, mu.aabb, vertexConsumer, r, g, b, 0.5f);
                }
            }
        }
    }

    public static void clearVirtualUnits() {
        virtualUnits.clear();
        neutralFogUnits.clear();
    }

    public static class MapMarker {
        public final int x;
        public final int y;
        public final int z;
        public final String playerName;
        public final PingType type;
        public int ticksRemaining;
        public int ageTicks;

        public MapMarker(int x, int y, int z, String playerName, PingType type) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.playerName = playerName;
            this.type = type;
            this.ticksRemaining = 200; // 10 seconds
            this.ageTicks = 0;
        }

        public boolean tick() {
            ageTicks++;
            ticksRemaining--;
            return ticksRemaining <= 0;
        }
    }

    public static void addMapMarker(int x, int y, int z, String playerName, PingType type) {
        mapMarkers.add(new MapMarker(x, y, z, playerName, type));
    }

    public static void removeVirtualUnit(int id) {
        virtualUnits.removeIf(u -> u.id == id);
    }

    public static void syncVirtualUnits(BlockPos pos, int id, String ownerName, int population) {
        for (VirtualUnit unit : virtualUnits) {
            if (unit.id == id) {
                unit.pos = pos;
                return;
            }
        }
        virtualUnits.add(new VirtualUnit(pos, id, ownerName, population));
    }

    public static void setMapCentre(double x, double z) {
        if (!lockedMap) {
            xc_world = (int) x;
            zc_world = (int) z;
        }
    }

    public static int getWorldRadius() {
        return worldRadius;
    }

    public static int getMapCentreWorldX() {
        return xc_world;
    }

    public static int getMapCentreWorldZ() {
        return zc_world;
    }

    public static boolean isMapReady() {
        return mapTexture != null && mapTexLoc != null;
    }

    /**
     * The square of the world shown by the match setup map, in blocks: {minX, minZ, size}. The playable
     * area (world border) when the mapped texture covers it, so the map is centred on it with no unmapped
     * edges; otherwise the whole mapped area.
     */
    private static int[] playArea() {
        int texMinX = xc_world - worldRadius;
        int texMinZ = zc_world - worldRadius;
        int tex = worldRadius * 2;
        if (MC.level != null) {
            var border = MC.level.getWorldBorder();
            int size = (int) Math.ceil(Math.max(border.getMaxX() - border.getMinX(), border.getMaxZ() - border.getMinZ()));
            int minX = (int) Math.floor((border.getMinX() + border.getMaxX()) / 2 - size / 2.0);
            int minZ = (int) Math.floor((border.getMinZ() + border.getMaxZ()) / 2 - size / 2.0);
            if (minX >= texMinX && minZ >= texMinZ && minX + size <= texMinX + tex && minZ + size <= texMinZ + tex)
                return new int[] { minX, minZ, size };
        }
        return new int[] { texMinX, texMinZ, tex };
    }

    /** Draws the playable area of the map, north up, into the rectangle. */
    public static void renderMapInto(GuiGraphics g, int x, int y, int w, int h) {
        if (mapTexture == null || mapTexLoc == null) return;
        int tex = worldRadius * 2;
        int[] area = playArea();
        int u = area[0] - (xc_world - worldRadius);
        int v = area[1] - (zc_world - worldRadius);
        g.blit(mapTexLoc, x, y, w, h, u, v, area[2], area[2], tex, tex);
    }

    public static Vector2f worldToRect(int worldX, int worldZ, float rectX, float rectY, float rectW, float rectH) {
        int[] area = playArea();
        float u = (worldX - area[0]) / (float) area[2];
        float v = (worldZ - area[1]) / (float) area[2];
        return new Vector2f(rectX + u * rectW, rectY + v * rectH);
    }

    public static void setLargeMap(boolean large) {
        if (largeMap != large) {
            toggleMapSize();
        }
    }

    public static void forceMapCentre(int x, int z) {
        lockedMap = false;
        setMapCentre(x, z);
        forceUpdateAllPartitions = true;
    }

    public static void setMapLocked(boolean locked) {
        lockedMap = locked;
    }

    public static boolean isMapLocked() {
        return lockedMap;
    }

    public static boolean suppressViewQuad = false;

    private static void toggleMapSize() {
        largeMap = !largeMap;
        // the on-screen size comes from the HUD layout, see updateScreenGeometry()
        worldRadius = largeMap ? 240 : 120;
        if (mapTexLoc != null)
            Minecraft.getInstance().textureManager.release(mapTexLoc); // also closes the old DynamicTexture
        mapTexture = new DynamicTexture(worldRadius * 2, worldRadius * 2, true);
        mapTexLoc = Minecraft.getInstance().textureManager.register(
            ReignOfNether.MOD_ID + "_" + "minimap",
            mapTexture
        );
        mapRenderType = RenderType.textSeeThrough(mapTexLoc);
        mapColoursTerrain = new int[worldRadius * 2][worldRadius * 2];
        mapColoursOverlays = new int[worldRadius * 2][worldRadius * 2];
        forceUpdateAllPartitions = true;
    }

    public static Button getCamSensitivityButton() {
        return new Button("Camera Sensitivity",
                14,
                Icon.SLIDERS.texture,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                () -> false,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK) || !largeMap,
                () -> true,
                () -> OrthoviewClientEvents.adjustPanSensitivityMult(true),
                () -> OrthoviewClientEvents.adjustPanSensitivityMult(false),
                List.of(
                        fcs(I18n.get("hud.map.reignofnether.pan_sensitivity.tooltip1",
                                Math.round(OrthoviewClientEvents.getPanSensitivityMult() * 10), Math.round(OrthoviewClientEvents.MAX_PAN_SENSITIVITY * 10))),
                        fcs(I18n.get("hud.map.reignofnether.pan_sensitivity.tooltip2"))
                )
        );
    }

    public static Button getNightCirclesModeButton() {
        return new Button("Night Circles Mode",
                14,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/repeating_command_block_front.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                () -> false,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK) || !largeMap,
                () -> true,
                () -> {
                    if (nightCircleMode == NightCircleMode.ALL) {
                        nightCircleMode = NightCircleMode.NO_OVERLAPS;
                    } else if (nightCircleMode == NightCircleMode.NO_OVERLAPS) {
                        nightCircleMode = NightCircleMode.OFF;
                    } else if (nightCircleMode == NightCircleMode.OFF) {
                        nightCircleMode = NightCircleMode.ALL;
                    }
                    ReignOfNetherClientConfigs.NIGHT_CIRCLES.set(nightCircleMode);
                    for (BuildingPlacement building : BuildingClientEvents.getBuildings()) {
                        RangeIndicatorAddon ria;
                        if ((ria = building.getBuilding().getActiveAddon(RangeIndicatorAddon.class)) != null)
                            ria.updateHighlightBps(building);
                    }
                },
                null,
                List.of(
                    fcs(I18n.get("time.reignofnether.night_circle_mode_all"), nightCircleMode == NightCircleMode.ALL),
                    fcs(I18n.get("time.reignofnether.night_circle_mode_no_overlaps"), nightCircleMode == NightCircleMode.NO_OVERLAPS),
                    fcs(I18n.get("time.reignofnether.night_circle_mode_off"), nightCircleMode == NightCircleMode.OFF)
                )
        );
    }

    public static Button getMapLockButton() {
        return new Button("Lock Map",
                14,
                lockedMap ? Icon.LOCK.texture : Icon.UNLOCK.texture,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                () -> false,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK) || !largeMap,
                () -> true,
                () -> {
                    lockedMap = !lockedMap;
                    if (!lockedMap && MC.player != null)
                        setMapCentre(MC.player.getX(), MC.player.getZ());
                },
                null,
                List.of(fcs(lockedMap
                        ? I18n.get("hud.map.reignofnether.lock_map.tooltip1.enabled")
                        : I18n.get("hud.map.reignofnether.lock_map.tooltip1.disabled"))
                )
        );
    }

    public static Button getHighlightAnimalsButton() {
        return new Button("Highlight Animals",
                14,
                Icon.PAW.texture,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                MinimapClientEvents::shouldHighlightAnimals,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK) || !largeMap,
                () -> true,
                () -> ReignOfNetherClientConfigs.HIGHLIGHT_ANIMALS.set(!shouldHighlightAnimals()),
                null,
                List.of(fcs(shouldHighlightAnimals()
                        ? I18n.get("hud.map.reignofnether.highlight_animals.enabled")
                        : I18n.get("hud.map.reignofnether.highlight_animals.disabled"))
                )
        );
    }

    public static Button getToggleUnderlinesButton() {
        return new Button("Toggle Underlines",
                14,
                shouldUnderline() ?
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/dirt_green_diamond.png") :
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/dirt_dark.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                () -> false,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK) || !largeMap,
                () -> true,
                () -> ReignOfNetherClientConfigs.UNDERLINES.set(!shouldUnderline()),
                null,
                List.of(fcs(shouldUnderline()
                        ? I18n.get("hud.map.reignofnether.underlines.enabled")
                        : I18n.get("hud.map.reignofnether.underlines.disabled"))
                )
        );
    }

    public static Button getCameraRotateCWButton() {
        return new Button("Rotate camera clockwise",
                14,
                Icon.REDO.texture,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                Keybindings.rotCW,
                () -> false,
                () -> false, // has its own tile in the HUD's minimap tools
                () -> true,
                () -> OrthoviewClientEvents.fixedRotateCam(true),
                null,
                List.of(
                        fcs(I18n.get("hud.map.reignofnether.rotate_cw.tooltip1")),
                        fcs(I18n.get("hud.map.reignofnether.rotate_cw.tooltip2"))
                )
        );
    }

    public static Button getCameraRotateCCWButton() {
        return new Button("Rotate camera counter-clockwise",
                14,
                Icon.UNDO.texture,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                Keybindings.rotCCW,
                () -> false,
                () -> false, // has its own tile in the HUD's minimap tools
                () -> true,
                () -> OrthoviewClientEvents.fixedRotateCam(false),
                null,
                List.of(
                        fcs(I18n.get("hud.map.reignofnether.rotate_ccw.tooltip1")),
                        fcs(I18n.get("hud.map.reignofnether.rotate_ccw.tooltip2"))
                )
        );
    }

    public static void updateMapTexture() {
        if (MC.player == null) {
            return;
        }

        // if camera is off the map, start panning the centre of the map
        double xCam = MC.player.getX();
        double zCam = MC.player.getZ();
        double xDiff1 = xCam - (xc_world + worldRadius);

        if (!lockedMap) {
            if (xDiff1 > 0) {
                xc_world += xDiff1;
            }
            double zDiff1 = zCam - (zc_world + worldRadius);
            if (zDiff1 > 0) {
                zc_world += zDiff1;
            }
            double xDiff2 = xCam - (xc_world - worldRadius);
            if (xDiff2 < 0) {
                xc_world += xDiff2;
            }
            double zDiff2 = zCam - (zc_world - worldRadius);
            if (zDiff2 < 0) {
                zc_world += zDiff2;
            }
        }

        NativeImage pixels = mapTexture.getPixels();
        if (pixels != null) {
            int i = 0;
            for (int z = 0; z < worldRadius * 2; z++) {
                for (int x = 0; x < worldRadius * 2; x++) {
                    if (mapColoursOverlays[x][z] != 0) {
                        pixels.setPixelRGBA(x, z, mapColoursOverlays[x][z]);
                    } else {
                        pixels.setPixelRGBA(x, z, mapColoursTerrain[x][z]);
                    }
                    i += 1;
                }
            }
            mapTexture.upload();
        }
    }

    private static void updateMapTerrain(int partition, int darkPartition) {
        if (MC.level == null || MC.player == null) {
            return;
        }

        int zMin = zc_world - worldRadius;
        int zMax = zc_world + worldRadius;
        int xMin = xc_world - worldRadius;
        int xMax = xc_world + worldRadius;

        // sampled once per pass rather than per column: cheaper, and stops a mid-loop fog toggle
        // from producing a half-shaded map
        final boolean fogEnabled = FogOfWarClientEvents.isEnabled();
        final BlockPos.MutableBlockPos borderPos = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // fog state for the chunk the scan is currently inside; refreshed only on chunk boundaries
        long cachedChunkKey = ChunkPos.INVALID_CHUNK_POS;
        boolean chunkBright = true;
        long[] edgeMask = null;

        // draw terrain blocks
        for (int z = zMin; z < zMax; z++) {
            boolean skipDarkPartition = false;

            // eg. if z ranges from -500 to 300, that's a range of 800
            // if we have 10 partitions, partition 3 should range from:
            // zPartMin = (800 / 10) * 2 = 160
            // xPartMax = (800 / 10) * 3 = 240
            // so only update this pixel if the z row is between 160 and 240
            if (!forceUpdateAllPartitions) {
                int zMaxN = zMax - zMin; // zMax normalised to 0 -> (worldRadius * 2)
                int zPartMin = (zMaxN / TERRAIN_PARTITIONS_MAX) * (partition - 1);
                int zPartMax = (zMaxN / TERRAIN_PARTITIONS_MAX) * partition;
                int zN = z - zMin;
                if (zN < zPartMin || zN >= zPartMax) {
                    continue;
                }

                int zPartMind = (zMaxN / DARK_TERRAIN_PARTITIONS_MAX) * (darkPartition - 1);
                int zPartMaxd = (zMaxN / DARK_TERRAIN_PARTITIONS_MAX) * darkPartition;
                if (zN < zPartMind || zN >= zPartMaxd) {
                    skipDarkPartition = true;
                }
            }

            final int chunkZ = z >> 4;

            for (int x = xMin; x < xMax; x++) {

                boolean columnVisible = true;
                if (fogEnabled) {
                    int chunkX = x >> 4;
                    long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
                    if (chunkKey != cachedChunkKey) { // ~once per 16 columns
                        cachedChunkKey = chunkKey;
                        chunkBright = FogOfWarClientEvents.brightChunks.contains(new ChunkPos(chunkX, chunkZ));
                        edgeMask = chunkBright ? FogOfWarClientEvents.getEdgeMask(chunkX, chunkZ) : null;
                    }
                    // apply a much slower update rate to dark chunks
                    if (skipDarkPartition && !chunkBright) {
                        continue;
                    }
                    if (!chunkBright) {
                        columnVisible = false;
                    } else if (edgeMask != null) {
                        int i = ((x & 15) << 4) | (z & 15);
                        columnVisible = (edgeMask[i >> 6] & (1L << (i & 63))) != 0;
                    }
                }

                int y = MC.level.getChunkAt(pos.set(x, 0, z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                BlockState bs;
                do {
                    bs = MC.level.getBlockState(pos.set(x, y, z));
                    if (bs.getBlock() instanceof SnowLayerBlock) {
                        int layers = bs.getValue(SnowLayerBlock.LAYERS);
                        y += (int) (layers * (1.0F / 8.0F));
                        break;
                    }
                    if (!bs.isSolid() && bs.getFluidState().isEmpty() && y > 0) {
                        y -= 1;
                    } else {
                        break;
                    }
                } while (true);

                int yNorth = MC.level.getChunkAt(pos.set(x, 0, z - 1))
                        .getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1);
                BlockState bsNorth;
                do {
                    bsNorth = MC.level.getBlockState(pos.set(x, yNorth, z - 1));
                    if (bsNorth.getBlock() instanceof SnowLayerBlock) {
                        int layersNorth = bsNorth.getValue(SnowLayerBlock.LAYERS);
                        yNorth += (int) (layersNorth * (1.0F / 8.0F));
                        break;
                    }
                    if (!bsNorth.isSolid() && bsNorth.getFluidState().isEmpty() && yNorth > 0) {
                        yNorth -= 1;
                    } else {
                        break;
                    }
                } while (true);

                pos.set(x, yNorth, z - 1);
                MapColor mat = MC.level.getBlockState(pos).getMapColor(MC.level, pos);
                int rgb = mat.col;
                if (bs.getBlock() instanceof SnowLayerBlock) {
                    rgb = 0xFFFFFF;
                }

                // shade blocks to give elevation effects, excluding liquids and nonblocking blocks (eg. grass, flowers)
                if (MC.level.getBlockState(pos).getFluidState().isEmpty()) {
                    if (yNorth > y) {
                        rgb = MiscUtil.shadeHexRGB(rgb, 0.82F);
                    } else if (yNorth < y) {
                        rgb = MiscUtil.shadeHexRGB(rgb, 1.16F);
                    }
                } else { // shade liquid based on depth
                    int depth = 0;
                    int depthMax = 20;
                    BlockState matBelow;
                    do {
                        depth += 1;
                        matBelow = MC.level.getBlockState(pos.set(x, y - depth, z));
                    } while (!matBelow.getFluidState().isEmpty() && depth < depthMax);

                    // only reduce shade every nth step to have the map look sharper
                    depth = (int) (5 * (Math.ceil(Math.abs(depth / 5))));

                    rgb = MiscUtil.shadeHexRGB(rgb, 1.2F - (0.025F * depth));
                }

                // normalise xz's to colour array ranges
                int x0 = x - xc_world + worldRadius;
                int z0 = z - zc_world + worldRadius;

                borderPos.set(x, 0, z);
                if (!MC.level.getWorldBorder().isWithinBounds(borderPos)) {
                    rgb = MiscUtil.shadeHexRGB(rgb, EXTRA_DARK);
                } else if (!columnVisible) {
                    rgb = MiscUtil.shadeHexRGB(rgb, DARK);
                }

                // append 0xFF to include 100% alpha (<< 4 shifts by 1 hex digit)
                mapColoursTerrain[x0][z0] = MiscUtil.reverseHexRGB(rgb) | (0xFF << 24);
            }
        }
        forceUpdateAllPartitions = false;
    }



    private static void updateNightCircles() {

        // get list of night source centre:range pairs
        ArrayList<Pair<BlockPos, Integer>> nightSources = new ArrayList<>();

        for (Pair<BlockPos, Integer> ns : BlockClientEvents.nightSourceOrigins) {

            int xc = ns.getFirst().getX() + (7 / 2);
            int zc = ns.getFirst().getZ() + (7 / 2);
            int xN = xc - xc_world + worldRadius;
            int zN = zc - zc_world + worldRadius;

            nightSources.add(new Pair<>(new BlockPos(xN, 0, zN), ns.getSecond()));
        }

        for (Pair<BlockPos, Integer> ns : nightSources) {
            Set<BlockPos> nightCircleBps;
            if (BlockClientEvents.nightCircleMode == NightCircleMode.NO_OVERLAPS)
                nightCircleBps = MiscUtil.CircleUtil.getCircleWithCulledOverlaps(ns.getFirst(), ns.getSecond(), nightSources);
            else
                nightCircleBps = MiscUtil.CircleUtil.getCircle(ns.getFirst(), ns.getSecond());

            ArrayList<BlockPos> nightCircleBpsThick = new ArrayList<>();
            // raise thickness
            for (BlockPos bp : nightCircleBps) {
                nightCircleBpsThick.add(bp);
                nightCircleBpsThick.add(bp.offset(-1,0,0));
                nightCircleBpsThick.add(bp.offset(0,0,-1));
            }
            for (BlockPos bp : nightCircleBpsThick) {
                if (bp.getX() > 0 && bp.getX() < mapColoursOverlays.length &&
                    bp.getZ() > 0 && bp.getZ() < mapColoursOverlays[0].length)
                    mapColoursOverlays[bp.getX()][bp.getZ()] = MiscUtil.reverseHexRGB(0x0) | (0xFF << 24);
            }
        }
    }

    private static int getBuildingRadius(BuildingPlacement placement) {
        int blockCount = placement.getHighestBlockCountReached();
        if (placement.ownerName.isBlank()) {
            if (blockCount < 20) {
                return 0;
            } else if (blockCount < 50) {
                return 4;
            } else {
                return 7;
            }
        }
        return 7;
    }

    private static int getBuildingThickness(BuildingPlacement placement) {
        int blockCount = placement.getHighestBlockCountReached();
        if (placement.ownerName.isBlank()) {
            if (blockCount < 20) {
                return 0;
            } else if (blockCount < 50) {
                return 1;
            } else {
                return 2;
            }
        }
        return 2;
    }

    private static void updateMapUnitsAndBuildings() {
        // draw buildings
        for (BuildingPlacement building : BuildingClientEvents.getBuildings()) {

            if (!building.isExploredClientside || building.getBuilding() instanceof AbstractBridge)
                continue;

            int buildingRadius = getBuildingRadius(building);
            int buildingThickness = getBuildingThickness(building);
            if (buildingRadius > 0) {
                int xc = building.centrePos.getX() + (buildingRadius / 2);
                int zc = building.centrePos.getZ() + (buildingRadius / 2);
                var rgb = PlayerColors.getPlayerDisplayColorHex(building.ownerName);
                if (!FogOfWarClientEvents.isBuildingInBrightChunk(building)) {
                    var color = new Color(rgb);
                    color = new Color(color.getRed() / 2, color.getGreen() / 2, color.getBlue() / 2);
                    rgb = color.getRGB();
                }
                drawBuildingOnMap(xc, zc, rgb, buildingRadius, buildingThickness);
            }
        }
        // draw starting locations
        if (MC.level != null && StartPosClientEvents.isEnabled() &&
                !StartPosClientEvents.isStarting &&
                !PlayerClientEvents.rtsLocked) {
            drawStartingPosesOnMap();
        }

        // draw players
        if (MC.level != null) {
            for (Player player : MC.level.players()) {
                if (!FogOfWarClientEvents.isInBrightChunk(player))
                    continue;
                drawPlayerOnMap(player.getOnPos().getX(), player.getOnPos().getZ(), player);
            }
        }

        // draw units
        for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
            if (!FogOfWarClientEvents.isInBrightChunk(entity))
                continue;
            var colorHex = PlayerColors.getPlayerDisplayColorHex(entity instanceof Unit unit ? unit.getOwnerName() : null);
            drawUnitOnMap(entity.getOnPos().getX(),
                    entity.getOnPos().getZ(),
                    colorHex
            );
        }
        for (VirtualUnit virtualUnit : virtualUnits) {
            if (!FogOfWarClientEvents.isInBrightChunk(virtualUnit.pos) || MC.player == null)
                continue;
            String unitOwnerName = virtualUnit.ownerName;
            var colorHex = PlayerColors.getPlayerDisplayColorHex(unitOwnerName);
            drawUnitOnMap(
                    virtualUnit.pos.getX(),
                    virtualUnit.pos.getZ(),
                    colorHex
            );
        }
        for (VirtualUnit neutralFogUnit : neutralFogUnits) {
            Entity entity = MC.level.getEntity(neutralFogUnit.id);
            if (FogOfWarClientEvents.isInBrightChunk(neutralFogUnit.pos) || MC.player == null ||
                (entity != null && FogOfWarClientEvents.isInBrightChunk(entity)))
                continue;
            var colorHex = PlayerColors.getPlayerDisplayColorHex("");
            drawUnitOnMap(
                    neutralFogUnit.pos.getX(),
                    neutralFogUnit.pos.getZ(),
                    colorHex
            );
        }

        // draw map markers
        for (MapMarker marker : mapMarkers) {
            drawMapMarker(marker);
        }

    }

    private static void drawBuildingOnMap(int xc, int zc, int color, int radius, int thickness) {
        for (int x = xc - radius; x < xc + radius; x++) {
            for (int z = zc - radius; z < zc + radius; z++) {
                if (isWorldXZinsideMap(x, z)) {
                    int x0 = x - xc + radius;
                    int z0 = z - zc + radius;

                    // if pixel is on the edge of the square keep it coloured black
                    var rgb = (x0 < thickness || x0 >= (radius * 2) - thickness ||
                            z0 < thickness || z0 >= (radius * 2) - thickness)
                            ? 0x000000
                            : color;

                    int xN = x - xc_world + worldRadius;
                    int zN = z - zc_world + worldRadius;

                    mapColoursOverlays[xN][zN] = MiscUtil.reverseHexRGB(rgb) | (0xFF << 24);
                }
            }
        }
    }

    private static void drawUnitOnMap(int xc, int zc, int color) {
        for (int x = xc - UNIT_RADIUS; x < xc + UNIT_RADIUS; x++) {
            for (int z = zc - UNIT_RADIUS; z < zc + UNIT_RADIUS; z++) {
                if (isWorldXZinsideMap(x, z)) {
                    int x0 = x - xc + UNIT_RADIUS;
                    int z0 = z - zc + UNIT_RADIUS;

                    // if pixel is on the edge of the square keep it coloured black
                    var rgb = (x0 < UNIT_THICKNESS || x0 >= (UNIT_RADIUS * 2) - UNIT_THICKNESS ||
                            z0 < UNIT_THICKNESS || z0 >= (UNIT_RADIUS * 2) - UNIT_THICKNESS)
                            ? 0x000000
                            : color;

                    int xN = x - xc_world + worldRadius;
                    int zN = z - zc_world + worldRadius;

                    mapColoursOverlays[xN][zN] = MiscUtil.reverseHexRGB(rgb) | (0xFF << 24);
                }
            }
        }
    }

    private static void drawPlayerOnMap(int xc, int zc, Player player) {
        if (MC.player == null)
            return;
        String thatPlayerName = player.getName().getString();
        if (MC.player.getName().getString().equals(thatPlayerName))
            return;
        // compare factions, so a co-commander's lead and same-faction soldiers count as friendly
        String myFaction = PlayerRolesClient.myCommander();
        String thatFaction = PlayerRolesClient.commanderOf(thatPlayerName);
        boolean friendly = myFaction.equals(thatFaction) || AlliancesClient.isAllied(myFaction, thatFaction);
        if (player.isSpectator() || player.isCreative())
            return;

        for (int x = xc - PLAYER_RADIUS; x < xc + PLAYER_RADIUS; x++) {
            for (int z = zc - PLAYER_RADIUS; z < zc + PLAYER_RADIUS; z++) {
                if (isWorldXZinsideMap(x, z)) {
                    int x0 = x - xc + PLAYER_RADIUS;
                    int z0 = z - zc + PLAYER_RADIUS;
                    int rgb = 0x000000;

                    // if pixel is on the edge of the square keep it coloured black
                    if (!(
                            x0 < PLAYER_THICKNESS || x0 >= (PLAYER_RADIUS * 2) - PLAYER_THICKNESS ||
                                    z0 < PLAYER_THICKNESS || z0 >= (PLAYER_RADIUS * 2) - PLAYER_THICKNESS
                    )) {
                        if (friendly)
                            rgb = 0x3232FF;
                        else
                            rgb = 0xFF0000;
                    }
                    int xN = x - xc_world + worldRadius;
                    int zN = z - zc_world + worldRadius;

                    mapColoursOverlays[xN][zN] = MiscUtil.reverseHexRGB(rgb) | (0xFF << 24);
                }
            }
        }
    }

    private static void drawStartingPosesOnMap() {
        if (MC.player == null)
            return;

        for (StartPos startPos : StartPosClientEvents.startPoses) {
            int xc = startPos.pos.getX();
            int zc = startPos.pos.getZ();

            for (int x = xc - START_POS_RADIUS; x < xc + START_POS_RADIUS; x++) {
                for (int z = zc - START_POS_RADIUS; z < zc + START_POS_RADIUS; z++) {
                    if (isWorldXZinsideMap(x, z)) {
                        int x0 = x - xc + START_POS_RADIUS;
                        int z0 = z - zc + START_POS_RADIUS;
                        int rgb = 0x000000;

                        // if pixel is on the edge of the square keep it coloured black
                        if (!(x0 < START_POS_THICKNESS || x0 >= (START_POS_RADIUS * 2) - START_POS_THICKNESS ||
                                z0 < START_POS_THICKNESS || z0 >= (START_POS_RADIUS * 2) - START_POS_THICKNESS
                        )) {
                            //rgb = PlayerColors.getPlayerDisplayColorHex(startPos.playerName);
                            //if (startPos.faction == Factions.NONE)
                            //    rgb = 0xFFFF00;
                            rgb = startPos.getHexColor();
                        }
                        int xN = x - xc_world + worldRadius;
                        int zN = z - zc_world + worldRadius;

                        mapColoursOverlays[xN][zN] = MiscUtil.reverseHexRGB(rgb) | (0xFF << 24);
                    }
                }
            }
        }
    }

    /** Converts a world XZ coordinate to a minimap screen XY position (centre of that pixel). */
    public static Vec2 worldPosToMinimapScreen(int worldX, int worldZ) {
        float d = contentRadius();
        float u = (worldX + 0.5f - xc_world) / worldRadius;
        float v = (worldZ + 0.5f - zc_world) / worldRadius;
        return new Vec2(xc + d * (u + v) / 2, yc + d * (v - u) / 2);
    }

    /**
     * Half-diagonal the whole map texture would have on screen. The texture is turned 45 degrees to match
     * the camera; at zoom 1 it fills the diamond frame, zoomed in only its middle is drawn.
     */
    private static float contentRadius() {
        // the square frame is filled by a texture twice the diamond's size
        return (isSquare() ? 2 : 1) * ZOOM.value() * mapGuiRadius;
    }

    /** Square frame (the corners of the turned map are cut off) instead of the diamond; a client setting. */
    public static boolean isSquare() {
        return ReignOfNetherClientConfigs.SQUARE_MINIMAP.get();
    }

    /** The HUD minimap's zoom: 1 shows the whole map. */
    public static final MapZoom ZOOM = new MapZoom(1f).withWideStep(MinimapClientEvents::setLargeMap);

    private static void drawMapMarker(MapMarker marker) {
        int xc = marker.x;
        int zc = marker.z;
        int color = marker.type.rgb;
        float alphaPulse = MiscUtil.getOscillatingFloat(0.45d, 1.0d, marker.ageTicks * 35L);
        int alpha = Mth.clamp((int) (alphaPulse * 255f), 90, 255);

        for (int x = xc - PING_RADIUS - 1; x <= xc + PING_RADIUS + 1; x++) {
            for (int z = zc - PING_RADIUS - 1; z <= zc + PING_RADIUS + 1; z++) {
                if (isWorldXZinsideMap(x, z)) {
                    if (isMarkerPixel(marker.type, x - xc, z - zc)) {
                        int xN = x - xc_world + worldRadius + MARKER_PIXEL_OFFSET;
                        int zN = z - zc_world + worldRadius;
                        if (xN >= 0 && xN < mapColoursOverlays.length && zN >= 0 && zN < mapColoursOverlays[0].length) {
                            mapColoursOverlays[xN][zN] = MiscUtil.reverseHexRGB(color) | (alpha << 24);
                        }
                    }
                }
            }
        }
    }

    private static final int PING_RADIUS = 9;    // ping shapes, in map texture pixels
    private static final double PING_THICKNESS = 2.5;

    // each ping type has its own small symbol. The map is drawn rotated 45°,
    // so shapes are tested in screen-aligned axes (u, v); every symbol is symmetric, so the rotation's sign
    // doesn't matter. Same shapes as PingType.symbol: Help ◎ ring with a dot, Attack ⚔ X, Enemy ◇ diamond, Defend □ square
    static boolean isMarkerPixel(PingType type, int dx, int dz) {
        double u = (dx - dz) / Math.sqrt(2), v = (dx + dz) / Math.sqrt(2);
        double r = PING_RADIUS, t = PING_THICKNESS;
        double au = Math.abs(u), av = Math.abs(v);
        return switch (type) {
            case HELP -> {
                double d = Math.sqrt(u * u + v * v);
                yield (d > r - t && d <= r) || d <= t;
            }
            case ATTACK -> au <= r * 0.8 && av <= r * 0.8 && Math.abs(au - av) <= t * 0.6;
            case ENEMY -> au + av <= r && au + av > r - t;
            case DEFEND -> Math.max(au, av) <= r * 0.8 && Math.max(au, av) > r * 0.8 - t;
            case ON_MY_WAY -> au <= r * 0.8 && Math.abs(v) <= r * 0.8 - au; // a solid arrowhead
        };
    }


    // checks whether a given X Z in the world is part of our map
    public static boolean isWorldXZinsideMap(int x, int z) {
        return x >= xc_world - worldRadius && x < xc_world + worldRadius && z >= zc_world - worldRadius
            && z < zc_world + worldRadius;
    }

    private static void renderMap(GuiGraphics guiGraphics) {
        PoseStack stack = guiGraphics.pose();
        Matrix4f matrix4f = stack.last().pose();

        // diamond frame, the map turned 45 degrees like the camera view
        updateScreenGeometry();
        xc = mapRight - mapGuiRadius;
        yc = mapBottom - mapGuiRadius;

        if (isSquare()) {
            guiGraphics.enableScissor(Math.round(xc - mapGuiRadius), Math.round(yc - mapGuiRadius),
                    Math.round(xc + mapGuiRadius), Math.round(yc + mapGuiRadius));
            drawMapTexture(matrix4f, xc, yc, contentRadius(), 1);
            guiGraphics.disableScissor();
        } else {
            drawMapTexture(matrix4f, xc, yc, mapGuiRadius, ZOOM.value());
        }
        renderPingPulses(guiGraphics);
    }

    // a square outline growing and fading from each ping, once a second while it lasts
    private static void renderPingPulses(GuiGraphics g) {
        for (MapMarker m : mapMarkers) {
            Vec2 p = worldPosToMinimapScreen(m.x, m.z);
            if (!isPointInsideMinimap(p.x, p.y))
                continue;
            float t = (m.ageTicks % 20) / 20f;
            int s = Math.round(4 * (0.4f + 1.8f * t));
            int colour = ((int) ((1 - t) * 255) << 24) | m.type.rgb;
            int x = Math.round(p.x), y = Math.round(p.y);
            g.fill(x - s, y - s, x + s, y - s + 1, colour);
            g.fill(x - s, y + s - 1, x + s, y + s, colour);
            g.fill(x - s, y - s + 1, x - s + 1, y + s - 1, colour);
            g.fill(x + s - 1, y - s + 1, x + s, y + s - 1, colour);
        }
    }

    /**
     * Draws the map turned like the HUD minimap, centred in the square (x, y, size) in the current pose.
     * Zoom 1 shows the whole map, 2 fills the square like the HUD minimap; the caller clips to the square.
     */
    public static void renderSquare(GuiGraphics g, float x, float y, float size, float zoom) {
        drawMapTexture(g.pose().last().pose(), x + size / 2, y + size / 2, size / 2 * zoom, 1);
    }

    /** A diamond of half-diagonal d showing the middle 1/zoom of the map texture. */
    private static void drawMapTexture(Matrix4f matrix4f, float cx, float cy, float d, float zoom) {
        float lo = 0.5f - 0.5f / zoom, hi = 0.5f + 0.5f / zoom;
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer consumer = buffer.getBuffer(mapRenderType);
        consumer.vertex(matrix4f, cx, cy + d, 0.0F).color(255, 255, 255, 255).uv(lo, hi).uv2(255).endVertex();
        consumer.vertex(matrix4f, cx + d, cy, 0.0F).color(255, 255, 255, 255).uv(hi, hi).uv2(255).endVertex();
        consumer.vertex(matrix4f, cx, cy - d, 0.0F).color(255, 255, 255, 255).uv(hi, lo).uv2(255).endVertex();
        consumer.vertex(matrix4f, cx - d, cy, 0.0F).color(255, 255, 255, 255).uv(lo, lo).uv2(255).endVertex();
        buffer.endBatch();
    }

    public static boolean isPointInsideMinimap(double x, double y) {
        if (isSquare())
            return Math.abs(x - xc) <= mapGuiRadius && Math.abs(y - yc) <= mapGuiRadius;
        return Math.abs(x - xc) + Math.abs(y - yc) <= mapGuiRadius;
    }

    // given an x and y on the screen that the player clicked, return the world position of that spot
    public static BlockPos getWorldPosOnMinimap(float x, float y, boolean offsetForCamera) {
        if (!isPointInsideMinimap(x, y) || CursorClientEvents.isBoxSelecting() || MC.level == null) {
            return null;
        }

        return toWorldPos(x - xc, y - yc, contentRadius(), offsetForCamera);
    }

    /** The world position under (x, y) on a map drawn by renderSquare into the square at (left, top). */
    public static BlockPos getWorldPosOnSquare(float x, float y, float left, float top, float size, float zoom) {
        if (MC.level == null || x < left || y < top || x > left + size || y > top + size)
            return null;
        return toWorldPos(x - (left + size / 2), y - (top + size / 2), size / 2 * zoom, true);
    }

    // dx, dy from the centre of a diamond of half-diagonal d
    private static BlockPos toWorldPos(float dx, float dy, float d, boolean offsetForCamera) {
        double xWorld = xc_world + (dx - dy) / d * worldRadius;
        double zWorld = zc_world + (dx + dy) / d * worldRadius;

        // offset so the click lands on the centre of the view quad instead of its bottom edge
        if (offsetForCamera) {
            double angleRad = Math.toRadians(OrthoviewClientEvents.getCamRotX() - 315.0);
            Vec2 dir = MyMath.rotateCoords((float) Math.cos(angleRad), (float) Math.sin(angleRad), 45);
            double blocks = OrthoviewClientEvents.getZoom() * 0.5 * Math.sqrt(2);
            xWorld -= dir.x * blocks;
            zWorld -= dir.y * blocks;
        }
        int roundedX = Mth.floor(xWorld + 0.5d);
        int roundedZ = Mth.floor(zWorld + 0.5d);
        int roundedY = MiscUtil.getHighestNonAirBlock(MC.level, new BlockPos(roundedX, 0, roundedZ)).getY();

        return new BlockPos(roundedX, roundedY, roundedZ);
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre evt) {
        if (!OrthoviewClientEvents.isEnabled() ||
                OrthoviewClientEvents.isCameraLocked() ||
                HudClientEvents.isMouseOverAnyButton() ||
                !(MC.screen instanceof TopdownGui)) {
            return;
        }

        if (evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_1 &&
            !Keybindings.shiftMod.isDown() && !OrthoviewClientEvents.isCameraLocked() &&
            lastDragTeleportTimestamp < System.currentTimeMillis() - 100) {

            lastDragTeleportTimestamp = System.currentTimeMillis();
            BlockPos moveTo = getWorldPosOnMinimap((float) evt.getMouseX(), (float) evt.getMouseY(), true);
            if (MC.player != null && moveTo != null) {
                PlayerServerboundPacket.teleportPlayer(
                    (double) moveTo.getX(),
                    MC.player.getY(),
                    (double) moveTo.getZ()
                );
            }
        }
        else if (evt.getMouseButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
			if (!minimapRightClickDown || Keybindings.altMod.isDown() || UnitClientEvents.getSelectedUnits().isEmpty()) return;
            BlockPos currentPos = getWorldPosOnMinimap((float) evt.getMouseX(), (float) evt.getMouseY(), false);
            if (currentPos == null) return;

            if (minimapDragStartBp == null) {
                minimapDragStartBp = currentPos;
                FormationDragMove.startDrag(currentPos);
            }

            if (FormationDragMove.isDragging()) {
                FormationDragMove.updateDrag(currentPos, UnitClientEvents.getSelectedUnits().size(), MC.level);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre evt) {
        if (!OrthoviewClientEvents.isEnabled() ||
            OrthoviewClientEvents.isCameraLocked() ||
            !(MC.screen instanceof TopdownGui)) {
            return;
        }
        boolean altDown = Keybindings.altMod.isDown();

        // when clicking on map move player there
        Button startPosButton = getMousedOverStartPosButton();
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1 && (!isMouseOverAnyButton() || startPosButton != null)) {
            BlockPos moveTo = startPosButton != null ?
                    getWorldPosOnMinimap((float) HudGeometry.scale().toGui(startPosButton.x + startPosButton.drawnSize / 2.0),
                            (float) HudGeometry.scale().toGui(startPosButton.y + startPosButton.drawnSize / 2.0), true) :
                    getWorldPosOnMinimap((float) evt.getMouseX(), (float) evt.getMouseY(), true);

            if (MC.player != null && moveTo != null) {
                if (!altDown && Keybindings.shiftMod.isDown()) {
                    setMapCentre(moveTo.getX(), moveTo.getZ());
                    forceUpdateAllPartitions = true;
                    TutorialClientEvents.clickedMinimap = true;
                    PlayerServerboundPacket.teleportPlayer(
                        (double) moveTo.getX(),
                        MC.player.getY(),
                        (double) moveTo.getZ()
                    );
                } else if (!altDown) {
                    TutorialClientEvents.clickedMinimap = true;
                    PlayerServerboundPacket.teleportPlayer(
                        (double) moveTo.getX(),
                        MC.player.getY(),
                        (double) moveTo.getZ()
                    );
            }
            }
        }
		if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
			minimapRightClickDown = isPointInsideMinimap(evt.getMouseX(), evt.getMouseY());
		}
    }

    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseButtonReleased.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() ||
            OrthoviewClientEvents.isCameraLocked() ||
            !(MC.screen instanceof TopdownGui)) {
            return;
        }

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            if (FormationDragMove.isDragging()) {
                var pairs = FormationDragMove.endDrag(UnitClientEvents.getSelectedUnits());
                for (var pair : pairs) {
                    LivingEntity le = pair.getFirst();
                    BlockPos targetBp = pair.getSecond();
                    if (le instanceof Unit unit) {
					int[] singleUnitId = new int[]{le.getId()};
					boolean queueOrders = Keybindings.shiftMod.isDown();
					if (!queueOrders) {
						new UnitActionItem(
							PlayerRolesClient.myOwner(),
							UnitAction.MOVE, -1, singleUnitId,
							targetBp,
							new BlockPos(0, 0, 0)
						).action(MC.level);
					} else {
						MiscUtil.addUnitCheckpoint(unit, targetBp, true);
					}
					PacketHandler.INSTANCE.sendToServer(new UnitActionServerboundPacket(
						PlayerRolesClient.myOwner(),
						UnitAction.MOVE, -1, singleUnitId,
						targetBp,
						new BlockPos(0, 0, 0),
						queueOrders
					));
                }
			}
                minimapDragStartBp = null;
            } else {
                BlockPos moveTo = getWorldPosOnMinimap((float) evt.getMouseX(), (float) evt.getMouseY(), false);
                if (moveTo == null) return;
                if (!UnitClientEvents.getSelectedUnits().isEmpty()) {
                    var ids = UnitClientEvents.getSelectedUnits();
                    var idArray = ArrayUtil.livingEntityListToIdArray(ids);
                    UnitClientEvents.sendUnitCommandManual(UnitAction.MOVE, -1, idArray, moveTo);
                }
            }
		    minimapRightClickDown = false;
        }
    }

    /** Size and corner of the map on screen, taken from the HUD layout so it always sits in the console. */
    private static void updateScreenGeometry() {
        float[] r = HudGeometry.toGui(HudGeometry.layout().minimap());
        float size = r[2];
        mapGuiRadius = Math.max(1, Math.round(size / 2));
        mapRight = r[0] + r[2];
        mapBottom = r[1] + r[3];
    }

    /** Drawn by the HUD, after the console background and before the panels. */
    public static void render(GuiGraphics guiGraphics) {
        if (!OrthoviewClientEvents.isEnabled() || MC.isPaused() || !HudClientEvents.enabled
            || !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK) || MC.screen instanceof MatchStartScreen) {
            return;
        }

        renderMap(guiGraphics);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        boolean soldier = PlayerRolesClient.myRole() == PlayerRole.SOLDIER; // their HUD has a minimap too
        if (evt.phase != TickEvent.Phase.END || !(OrthoviewClientEvents.isEnabled() || soldier))
            return;
        update(soldier);
    }

    /** Keeps the map updating for a menu that shows it (the saved game resume screen) when orthoview is off. */
    public static void tickForScreen() {
        if (!OrthoviewClientEvents.isEnabled())
            update(false);
    }

    private static void update(boolean soldier) {

        updateMapTerrain(terrainPartition, darkTerrainPartition);
        if (mapColoursOverlays.length != worldRadius * 2)
            mapColoursOverlays = new int[worldRadius * 2][worldRadius * 2];
        else
            for (int[] row : mapColoursOverlays)
                Arrays.fill(row, 0);
        if (BlockClientEvents.nightCircleMode != NightCircleMode.OFF)
            updateNightCircles();
        
        // Update map markers
        mapMarkers.removeIf(MapMarker::tick);

        updateMapUnitsAndBuildings();
        if (!suppressViewQuad && !soldier)
            mapColoursOverlays = MinimapViewQuadRenderer.updateMapViewQuad(mapColoursOverlays);

        // as the map area increases, decrease refresh rate to maintain FPS
        terrainPartition += 1;
        if (terrainPartition > TERRAIN_PARTITIONS_MAX) {
            terrainPartition = 1;

            darkTerrainPartition += 1;
            if (darkTerrainPartition > DARK_TERRAIN_PARTITIONS_MAX) {
                darkTerrainPartition = 1;
            }
        }
        updateMapTexture();
    }
}