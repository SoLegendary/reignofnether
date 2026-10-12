package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitSyncAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class UnitSyncAbilityClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<UnitSyncAbilityClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "unit_sync_ability_clientbound"));

    @Override
    public CustomPacketPayload.Type<UnitSyncAbilityClientboundPacket> type() {
        return TYPE;
    }

    private final UnitSyncAction syncAction;
    private final int entityId;
    private final int[] abilityCooldowns;
    private final int[] abilityCharges;

    public static void sendSyncAbilitiesPacket(LivingEntity entity) {
        if (entity instanceof Unit unit) {
            var abilities = unit.getAbilities().get();
            var abilityCooldowns = new int[abilities.size()];
            var abilityCharges = new int[abilities.size()];
            for (int i = 0; i < abilities.size(); i++) {
                abilityCooldowns[i] = (int) abilities.get(i).getCooldown(unit);
            }
            for (int i = 0; i < abilities.size(); i++) {
                abilityCharges[i] = abilities.get(i).getCharges(unit);
            }
            PacketHandler.send(PacketHandler.allPlayers(),
                    new UnitSyncAbilityClientboundPacket(
                        UnitSyncAction.SYNC_ABILITIES,
                        entity.getId(),
                        abilityCooldowns,
                        abilityCharges
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

    public UnitSyncAbilityClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.syncAction = buffer.readEnum(UnitSyncAction.class);
        this.entityId = buffer.readInt();
        this.abilityCooldowns = buffer.readVarIntArray();
        this.abilityCharges = buffer.readVarIntArray();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.syncAction);
        buffer.writeInt(this.entityId);
        buffer.writeVarIntArray(this.abilityCooldowns);
        buffer.writeVarIntArray(this.abilityCharges);
    }

    // client-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    switch (this.syncAction) {
                        case SYNC_ABILITIES -> {
                            for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                                if (entity.getId() == this.entityId && entity instanceof Unit unit) {
                                    for (int i = 0; i < unit.getAbilities().get().size(); i++) {
                                        if (this.abilityCooldowns.length > i)
                                            unit.getAbilities().get().get(i).setCooldown(this.abilityCooldowns[i], false, unit);
                                        if (this.abilityCharges.length > i)
                                            unit.getAbilities().get().get(i).setCharges(unit, this.abilityCharges[i]);
                                    }
                                    unit.updateAbilityButtons();
                                    break;
                                }
                            }
                        }
                    }
                });
        });
        return;
    }
}
