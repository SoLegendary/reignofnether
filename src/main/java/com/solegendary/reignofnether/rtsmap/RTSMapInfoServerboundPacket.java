package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class RTSMapInfoServerboundPacket {
    private final RTSMapInfoAction action;
    private final String value;

    public static void setStartingMode(String mode) {
        PacketHandler.INSTANCE.sendToServer(new RTSMapInfoServerboundPacket(RTSMapInfoAction.SET_MODE, mode));
    }

    // the Editor's map tab: replaces ron/map.json with this
    public static void saveMapJson(String json) {
        PacketHandler.INSTANCE.sendToServer(new RTSMapInfoServerboundPacket(RTSMapInfoAction.SET_JSON, json));
    }

    // the mapmaker converts this old format map (see RTSMapInfoServerEvents.convert)
    public static void convertMap() {
        PacketHandler.INSTANCE.sendToServer(new RTSMapInfoServerboundPacket(RTSMapInfoAction.CONVERT_MAP, ""));
    }

    public static void setBorderCenter(int x, int z) {
        PacketHandler.INSTANCE.sendToServer(new RTSMapInfoServerboundPacket(RTSMapInfoAction.SET_BORDER_CENTER, x + " " + z));
    }

    public static void setBorderSize(int size) {
        PacketHandler.INSTANCE.sendToServer(new RTSMapInfoServerboundPacket(RTSMapInfoAction.SET_BORDER, String.valueOf(size)));
    }

    public RTSMapInfoServerboundPacket(RTSMapInfoAction action, String value) {
        this.action = action;
        this.value = value;
    }

    public RTSMapInfoServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(RTSMapInfoAction.class);
        this.value = buffer.readUtf(RTSMapInfoClientboundPacket.MAX_LENGTH);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.value, RTSMapInfoClientboundPacket.MAX_LENGTH);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("RTSMapInfoServerboundPacket: Sender was null");
                success.set(false);
                return;
            }
            // the singleplayer/P2P host or an op: picks the mode and edits the map
            if (!StartPosServerEvents.isHost(player)) {
                ReignOfNether.LOGGER.warn("RTSMapInfoServerboundPacket: Tried to process packet from " + player.getName() + " with insufficient permissions");
                success.set(false);
                return;
            }
            switch (action) {
                case SET_MODE -> {
                    if (RTSMapInfoServerEvents.rtsMapInfo != null &&
                        RTSMapInfoServerEvents.rtsMapInfo.supportsMode(value) &&
                        !StartPosServerEvents.isStartingGame()) {
                        RTSMapInfoServerEvents.rtsMapInfo.setSelectedMode(value);
                        RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_MODE, value);
                        StartPosServerEvents.loadPositionsFromMapInfo();
                    }
                }
                case SET_JSON -> {
                    Component refusal = RTSMapInfoServerEvents.writeRefusal();
                    if (refusal != null)
                        player.sendSystemMessage(SystemMessage.error(refusal));
                    else
                        saveMapJson(player);
                }
                case CONVERT_MAP -> MapConverter.convert(player);
                case SET_BORDER -> {
                    ServerLevel level = player.server.getLevel(Level.OVERWORLD);
                    if (level != null && value.matches("\\d{1,8}"))
                        level.getWorldBorder().setSize(Mth.clamp(Integer.parseInt(value), 16, 59999968));
                }
                case SET_BORDER_CENTER -> {
                    ServerLevel level = player.server.getLevel(Level.OVERWORLD);
                    String[] xz = value.split(" ");
                    if (level != null && xz.length == 2 && xz[0].matches("-?\\d{1,8}") && xz[1].matches("-?\\d{1,8}"))
                        level.getWorldBorder().setCenter(Mth.clamp(Integer.parseInt(xz[0]), -29999984, 29999984),
                                Mth.clamp(Integer.parseInt(xz[1]), -29999984, 29999984));
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }

    private void saveMapJson(ServerPlayer player) {
        RTSMapInfo info;
        try {
            info = RTSMapInfoLoader.fromJson(value);
        } catch (Exception e) {
            player.sendSystemMessage(SystemMessage.error(Component.literal("map.json: " + e.getMessage())));
            return;
        }
        // fromJson already dropped modes' removed seats and teams left empty by the team buttons
        if (RTSMapInfoServerEvents.setAndSave(player.server, info))
            player.sendSystemMessage(SystemMessage.info(Component.translatable("sandbox.reignofnether.map.saved")));
        else
            player.sendSystemMessage(SystemMessage.error(Component.translatable("sandbox.reignofnether.map.save_failed")));
    }
}
