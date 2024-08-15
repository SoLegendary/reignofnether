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
                case SPAWN_MONSTERS_A -> TutorialServerEvents.spawnMonstersA();
                case ATTACK_WITH_MONSTERS_A -> TutorialServerEvents.attackWithMonstersA();
                case SPAWN_MONSTERS_B -> TutorialServerEvents.spawnMonstersB();
                case ATTACK_WITH_MONSTERS_B -> TutorialServerEvents.attackWithMonstersB();
                case SPAWN_MONSTER_WORKERS -> TutorialServerEvents.spawnMonsterWorkers();
                case START_BUILDING_MONSTER_BASE -> TutorialServerEvents.startBuildingMonsterBase();
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
