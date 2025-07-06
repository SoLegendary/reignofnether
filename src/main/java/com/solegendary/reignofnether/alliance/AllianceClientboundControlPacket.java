package com.solegendary.reignofnether.alliance;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AllianceClientboundControlPacket {
    private final String playerName;
    private final boolean value;

    public AllianceClientboundControlPacket(String playerName, boolean value) {
        this.playerName = playerName;
        this.value = value;
    }

    public AllianceClientboundControlPacket(FriendlyByteBuf buf) {
        this.playerName = buf.readUtf(32767);
        this.value = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(playerName);
        buf.writeBoolean(value);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (value)
                AlliancesClient.playersWithAlliedControl.add(playerName);
            else
                AlliancesClient.playersWithAlliedControl.remove(playerName);
        });
        return true;
    }
}