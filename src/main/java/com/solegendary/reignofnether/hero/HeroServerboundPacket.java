package com.solegendary.reignofnether.hero;

import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.function.Supplier;

public class HeroServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<HeroServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "hero_serverbound"));

    @Override
    public CustomPacketPayload.Type<HeroServerboundPacket> type() {
        return TYPE;
    }

    private final int unitId;
    private final HeroAction heroAction;

    public static void requestHeroSync(int unitId) {
        PacketHandler.sendToServer(new HeroServerboundPacket(unitId, HeroAction.REQUEST_SYNC));
    }

    public HeroServerboundPacket(
            int unitId,
            HeroAction heroAction
    ) {
        this.unitId = unitId;
        this.heroAction = heroAction;
    }

    public HeroServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.heroAction = buffer.readEnum(HeroAction.class);
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        buffer.writeEnum(this.heroAction);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (heroAction == HeroAction.REQUEST_SYNC) {
                for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
                    if (entity.getId() == this.unitId && entity instanceof HeroUnit hero) {
                        hero.syncToClients();
                    }
                }
            }
        });
        return;
    }
}
