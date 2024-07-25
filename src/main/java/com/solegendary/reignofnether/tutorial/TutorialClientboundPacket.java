package com.solegendary.reignofnether.tutorial;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class TutorialClientboundPacket {

    private final boolean enable;

    public static void setEnableTutorial(boolean enable) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new TutorialClientboundPacket(enable));
    }

    public TutorialClientboundPacket(
            boolean enable
    ) {
        this.enable = enable;
    }

    public TutorialClientboundPacket(FriendlyByteBuf buffer) {
        this.enable = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.enable);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        TutorialClientEvents.setEnabled(enable);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
