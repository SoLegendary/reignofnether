package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.solegendary.reignofnether.fogofwar.FogOfWarServerboundPacket.setServerFog;

public class FogOfWarClientEvents {

    // RGB multiplier baked into block vertex colors in dark chunks (see BlockColorsMixin).
    public static final int FOG_TINT_RGB = 0x7882A0;

    public static final int CHUNK_VIEW_DIST = 1;
    public static final int CHUNK_FAR_VIEW_DIST = 2;
    private static final Minecraft MC = Minecraft.getInstance();

    // mirror of the server's authoritative bright set for this player
    public static final Set<ChunkPos> brightChunks = ConcurrentHashMap.newKeySet();

    // if false, disables ALL mixins related to fog of war
    private static boolean enabled = false;

    // cached - isEnabled() runs per quad during chunk mesh rebuilds
    private static volatile boolean localIsRTSPlayer = false;

    public static void refreshLocalIsRTSPlayer() {
        boolean was = localIsRTSPlayer;
        localIsRTSPlayer = MC.player != null && PlayerClientEvents.isRTSPlayer();
        if (was != localIsRTSPlayer && enabled) resetFogChunks();
    }

    public static boolean movedToCapitol = false;

    public static void applyServerFogState(Set<ChunkPos> bright) {
        Set<ChunkPos> newlyDark = new HashSet<>(brightChunks);
        newlyDark.removeAll(bright);
        Set<ChunkPos> newlyBright = new HashSet<>(bright);
        newlyBright.removeAll(brightChunks);

        brightChunks.clear();
        brightChunks.addAll(bright);

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

    @SubscribeEvent
    // can't use ScreenEvent.KeyboardKeyPressedEvent as that only happens when a screen is up
    public static void onInput(InputEvent.Key evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions
            if (MC.player == null)
                return;
            if (!MC.player.hasPermissions(4))
                return;

            // resetFogChunks
            if (evt.getKey() == Keybindings.getFnum(8).getKey() && isEnabled()) {
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
            }
        }
    }

    public static boolean isEnabled() {
        return enabled && localIsRTSPlayer;
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

    public static boolean isInBrightChunk(BlockPos bp) {
        if (!isEnabled() || MC.level == null)
            return true;
        return brightChunks.contains(new ChunkPos(bp));
    }

    public static boolean isInBrightChunk(Entity entity) {
        if (!isEnabled() || MC.level == null)
            return true;

        if (brightChunks.contains(new ChunkPos(entity.getOnPos()))) return true;

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
