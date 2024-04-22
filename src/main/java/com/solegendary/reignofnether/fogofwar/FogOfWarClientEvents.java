package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.solegendary.reignofnether.fogofwar.FogOfWarServerboundPacket.setServerFog;

public class FogOfWarClientEvents {
    public static final float BRIGHT = 1.0f;
    public static final float DARK = 0.35f;
    public static final int CHUNK_VIEW_DIST = 1;
    public static final int CHUNK_FAR_VIEW_DIST = 2;
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int UPDATE_TICKS_MAX = 10;
    private static int updateTicksLeft = UPDATE_TICKS_MAX;

    public static final Set<ChunkPos> brightChunks = ConcurrentHashMap.newKeySet();
    public static final Set<ChunkPos> lastBrightChunks = ConcurrentHashMap.newKeySet();
    public static final Set<ChunkPos> rerenderChunks = ConcurrentHashMap.newKeySet();

    // chunks that will not update while we are looking at them
    public static final Set<BlockPos> semiFrozenChunks = ConcurrentHashMap.newKeySet();

    // chunkInfos that should never be updated, even if the client does a reset or moves the camera out of range
    public static final Set<FrozenChunk> frozenChunks = ConcurrentHashMap.newKeySet();

    // have we already warned the client about using Optifine?
    public static boolean fogOptifineWarningSent = false;

    // if false, disables ALL mixins related to fog of war
    private static boolean enabled = false;

    private static final Set<String> revealedPlayerNames = ConcurrentHashMap.newKeySet();


    public static void revealOrHidePlayer(boolean reveal, String playerName) {
        if (reveal)
            revealedPlayerNames.add(playerName);
        else
            revealedPlayerNames.removeIf(p -> p.equals(playerName));
    }

