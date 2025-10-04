package com.solegendary.reignofnether.alliance;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class AllianceServerboundPacket {

    AllianceAction action;
    public String targetPlayerName;
    public boolean boolValue;

    public static void doAllianceAction(AllianceAction action, String targetPlayerName) {
        PacketHandler.INSTANCE.sendToServer(new AllianceServerboundPacket(
                action,
                targetPlayerName,
                false
        ));
    }

    public static void doAllianceAction(AllianceAction action, boolean value) {
        PacketHandler.INSTANCE.sendToServer(new AllianceServerboundPacket(
                action,
                "",
                value
        ));
    }

    public AllianceServerboundPacket(AllianceAction action, String targetPlayerName, boolean boolValue) {
        this.action = action;
        this.targetPlayerName = targetPlayerName;
        this.boolValue = boolValue;
    }

    public AllianceServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(AllianceAction.class);
        this.targetPlayerName = buffer.readUtf();
        this.boolValue = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.targetPlayerName);
        buffer.writeBoolean(this.boolValue);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("AbilityServerboundPacket: Sender was null");
                success.set(false);
                return;
            }
            switch (action) {
                case REQUEST -> MiscUtil.runPlayerCommand(player, "ally " + targetPlayerName);
                case CANCEL_REQUEST -> MiscUtil.runPlayerCommand(player, "allycancelrequest " + targetPlayerName);
                case ACCEPT_REQUEST -> MiscUtil.runPlayerCommand(player, "allyconfirm " + targetPlayerName);
                case DISBAND -> MiscUtil.runPlayerCommand(player, "disband " + targetPlayerName);
                case SET_ALLY_CONTROL -> MiscUtil.runPlayerCommand(player, "allycontrol " + boolValue);
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
