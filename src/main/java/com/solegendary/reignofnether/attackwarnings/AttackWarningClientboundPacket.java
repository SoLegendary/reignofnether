
package com.solegendary.reignofnether.attackwarnings;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class AttackWarningClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<AttackWarningClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "attack_warning_clientbound"));

    @Override
    public CustomPacketPayload.Type<AttackWarningClientboundPacket> type() {
        return TYPE;
    }

    private final String attackedPlayerName;
    private final BlockPos attackPos;

    public static void sendWarning(String attackedPlayerName, BlockPos attackPos) {
        PacketHandler.send(PacketHandler.allPlayers(),
            new AttackWarningClientboundPacket(
                attackedPlayerName,
                attackPos
            ));
    }

    // packet-handler functions
    public AttackWarningClientboundPacket(
        String attackedPlayerName,
        BlockPos attackPos
    ) {
        this.attackedPlayerName = attackedPlayerName;
        this.attackPos = attackPos;
    }

    public AttackWarningClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.attackedPlayerName = buffer.readUtf();
        this.attackPos = buffer.readBlockPos();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.attackedPlayerName);
        buffer.writeBlockPos(this.attackPos);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            AttackWarningClientEvents.checkAndTriggerAttackWarning(attackedPlayerName, attackPos);
        });
        return;
    }
}
