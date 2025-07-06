package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.UnitSyncAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitSyncServerboundPacket {

    private final UnitSyncAction syncAction;
    private final int entityId;

    public static void requestSyncAbilities(int unitId) {
        PacketHandler.INSTANCE.sendToServer(new UnitSyncServerboundPacket(UnitSyncAction.REQUEST_SYNC_ABILITIES, unitId));
    }

    // packet-handler functions
    public UnitSyncServerboundPacket(
        UnitSyncAction syncAction,
        int unitId
    ) {
        this.syncAction = syncAction;
        this.entityId = unitId;
    }

    public UnitSyncServerboundPacket(FriendlyByteBuf buffer) {
        this.syncAction = buffer.readEnum(UnitSyncAction.class);
        this.entityId = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.syncAction);
        buffer.writeInt(this.entityId);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            if (this.syncAction == UnitSyncAction.REQUEST_SYNC_ABILITIES) {
                for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
                    if (entity.getId() == this.entityId) {
                        UnitSyncAbilityClientboundPacket.sendSyncAbilitiesPacket(entity);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
