package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class FogOfWarClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<FogOfWarClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "fog_of_war_clientbound"));

    @Override
    public CustomPacketPayload.Type<FogOfWarClientboundPacket> type() {
        return TYPE;
    }

    public boolean enable;
    public String playerName;
    public int unitId;

    public static void setEnabled(boolean enable) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new FogOfWarClientboundPacket(enable, "", 0));
    }

    public static void revealOrHidePlayer(boolean reveal, String playerName) {
        FogOfWarServerEvents.setPlayerRevealed(playerName, reveal);
    }

    // reveal a ranged unit briefly to the player it's attacking
    public static void revealRangedUnit(String playerBeingAttacked, int unitId) {
        FogOfWarServerEvents.revealRangedUnit(unitId, playerBeingAttacked, RangedAttackerUnit.FOG_REVEAL_TICKS_MAX);
        PacketHandler.send(PacketHandler.allPlayers(),
                new FogOfWarClientboundPacket(true, playerBeingAttacked, unitId));
    }

    public FogOfWarClientboundPacket(boolean enable, String playerName, int unitId) {
        this.enable = enable;
        this.playerName = playerName;
        this.unitId = unitId;
    }

    public FogOfWarClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.enable = buffer.readBoolean();
        this.playerName = buffer.readUtf();
        this.unitId = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(this.enable);
        buffer.writeUtf(this.playerName);
        buffer.writeInt(this.unitId);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    if (unitId > 0)
                        FogOfWarClientEvents.revealRangedUnit(playerName, unitId);
                    else if (playerName.isEmpty())
                        FogOfWarClientEvents.setEnabled(enable);
                });
        });
        return;
    }
}
