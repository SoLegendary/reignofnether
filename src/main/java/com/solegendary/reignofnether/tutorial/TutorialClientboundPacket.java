package com.solegendary.reignofnether.tutorial;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class TutorialClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<TutorialClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "tutorial_clientbound"));

    @Override
    public CustomPacketPayload.Type<TutorialClientboundPacket> type() {
        return TYPE;
    }

    private final TutorialAction action;
    private final TutorialStage stage;

    public static void enableTutorial() {
        PacketHandler.send(PacketHandler.allPlayers(),
                new TutorialClientboundPacket(TutorialAction.ENABLE, TutorialStage.INTRO));
    }
    public static void disableTutorial() {
        PacketHandler.send(PacketHandler.allPlayers(),
                new TutorialClientboundPacket(TutorialAction.DISABLE, TutorialStage.INTRO));
    }
    public static void loadTutorialStage(TutorialStage stage) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new TutorialClientboundPacket(TutorialAction.LOAD_STAGE, stage));
    }

    public TutorialClientboundPacket(TutorialAction action, TutorialStage stage) {
        this.action = action;
        this.stage = stage;
    }

    public TutorialClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(TutorialAction.class);
        this.stage = buffer.readEnum(TutorialStage.class);
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeEnum(this.stage);
    }

    // client-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    switch (action) {
                        case ENABLE -> TutorialClientEvents.setEnabled(true);
                        case DISABLE -> TutorialClientEvents.setEnabled(false);
                        case LOAD_STAGE -> TutorialClientEvents.loadStage(stage);
                    }
                });
        });
        return;
    }
}
