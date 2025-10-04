package com.solegendary.reignofnether.hero;

import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class HeroServerboundPacket {

    private final int unitId;
    private final HeroAction heroAction;

    public static void requestHeroSync(int unitId) {
        PacketHandler.INSTANCE.sendToServer(new HeroServerboundPacket(unitId, HeroAction.REQUEST_SYNC));
    }

    public HeroServerboundPacket(
            int unitId,
            HeroAction heroAction
    ) {
        this.unitId = unitId;
        this.heroAction = heroAction;
    }

    public HeroServerboundPacket(FriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.heroAction = buffer.readEnum(HeroAction.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        buffer.writeEnum(this.heroAction);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            if (heroAction == HeroAction.REQUEST_SYNC) {
                for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
                    if (entity.getId() == this.unitId && entity instanceof HeroUnit hero) {
                        hero.syncToClients();
                    }
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
