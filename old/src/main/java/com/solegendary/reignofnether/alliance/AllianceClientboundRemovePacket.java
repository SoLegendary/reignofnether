package com.solegendary.reignofnether.alliance;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AllianceClientboundRemovePacket {
    private final String owner1;
    private final String owner2;
    private final boolean isReset;

    // Constructor for individual removal
    public AllianceClientboundRemovePacket(String owner1, String owner2) {
        this.owner1 = owner1;
        this.owner2 = owner2;
        this.isReset = false;
    }

    // Constructor for full reset
    public AllianceClientboundRemovePacket() {
        this.owner1 = "";
        this.owner2 = "";
        this.isReset = true;
    }

    // Deserialization
    public AllianceClientboundRemovePacket(FriendlyByteBuf buf) {
        this.isReset = buf.readBoolean();
        if (!isReset) {
            this.owner1 = buf.readUtf(32767);
            this.owner2 = buf.readUtf(32767);
        } else {
            this.owner1 = "";
            this.owner2 = "";
        }
    }

    // Serialization
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(isReset);
        if (!isReset) {
            buf.writeUtf(owner1);
            buf.writeUtf(owner2);
        }
    }

    // Handling
    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (isReset) {
                // Client-side handling for a reset: clear all alliances
                AlliancesClient.resetAllAlliances();
            } else {
                // Client-side handling for removing a specific alliance
                AlliancesClient.removeAlliance(owner1, owner2);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
