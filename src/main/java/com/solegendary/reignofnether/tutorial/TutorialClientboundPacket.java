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

    private final TutorialAction action;

    public static void enableTutorial() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new TutorialClientboundPacket(TutorialAction.ENABLE));
    }
    public static void disableTutorial() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new TutorialClientboundPacket(TutorialAction.DISABLE));
    }
    public static void updateTutorialStage() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new TutorialClientboundPacket(TutorialAction.UPDATE_STAGE));
    }

    public TutorialClientboundPacket(TutorialAction action) {
        this.action = action;
    }

    public TutorialClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(TutorialAction.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    switch (action) {
                        case ENABLE -> TutorialClientEvents.setEnabled(true);
                        case DISABLE -> TutorialClientEvents.setEnabled(false);
                        case UPDATE_STAGE -> TutorialClientEvents.updateStage();
                    }
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
