package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.solegendary.reignofnether.fogofwar.FogOfWarServerboundPacket.setServerFog;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class FogOfWarClientEvents {

    // RGB multiplier baked into block vertex colors in dark chunks (see BlockColorsMixin).
    public static final int FOG_TINT_RGB = 0x7882A0;

    private static final Minecraft MC = Minecraft.getInstance();

    // mirror of the server's authoritative sent (bright) set for this player
    public static final Set<ChunkPos> brightChunks = ConcurrentHashMap.newKeySet();

    // Per-edge-chunk 16x16 column visibility bitmask (long[4] = 256 bits, index = (localX<<4)|localZ),
    // server-authoritative: arrives with the bright set in FogChunksClientboundPacket, so the tint circle
    // always matches the data/entity gating the server applied. A bright chunk with no entry is fully
    // visible (live). Published as an immutable snapshot via a volatile ref so the off-thread chunk mesher
    // reads it lock-free.
    private static volatile Map<ChunkPos, long[]> edgeMasks = Map.of();

    // if false, disables ALL mixins related to fog of war
    private static boolean enabled = false;

    // cached - isEnabled() runs per quad during chunk mesh rebuilds
    private static volatile boolean localSeesFog = false;

    // Mirror of the server's per-spectator fog opt-out (FogViewClientboundPacket). Never applies to RTS players.
    private static boolean spectatorFogHidden = false;

    // fog shows for everyone once any RTS player exists (non-RTS players see the combined vision of all of them)
    // unless a spectator has hidden it for themselves
    public static void refreshLocalSeesFog() {
        boolean was = localSeesFog;
        localSeesFog = MC.player != null && !PlayerClientEvents.rtsPlayers.isEmpty()
                && !(spectatorFogHidden && !PlayerClientEvents.isRTSPlayer());
        if (was != localSeesFog && enabled) resetFogChunks();
    }

    public static void setSpectatorFogHidden(boolean hidden) {
        spectatorFogHidden = hidden;
        if (hidden) { // the server stops sending vision updates, so don't keep stale ones around
            brightChunks.clear();
            edgeMasks = Map.of();
        }
        refreshLocalSeesFog();
    }

    public static void resetSpectatorFogHidden() {
        spectatorFogHidden = false;
    }

    public static boolean isSpectatorFogHidden() {
        return spectatorFogHidden && !PlayerClientEvents.isRTSPlayer();
    }

    // fog is on server-side and there is something to see through (the toggle button is pointless otherwise)
    public static boolean canToggleSpectatorFog() {
        return enabled && !PlayerClientEvents.rtsPlayers.isEmpty() && !PlayerClientEvents.isRTSPlayer();
    }

    public static Button getSpectatorFogButton() {
        boolean hidden = isSpectatorFogHidden();
        return new Button("Toggle Fog (spectator)",
                Button.DEFAULT_ICON_SIZE,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/glass.png"),
                null,
                () -> hidden,
                () -> !canToggleSpectatorFog(),
                () -> true,
                () -> FogViewServerboundPacket.setFogHidden(!hidden),
                null,
                List.of(fcs(I18n.get(hidden
                        ? "fogofwar.reignofnether.spectator_fog_hidden"
                        : "fogofwar.reignofnether.spectator_fog_shown")))
        );
    }

    public static boolean movedToCapitol = false;

    public static void applyServerFogState(Set<ChunkPos> bright, Map<ChunkPos, long[]> masks) {
        Set<ChunkPos> newlyDark = new HashSet<>(brightChunks);
        newlyDark.removeAll(bright);
        Set<ChunkPos> newlyBright = new HashSet<>(bright);
        newlyBright.removeAll(brightChunks);

        brightChunks.clear();
        brightChunks.addAll(bright);

        // Publish the server's edge masks atomically with the bright set (same packet), so there is never
        // a window where a chunk is bright but its mask hasn't caught up.
        Map<ChunkPos, long[]> oldMasks = edgeMasks;
        edgeMasks = masks;

        // collect the union of (flipped chunk + 8 neighbors) so adjacent flips don't
        // re-dirty the same sections 9× each
        Set<ChunkPos> toRerender = new HashSet<>();
        for (ChunkPos cpos : newlyDark) addWithNeighbors(toRerender, cpos);
        for (ChunkPos cpos : newlyBright) {
            addWithNeighbors(toRerender, cpos);
            if (MC.level != null) {
                for (BuildingPlacement building : BuildingClientEvents.getBuildings()) {
                    if (building.isExploredClientside) continue;
                    for (BlockPos bp : building.getRenderChunkOrigins(false))
                        if (MC.level.getChunk(bp).getPos().equals(cpos))
                            building.isExploredClientside = true;
                }
            }
        }
        // re-mesh chunks whose mask changed (with neighbors: border columns affect adjacent chunks' tint
        // sampling in BiomeColorsMixin)
        for (ChunkPos cpos : diffMasks(oldMasks, masks)) addWithNeighbors(toRerender, cpos);
        markSectionsDirty(toRerender);
    }

    private static void addWithNeighbors(Set<ChunkPos> set, ChunkPos cpos) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                set.add(new ChunkPos(cpos.x + dx, cpos.z + dz));
    }

    private static void markSectionsDirty(Set<ChunkPos> chunks) {
        if (MC.level == null || MC.levelRenderer == null || chunks.isEmpty()) return;
        int minSection = MC.level.getMinSection();
        int maxSection = MC.level.getMaxSection();
        for (ChunkPos cpos : chunks) {
            for (int y = minSection; y < maxSection; y++) {
                MC.levelRenderer.setSectionDirty(cpos.x, y, cpos.z);
            }
        }
    }

    // Block-level visibility used by the fog tint hooks (FogTintingBlockColor, BiomeColorsMixin,
    // LiquidBlockRendererMixin) in place of the old whole-chunk brightChunks test. Called from the
    // off-thread chunk mesher, so it only reads immutable published state. O(1).
    public static boolean isBlockVisible(BlockPos pos) {
        return isBlockVisible(pos.getX(), pos.getZ());
    }

    public static boolean isBlockVisible(int x, int z) {
        if (!enabled) return true;
        ChunkPos cp = new ChunkPos(x >> 4, z >> 4);
        if (!brightChunks.contains(cp)) return false;              // server-dark chunk: fully fogged
        long[] mask = edgeMasks.get(cp);                            // single volatile read
        if (mask == null) return true;                             // live chunk (no mask): fully visible
        int i = ((x & 15) << 4) | (z & 15);
        return (mask[i >> 6] & (1L << (i & 63))) != 0;
    }

    private static final long[] ALL_FOGGED = new long[4]; // read-only; every column masked

    // Chunks whose most recent chunk packet created them from scratch. The light engine holds empty section
    // data for these, so incoming light has to be adopted wholesale - merging against it freezes black.
    private static final Set<ChunkPos> freshlyLoaded = ConcurrentHashMap.newKeySet();

    public static void setChunkFreshlyLoaded(int chunkX, int chunkZ, boolean fresh) {
        ChunkPos cp = new ChunkPos(chunkX, chunkZ);
        if (fresh) freshlyLoaded.add(cp); else freshlyLoaded.remove(cp);
    }

    // Column mask for merging incoming light: null = adopt server light untouched (fog off, or a fully
    // visible chunk), otherwise a 0 column keeps whatever light the client already has.
    public static long[] getLightMask(int chunkX, int chunkZ) {
        if (!isEnabled()) return null;
        ChunkPos cp = new ChunkPos(chunkX, chunkZ);
        if (freshlyLoaded.contains(cp)) return null;   // nothing remembered to preserve
        if (!brightChunks.contains(cp)) return ALL_FOGGED;
        return edgeMasks.get(cp);
    }

    // Column-visibility mask if this chunk is an edge chunk, else null. Used by ClientChunkCacheMixin to
    // keep fogged columns rendering their previous state when the server resends a full edge chunk.
    public static long[] getEdgeMask(int chunkX, int chunkZ) {
        if (!isEnabled()) return null;
        return edgeMasks.get(new ChunkPos(chunkX, chunkZ));
    }

    // Chunks whose mask differs (added, removed, or bits changed) need a re-mesh.
    private static Set<ChunkPos> diffMasks(Map<ChunkPos, long[]> oldM, Map<ChunkPos, long[]> newM) {
        Set<ChunkPos> changed = new HashSet<>();
        for (Map.Entry<ChunkPos, long[]> e : newM.entrySet()) {
            long[] o = oldM.get(e.getKey());
            if (o == null || !Arrays.equals(o, e.getValue())) changed.add(e.getKey());
        }
        for (ChunkPos cp : oldM.keySet())
            if (!newM.containsKey(cp)) changed.add(cp); // removed -> reverts to full-bright fallback
        return changed;
    }

    // Chunks that are entirely missing from the client's chunk cache are re-requested from the server (see
    // FogChunkRequestServerboundPacket). Fully bright chunks are never requested - vanilla chunk tracking
    // delivers those - only dark and edge ones.
    private static final int CHUNK_SCAN_TICKS = 20;
    private static final int MAX_CHUNK_REQUESTS_PER_SCAN = 8; // per CHUNK_SCAN_TICKS, ie. per second
    private static final int CHUNK_REQUEST_GRACE_TICKS = 60; // a chunk must stay missing this long before asking, as it may just be in transit
    private static final int CHUNK_REQUEST_COOLDOWN_TICKS = 100; // before asking again for a chunk the server didn't deliver

    // missing chunk -> client tick at which it may next be requested
    private static final Map<ChunkPos, Long> nextChunkRequestTick = new HashMap<>();
    private static long clientTicks = 0;

    public static void resetChunkRequests() {
        nextChunkRequestTick.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;
        if (MC.level == null || MC.player == null || !isEnabled()) {
            resetChunkRequests();
            return;
        }
        clientTicks++;
        if (clientTicks % CHUNK_SCAN_TICKS != 0) return;

        ChunkPos center = MC.player.chunkPosition();
        int r = MC.options.getEffectiveRenderDistance();
        Map<ChunkPos, long[]> masks = edgeMasks;
        Set<ChunkPos> missing = new HashSet<>();
        List<ChunkPos> due = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                if (MC.level.getChunkSource().getChunk(cp.x, cp.z, ChunkStatus.FULL, false) != null) continue;
                if (brightChunks.contains(cp) && !masks.containsKey(cp)) continue; // fully bright
                missing.add(cp);
                Long next = nextChunkRequestTick.get(cp);
                if (next == null)
                    nextChunkRequestTick.put(cp, clientTicks + CHUNK_REQUEST_GRACE_TICKS);
                else if (clientTicks >= next)
                    due.add(cp);
            }
        }
        nextChunkRequestTick.keySet().retainAll(missing); // forget chunks that have arrived or left range

        // nearest first
        due.sort(Comparator.comparingInt(cp -> Math.max(Math.abs(cp.x - center.x), Math.abs(cp.z - center.z))));
        if (due.size() > MAX_CHUNK_REQUESTS_PER_SCAN)
            due = due.subList(0, MAX_CHUNK_REQUESTS_PER_SCAN);
        for (ChunkPos cp : due)
            nextChunkRequestTick.put(cp, clientTicks + CHUNK_REQUEST_COOLDOWN_TICKS);
        FogChunkRequestServerboundPacket.requestChunks(due);
    }

    @SubscribeEvent
    // can't use ScreenEvent.KeyboardKeyPressedEvent as that only happens when a screen is up
    public static void onInput(InputEvent.Key evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions
            if (MC.player == null)
                return;
            if (!MC.player.hasPermissions(4))
                return;

            // resetFogChunks
            if (Keybindings.shiftMod.isDown() && evt.getKey() == Keybindings.getFnum(8).getKey() && isEnabled()) {
                resetFogChunks();
            }
        }
    }

    // reload chunks like player pressed F3 + A
    public static void resetFogChunks() {
        MC.levelRenderer.allChanged();
    }

    public static void setEnabled(boolean value) {
        if (MC.player == null)
            return;

        if (enabled != value) {
            enabled = value;
            resetFogChunks();

            if (!enabled) {
                brightChunks.clear();
                edgeMasks = Map.of();
            }
        }
    }

    public static boolean isEnabled() {
        return enabled && localSeesFog;
    }

    // client-side mirror of WorldBorderServerEvents.isRtsOptimisedMap using the client's known border
    public static boolean isOnRtsOptimisedMap() {
        return MC.level != null
                && MC.level.getWorldBorder().getSize() <= WorldBorderServerEvents.RTS_OPTIMIZED_BORDER;
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterClientCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("rts-fog").then(Commands.literal("enable")
                .executes((command) -> {
                    if (MC.player == null)
                        return -1;
                    if (!MC.player.hasPermissions(4))
                        return -1;
                    // Fog only works on RTS-optimised maps; refuse locally so the toggle is effectively
                    // hidden on vanilla-sized maps instead of bouncing off the server.
                    if (!isOnRtsOptimisedMap()) {
                        MC.player.sendSystemMessage(Component.literal(
                                I18n.get("server.reignofnether.fog_requires_rts_map")));
                        return -1;
                    }
                    setServerFog(true);
                    return 1;
                })));
        evt.getDispatcher().register(Commands.literal("rts-fog").then(Commands.literal("disable")
                .executes((command) -> {
                    if (MC.player == null)
                        return -1;
                    if (!MC.player.hasPermissions(4))
                        return -1;
                    setServerFog(false);
                    return 1;
                })));
    }

    public static boolean isBuildingInBrightChunk(BuildingPlacement building) {
        if (!isEnabled())
            return true;

        for (BlockPos bp : BuildingUtils.getUniqueChunkBps(building))
            if (isInBrightChunk(bp))
                return true;

        return false;
    }

    // Kept named isInBrightChunk for its many callers, but now block-granular: a position is "bright" only
    // if visible at block level, so entities/items/particles hide inside the fogged part of an edge chunk
    // (own + allied units are viewers, always inside their own circle, so they never self-hide).
    public static boolean isInBrightChunk(BlockPos bp) {
        if (!isEnabled() || MC.level == null)
            return true;
        return isBlockVisible(bp);
    }

    public static boolean isInBrightChunk(Entity entity) {
        if (!isEnabled() || MC.level == null)
            return true;

        if (isBlockVisible(entity.getOnPos())) return true;

        return entity instanceof RangedAttackerUnit rangedAttackerUnit &&
                rangedAttackerUnit.getFogRevealDuration() > 0;
    }

    @SubscribeEvent
    // hudSelectedEntity and portraitRendererUnit should be assigned in the same event to avoid desyncs
    public static void onRenderLivingEntity(RenderLivingEvent.Pre<? extends LivingEntity, ? extends Model> evt) {
        if (MC.level != null && !MC.level.getWorldBorder().isWithinBounds(evt.getEntity().getOnPos())) {
            evt.setCanceled(true);
            return;
        }
        // don't render entities in non-bright chunks or outside of world border
        if (isInBrightChunk(evt.getEntity()))
            return;

        evt.setCanceled(true);
    }

    public static void setBuildingDestroyedServerside(BlockPos buildingOrigin) {
        for (BuildingPlacement building : BuildingClientEvents.getBuildings())
            if (building.originPos.equals(buildingOrigin))
                building.isDestroyedServerside = true;
    }

    public static void setBuildingBuiltServerside(BlockPos buildingOrigin) {
        for (BuildingPlacement building : BuildingClientEvents.getBuildings())
            if (building.originPos.equals(buildingOrigin))
                building.isBuiltServerside = true;
    }

    public static void revealRangedUnit(String playerBeingAttacked, int unitId) {
        if (MC.player != null && MC.player.getName().getString().equals(playerBeingAttacked))
            for (LivingEntity entity : UnitClientEvents.getAllUnits())
                if (entity.getId() == unitId && entity instanceof RangedAttackerUnit unit)
                    unit.setFogRevealDuration(RangedAttackerUnit.FOG_REVEAL_TICKS_MAX);
    }

}
