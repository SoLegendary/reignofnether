package com.solegendary.reignofnether.alliance;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class AllianceClientboundPacket {

    // pos is used to identify the building object serverside
    AllianceAction action;
    public String player1;
    public String player2;
    public boolean boolValue;

    public static void addAlliance(String playerName1, String playerName2) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new AllianceClientboundPacket(AllianceAction.ACCEPT_REQUEST, playerName1, playerName2, true));
    }

    public static void addPendingAlliance(String toPlayer, String fromPlayer) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new AllianceClientboundPacket(AllianceAction.REQUEST, toPlayer, fromPlayer, true));
    }

    public static void cancelPendingAlliance(String toPlayer, String fromPlayer) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new AllianceClientboundPacket(AllianceAction.CANCEL_REQUEST, toPlayer, fromPlayer, true));
    }

    public static void removeAlliance(String playerName1, String playerName2) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new AllianceClientboundPacket(AllianceAction.DISBAND, playerName1, playerName2, false));
    }

    public static void resetAlliances() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new AllianceClientboundPacket(AllianceAction.DISBAND, "", "", true));
    }

    public static void setAllyControl(String playerName1, boolean setValue) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new AllianceClientboundPacket(AllianceAction.SET_ALLY_CONTROL, playerName1, "", setValue));
    }

    public AllianceClientboundPacket(
            AllianceAction action,
            String player1,
            String player2,
            boolean boolValue
    ) {
        this.action = action;
        this.player1 = player1;
        this.player2 = player2;
        this.boolValue = boolValue;
    }

    public AllianceClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(AllianceAction.class);
        this.player1 = buffer.readUtf();
        this.player2 = buffer.readUtf();
        this.boolValue = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.player1);
        buffer.writeUtf(this.player2);
        buffer.writeBoolean(this.boolValue);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                switch (this.action) {

                    case REQUEST -> {
                        // receives a pending alliance request
                        AlliancesClient.addPendingAlliance(player1, player2);
                    }
                    case CANCEL_REQUEST -> {
                        // removes a pending alliance request
                        AlliancesClient.cancelPendingAlliance(player1, player2);
                    }
                    case ACCEPT_REQUEST -> {
                        AlliancesClient.addAlliance(player1, player2);
                    }
                    case DISBAND -> {
                        if (boolValue)
                            AlliancesClient.resetAllAlliances();
                        else
                            AlliancesClient.removeAlliance(player1, player2);
                    }
                    case SET_ALLY_CONTROL -> {
                        if (boolValue)
                            AlliancesClient.playersWithAlliedControl.add(player1);
                        else
                            AlliancesClient.playersWithAlliedControl.remove(player1);
                    }
                }
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
