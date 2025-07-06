package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitSyncAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitSyncAbilityClientboundPacket {

    private final UnitSyncAction syncAction;
    private final int entityId;
    private final int[] abilityCooldowns;
    private final int[] abilityCharges;

    public static void sendSyncAbilitiesPacket(LivingEntity entity) {
        if (entity instanceof Unit unit) {
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncAbilityClientboundPacket(
                    UnitSyncAction.SYNC_ABILITIES,
                    entity.getId(),
                    unit.getAbilities().stream()
                            .mapToInt(a -> (int) a.getCooldown())
                            .toArray(),
                    unit.getAbilities().stream()
                            .mapToInt(a -> a.charges)
                            .toArray()
                )
            );
        }
    }

    // packet-handler functions
    public UnitSyncAbilityClientboundPacket(
        UnitSyncAction syncAction,
        int entityId,
        final int[] abilityCooldowns,
        final int[] abilityCharges
    ) {
        // filter out non-owned entities so we can't control them
        this.syncAction = syncAction;
        this.entityId = entityId;
        this.abilityCooldowns = abilityCooldowns;
        this.abilityCharges = abilityCharges;
    }

    public UnitSyncAbilityClientboundPacket(FriendlyByteBuf buffer) {
        this.syncAction = buffer.readEnum(UnitSyncAction.class);
        this.entityId = buffer.readInt();
        this.abilityCooldowns = buffer.readVarIntArray();
        this.abilityCharges = buffer.readVarIntArray();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.syncAction);
        buffer.writeInt(this.entityId);
        buffer.writeVarIntArray(this.abilityCooldowns);
        buffer.writeVarIntArray(this.abilityCharges);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    switch (this.syncAction) {
                        case SYNC_ABILITIES -> {
                            for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                                if (entity.getId() == this.entityId && entity instanceof Unit unit) {
                                    for (int i = 0; i < unit.getAbilities().size(); i++) {
                                        if (this.abilityCooldowns.length > i)
                                            unit.getAbilities().get(i).setCooldown(this.abilityCooldowns[i], false);
                                        if (this.abilityCharges.length > i)
                                            unit.getAbilities().get(i).charges = this.abilityCharges[i];
                                    }
                                    break;
                                }
                            }
                        }
                    }
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
