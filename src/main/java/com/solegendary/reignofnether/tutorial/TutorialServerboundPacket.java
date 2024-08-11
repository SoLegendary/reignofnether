package com.solegendary.reignofnether.tutorial;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class TutorialServerboundPacket {

    TutorialAction action;

    public static void doServerAction(TutorialAction action) {
        PacketHandler.INSTANCE.sendToServer(new TutorialServerboundPacket(action));
    }

    // packet-handler functions
    public TutorialServerboundPacket(TutorialAction action) {
        this.action = action;
    }

    public TutorialServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(TutorialAction.class);
    }

    public void encode(FriendlyByteBuf buffer)  {
        buffer.writeEnum(this.action);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            switch (action) {
                case SPAWN_ANIMALS -> TutorialServerEvents.spawnAnimals();
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
