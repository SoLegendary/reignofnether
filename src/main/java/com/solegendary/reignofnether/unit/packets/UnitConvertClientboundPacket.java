package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.ArrayUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.function.Supplier;

// send a list of the old units ids that have been converted into new units (with new ids) so the client can retain
// these units' selections, goals and continue the same actions they were taking
public class UnitConvertClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<UnitConvertClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "unit_convert_clientbound"));

    @Override
    public CustomPacketPayload.Type<UnitConvertClientboundPacket> type() {
        return TYPE;
    }

    private final String ownerName; // the player that owns these units
    private final int[] oldUnitIds; // units to be controlled
    private final int[] newUnitIds; // units to be controlled

    public static void syncConvertedUnits(String ownerName, List<Integer> oldUnitIds, List<Integer> newUnitIds) {
        PacketHandler.send(PacketHandler.allPlayers(),
            new UnitConvertClientboundPacket(
                ownerName,
                ArrayUtil.intListToArray(oldUnitIds),
                ArrayUtil.intListToArray(newUnitIds)
            ));
    }

    // packet-handler functions
    public UnitConvertClientboundPacket(
            String ownerName,
            int[] oldUnitIds,
            int[] newUnitIds
    ) {
        this.ownerName = ownerName;
        this.oldUnitIds = oldUnitIds;
        this.newUnitIds = newUnitIds;
    }

    public UnitConvertClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.ownerName = buffer.readUtf();
        this.oldUnitIds = buffer.readVarIntArray();
        this.newUnitIds = buffer.readVarIntArray();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.ownerName);
        buffer.writeVarIntArray(this.oldUnitIds);
        buffer.writeVarIntArray(this.newUnitIds);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            UnitClientEvents.syncConvertedUnits(ownerName, oldUnitIds, newUnitIds);
        });
        return;
    }
}
