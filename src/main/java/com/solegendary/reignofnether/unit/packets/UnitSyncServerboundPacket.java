package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.UnitSyncAction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class UnitSyncServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<UnitSyncServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "unit_sync_serverbound"));

    @Override
    public CustomPacketPayload.Type<UnitSyncServerboundPacket> type() {
        return TYPE;
    }

    private final UnitSyncAction syncAction;
    private final int entityId;

    public static void requestSyncAbilities(int unitId) {
        PacketHandler.sendToServer(new UnitSyncServerboundPacket(UnitSyncAction.REQUEST_SYNC_ABILITIES, unitId));
    }

    // packet-handler functions
    public UnitSyncServerboundPacket(
        UnitSyncAction syncAction,
        int unitId
    ) {
        this.syncAction = syncAction;
        this.entityId = unitId;
    }

    public UnitSyncServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.syncAction = buffer.readEnum(UnitSyncAction.class);
        this.entityId = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.syncAction);
        buffer.writeInt(this.entityId);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (this.syncAction == UnitSyncAction.REQUEST_SYNC_ABILITIES) {
                for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
                    if (entity.getId() == this.entityId) {
                        UnitSyncAbilityClientboundPacket.sendSyncAbilitiesPacket(entity);
                    }
                }
            }
        });
        return;
    }
}
