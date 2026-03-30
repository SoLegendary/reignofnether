package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.gamemode.GameModeServerboundPacket;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.survival.SurvivalClientEvents;
import com.solegendary.reignofnether.survival.SurvivalServerboundPacket;
import com.solegendary.reignofnether.survival.WaveDifficulty;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class PlayerServerboundPacket {
    PlayerAction action;
    public int playerId;
    public double x;
    public double y;
    public double z;

    public static void teleportPlayer(Double x, Double y, Double z) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(PlayerAction.TELEPORT,
                MC.player.getId(),
                x,
                y,
                z
            ));
        }
    }

    public static void enableOrthoview() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(PlayerAction.ENABLE_ORTHOVIEW,
                MC.player.getId(),
                0d,
                0d,
                0d
            ));
        }
    }

    public static void disableOrthoview() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(PlayerAction.DISABLE_ORTHOVIEW,
                MC.player.getId(),
                0d,
                0d,
                0d
            ));
        }
    }

    public static void startRTS(Faction faction, Double x, Double y, Double z) {
        Minecraft MC = Minecraft.getInstance();

        if (MC.player != null && MC.level != null) {
            BlockState bs = MC.level.getBlockState(new BlockPos(x.intValue(), y.intValue(), z.intValue()));
            if (!bs.getFluidState().isEmpty() && faction != Faction.NONE) {
                HudClientEvents.showTemporaryMessage(I18n.get("hud.reignofnether.invalid_start_location"));
                return;
            }
            if (faction == Faction.NEUTRAL)
                faction = Faction.NONE;
            PlayerAction playerAction = switch (faction) {
                case VILLAGERS -> PlayerAction.START_RTS_VILLAGERS;
                case MONSTERS -> PlayerAction.START_RTS_MONSTERS;
                case PIGLINS -> PlayerAction.START_RTS_PIGLINS;
                case NONE, NEUTRAL -> PlayerAction.START_RTS_SANDBOX;
            };
            PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(playerAction, MC.player.getId(), x, y, z));
            GameModeServerboundPacket.setAndLockAllClientGameModes(ClientGameModeHelper.gameMode);
            if (ClientGameModeHelper.gameMode == GameMode.SURVIVAL) {
                SurvivalServerboundPacket.startSurvivalMode(SurvivalClientEvents.difficulty);

                CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS).execute(() -> {
                    WaveDifficulty diff = SurvivalClientEvents.difficulty;
                    String diffMsg = I18n.get("hud.gamemode.reignofnether.survival5",
                            diff, SurvivalClientEvents.getMinutesPerDay()).toLowerCase();
                    diffMsg = diffMsg.substring(0,1).toUpperCase() + diffMsg.substring(1);
                    MC.player.sendSystemMessage(Component.literal(""));
                    MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.survival1")
                            .withStyle(Style.EMPTY.withBold(true)));
                    MC.player.sendSystemMessage(Component.literal(diffMsg));
                    MC.player.sendSystemMessage(Component.literal(new String(new char[diffMsg.length()]).replace("\0", "-")));
                    MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.survival2"));
                    MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.survival3"));
                    MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.survival4"));
                    MC.player.sendSystemMessage(Component.literal(""));
                });
            } else if (ClientGameModeHelper.gameMode == GameMode.CLASSIC) {
                BeaconPlacement beacon = BuildingUtils.getBeacon(true);
                boolean isKotB = beacon != null && beacon.getBuilding().capturable;
                if (isKotB) {
                    CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS).execute(() -> {
                        MC.player.sendSystemMessage(Component.literal(""));
                        MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.kotb1")
                                .withStyle(Style.EMPTY.withBold(true)));
                        MC.player.sendSystemMessage(Component.literal("--------"));
                        MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.kotb2"));
                        MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.kotb3"));
                        MC.player.sendSystemMessage(Component.literal(""));
                    });
                } else {
                    CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS).execute(() -> {
                        MC.player.sendSystemMessage(Component.literal(""));
                        MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.classic1")
                                .withStyle(Style.EMPTY.withBold(true)));
                        MC.player.sendSystemMessage(Component.literal("--------"));
                        MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.classic2"));
                        MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.classic3"));
                        MC.player.sendSystemMessage(Component.literal(""));
                    });
                }
            } else if (ClientGameModeHelper.gameMode == GameMode.SANDBOX) {
                CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS).execute(() -> {
                    MC.player.sendSystemMessage(Component.literal(""));
                    MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.sandbox1")
                            .withStyle(Style.EMPTY.withBold(true)));
                    MC.player.sendSystemMessage(Component.literal("--------"));
                    MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.sandbox2"));
                    MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.sandbox3"));
                    MC.player.sendSystemMessage(Component.literal(""));
                });
            }
        }
    }

    public static void startRTSEveryone() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null && MC.level != null) {
            GameModeServerboundPacket.setAndLockAllClientGameModes(ClientGameModeHelper.gameMode);
            PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(PlayerAction.START_RTS_EVERYONE, MC.player.getId(), 0d,0d,0d));
        }
    }

    public static void startRTSScenario(int roleIndex) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null && MC.level != null) {
            PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(PlayerAction.START_RTS_SCENARIO, MC.player.getId(), (double) roleIndex, 0d, 0d));
        }
    }

    public static void cancelStartRTSEveryone() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null && MC.level != null) {
            PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(PlayerAction.CANCEL_START_RTS_EVERYONE, MC.player.getId(), 0d,0d,0d));
        }
    }

    public static void resetRTS() {
        PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(PlayerAction.RESET_RTS, -1, 0d, 0d, 0d));
    }

    public static void publishScenario() {
        PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(PlayerAction.PUBLISH_SCENARIO_MAP, -1, 0d, 0d, 0d));
    }

    // resets and also removes all neutral units and buildings
    public static void resetRTSHard() {
        PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(PlayerAction.RESET_RTS_HARD, -1, 0d, 0d, 0d));
    }

    public static void surrender() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(
                PlayerAction.DEFEAT,
                MC.player.getId(),
                0d,
                0d,
                0d
            ));
        }
    }

    public static void enableRTSSyncing() {
        PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(
            PlayerAction.ENABLE_RTS_SYNCING,
            -1,
            0d,
            0d,
            0d
        ));
    }

    public static void disableRTSSyncing() {
        PacketHandler.INSTANCE.sendToServer(new PlayerServerboundPacket(
            PlayerAction.DISABLE_RTS_SYNCING,
            -1,
            0d,
            0d,
            0d
        ));
    }

    // packet-handler functions
    public PlayerServerboundPacket(PlayerAction action, int playerId, Double x, Double y, Double z) {
        this.action = action;
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PlayerServerboundPacket(PlayerAction action, int playerId) {
        this.action = action;
        this.playerId = playerId;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }


    public PlayerServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(PlayerAction.class);
        this.playerId = buffer.readInt();
        this.x = buffer.readDouble();
        this.y = buffer.readDouble();
        this.z = buffer.readDouble();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeInt(this.playerId);
        buffer.writeDouble(this.x);
        buffer.writeDouble(this.y);
        buffer.writeDouble(this.z);
    }

    private static final List<PlayerAction> opOnlyActions = List.of(
            PlayerAction.RESET_RTS,
            PlayerAction.RESET_RTS_HARD
    );

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("PlayerServerboundPacket: Sender was null");
                success.set(false);
                return;
            } else if (playerId != -1 && player.getId() != playerId) {
                ReignOfNether.LOGGER.warn("PlayerServerboundPacket: Tried to process packet from " + player.getName() + " for id: " + this.playerId);
                success.set(false);
                return;
            } else if (opOnlyActions.contains(action) && !player.hasPermissions(4)) {
                ReignOfNether.LOGGER.warn("PlayerServerboundPacket: Non-op player " + player.getName() + " tried to run action: " + this.action.name());
                success.set(false);
                return;
            }

            switch (action) {
                case TELEPORT -> PlayerServerEvents.movePlayer(this.playerId, this.x, this.y, this.z);
                case ENABLE_ORTHOVIEW -> PlayerServerEvents.enableOrthoview(this.playerId);
                case DISABLE_ORTHOVIEW -> PlayerServerEvents.disableOrthoview(this.playerId);
                case START_RTS_VILLAGERS ->
                    PlayerServerEvents.startRTS(this.playerId, new Vec3(this.x, this.y, this.z), Faction.VILLAGERS);
                case START_RTS_MONSTERS ->
                    PlayerServerEvents.startRTS(this.playerId, new Vec3(this.x, this.y, this.z), Faction.MONSTERS);
                case START_RTS_PIGLINS ->
                    PlayerServerEvents.startRTS(this.playerId, new Vec3(this.x, this.y, this.z), Faction.PIGLINS);
                case START_RTS_SANDBOX ->
                    PlayerServerEvents.startRTS(this.playerId, new Vec3(this.x, this.y, this.z), Faction.NONE);
                case START_RTS_SCENARIO ->
                        PlayerServerEvents.startRTSScenario(this.playerId, (int) this.x);
                case PUBLISH_SCENARIO_MAP ->
                        PlayerServerEvents.publishScenarioMap();
                case START_RTS_EVERYONE -> StartPosServerEvents.startGameCountdown();
                case CANCEL_START_RTS_EVERYONE -> StartPosServerEvents.cancelStartGameCountdown(false);
                case DEFEAT -> PlayerServerEvents.defeat(this.playerId, Component.translatable("server.reignofnether.surrendered").getString());
                case RESET_RTS -> PlayerServerEvents.resetRTS(false);
                case RESET_RTS_HARD -> PlayerServerEvents.resetRTS(true);
                case LOCK_RTS -> PlayerServerEvents.setRTSLock(true);
                case UNLOCK_RTS -> PlayerServerEvents.setRTSLock(false);
                case ENABLE_RTS_SYNCING -> PlayerServerEvents.setRTSSyncingEnabled(true);
                case DISABLE_RTS_SYNCING -> PlayerServerEvents.setRTSSyncingEnabled(false);
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}