    public static boolean isPlayerRevealed(String name) {
        if (!PlayerClientEvents.isRTSPlayer)
            return true;

        return revealedPlayerNames.contains(name);
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
            if (evt.getKey() == Keybindings.getFnum(8).key && isEnabled()) {
                resetFogChunks();
            }
        }
    }

    // reload chunks like player pressed F3 + A
    public static void resetFogChunks() {
        MC.levelRenderer.allChanged();
        semiFrozenChunks.clear();
        frozenChunks.clear();
    }

    public static void setEnabled(boolean value) {
        if (MC.player == null)
            return;

        if (enabled != value) {
            enabled = value;
            resetFogChunks();
        }
    }

    public static boolean isEnabled() {
        if (ResearchClient.hasCheat("iseedeadpeople"))
            return false;
        return enabled;
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterClientCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("fog").then(Commands.literal("enable")
                .executes((command) -> {
                    if (MC.player == null)
                        return -1;
                    if (!MC.player.hasPermissions(4))
                        return -1;
                    if (!fogOptifineWarningSent) {
                        fogOptifineWarningSent = true;
                        MC.player.sendSystemMessage(Component.literal(""));
                        MC.player.sendSystemMessage(Component.literal("[WARNING]").withStyle(Style.EMPTY.withBold(true)));
                        MC.player.sendSystemMessage(Component.literal(
                                "If any players have rendering optimisation mods such as Optifine installed, enabling fog of war " +
                                "may cause them to crash. If you are prepared for this, then use /fog enable again to continue."));
                    } else {
                        setServerFog(true);
                    }
                    return 1;
                })));
        evt.getDispatcher().register(Commands.literal("fog").then(Commands.literal("disable")
                .executes((command) -> {
                    if (MC.player == null)
                        return -1;
                    if (!MC.player.hasPermissions(4))
                        return -1;
                    setServerFog(false);
                    return 1;
                })));
    }

    // returns the shade modifier that should be applied at a given position based on the fog of war state there
    public static float getPosBrightness(BlockPos pPos) {
        if (!isEnabled() || MC.level == null)
            return BRIGHT;

        // first check if the ChunkPos is already occupied as this is faster
        for (ChunkPos chunkPos : brightChunks)
            if (new ChunkPos(pPos).equals(chunkPos))
                return BRIGHT;

        return DARK;
    }

    public static boolean isBuildingInBrightChunk(Building building) {
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

        // first check if the ChunkPos is already occupied as this is faster
        for (ChunkPos chunkPos : brightChunks)
            if (new ChunkPos(bp).equals(chunkPos))
                return true;

        return false;
    }

    @SubscribeEvent
    // hudSelectedEntity and portraitRendererUnit should be assigned in the same event to avoid desyncs
    public static void onRenderLivingEntity(RenderLivingEvent.Pre<? extends LivingEntity, ? extends Model> evt) {
        if (!isEnabled())
            return;

        // don't render entities in non-bright chunks
        if (isInBrightChunk(evt.getEntity().getOnPos()))
            return;

        evt.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (!isEnabled() || MC.level == null || MC.player == null || evt.phase != TickEvent.Phase.END)
            return;

        if (updateTicksLeft > 0) {
            updateTicksLeft -= 1;
        } else {
            updateTicksLeft = UPDATE_TICKS_MAX;
            brightChunks.clear();
            Set<ChunkPos> occupiedChunks = ConcurrentHashMap.newKeySet();
            Set<ChunkPos> occupiedFarviewChunks = ConcurrentHashMap.newKeySet();

            // get chunks that have units/buildings that can see
            for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                if (UnitClientEvents.getPlayerToEntityRelationship(entity) == Relationship.OWNED ||
                    (entity instanceof Unit unit && isPlayerRevealed(unit.getOwnerName()))) {
                    if (entity instanceof GhastUnit)
                        occupiedFarviewChunks.add(new ChunkPos(entity.getOnPos()));
                    else
                        occupiedChunks.add(new ChunkPos(entity.getOnPos()));
                }
            }
            for (Building building : BuildingClientEvents.getBuildings()) {
                if (BuildingClientEvents.getPlayerToBuildingRelationship(building) == Relationship.OWNED ||
                    isPlayerRevealed(building.ownerName)) {
                    if ((building instanceof GarrisonableBuilding && GarrisonableBuilding.getNumOccupants(building) > 0 && building.isBuilt) ||
                        building.isCapitol)
                        occupiedFarviewChunks.add(new ChunkPos(building.centrePos));
                    else
                        occupiedChunks.add(new ChunkPos(building.centrePos));
                }
            }

            for (ChunkPos chunkPos : occupiedChunks)
                for (int x = -CHUNK_VIEW_DIST; x <= CHUNK_VIEW_DIST; x++)
                    for (int z = -CHUNK_VIEW_DIST; z <= CHUNK_VIEW_DIST; z++)
                        brightChunks.add(new ChunkPos(chunkPos.x + x, chunkPos.z + z));

            for (ChunkPos chunkPos : occupiedFarviewChunks)
                for (int x = -CHUNK_FAR_VIEW_DIST; x <= CHUNK_FAR_VIEW_DIST; x++)
                    for (int z = -CHUNK_FAR_VIEW_DIST; z <= CHUNK_FAR_VIEW_DIST; z++)
                        brightChunks.add(new ChunkPos(chunkPos.x + x, chunkPos.z + z));

            Set<ChunkPos> newlyDarkChunks = ConcurrentHashMap.newKeySet();
            newlyDarkChunks.addAll(lastBrightChunks);
            newlyDarkChunks.removeAll(brightChunks);

            for (ChunkPos cpos : newlyDarkChunks) {
                onChunkUnexplore(cpos);
                for (int x = -1; x <= 1; x++)
                    for (int z = -1; z <= 1; z++)
                        rerenderChunks.add(new ChunkPos(cpos.x + x, cpos.z + z));
            }
            Set<ChunkPos> newlyBrightChunks = ConcurrentHashMap.newKeySet();
            newlyBrightChunks.addAll(brightChunks);
            newlyBrightChunks.removeAll(lastBrightChunks);

            for (ChunkPos cpos : newlyBrightChunks)
                onChunkExplore(cpos);

            if (OrthoviewClientEvents.isEnabled())
                semiFrozenChunks.removeIf(bp -> bp.offset(8,8,8)
                        .distSqr(MC.player.getOnPos()) > Math.pow(OrthoviewClientEvents.getZoom() * 2, 2));
            else
                semiFrozenChunks.removeIf(bp -> bp.offset(8,8,8)
                        .distSqr(MC.player.getOnPos()) > Math.pow(MC.levelRenderer.getLastViewDistance() * 8, 2));
            semiFrozenChunks.removeIf(bp -> {
                if (isInBrightChunk(bp)) {
                    updateChunkLighting(bp);
                    return true;
                }
                return false;
            });

            lastBrightChunks.clear();
            lastBrightChunks.addAll(brightChunks);
        }
    }

    public static void updateChunkLighting(BlockPos originBp) {
        if (MC.level == null)
            return;

        for (int i = 0; i < 4; i++) {
            BlockPos updatePos = originBp.offset(4*i, 0, 4*i);
            for (int y = MC.level.getMaxBuildHeight(); y > MC.level.getMinBuildHeight(); y -= 1) {
                BlockPos bp = new BlockPos(updatePos.getX(), y, updatePos.getZ());
                BlockState bs = MC.level.getBlockState(bp);
                if (!bs.isAir()) {
                    MC.level.setBlockAndUpdate(bp, Blocks.GLOWSTONE.defaultBlockState());
                    MC.level.setBlockAndUpdate(bp, bs);
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChunkEvent(ChunkEvent.Load evt) {
        for (FrozenChunk frozenChunk : frozenChunks)
            if (MC.level.getChunk(frozenChunk.origin).getPos().equals(evt.getChunk().getPos()))
                frozenChunk.loadBlocks();
    }

    // triggered when a chunk goes from dark to bright
    public static void onChunkExplore(ChunkPos cpos) {
        System.out.println("explored: " + cpos);

        for (FrozenChunk frozenChunk : frozenChunks)
            if (MC.level.getChunk(frozenChunk.origin).getPos().equals(cpos))
                frozenChunk.syncServerBlocks(frozenChunk.origin);
    }

    // triggered when a chunk goes from bright to dark
    public static void onChunkUnexplore(ChunkPos cpos) {
        System.out.println("unexplored: " + cpos);

        for (FrozenChunk frozenChunk : frozenChunks)
            if (MC.level.getChunk(frozenChunk.origin).getPos().equals(cpos))
                frozenChunk.saveBlocks();
    }

    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        if (MC.level == null)
            return;

        ChunkPos cpos = MC.level.getChunk(CursorClientEvents.getPreselectedBlockPos()).getPos();

        MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
                "x: " + cpos.x,
                "z: " + cpos.z,
                "xo: " + cpos.getWorldPosition().getX(),
                "zo: " + cpos.getWorldPosition().getZ()
        });
    }
}
