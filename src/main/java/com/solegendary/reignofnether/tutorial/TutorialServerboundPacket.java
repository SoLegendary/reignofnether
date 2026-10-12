package com.solegendary.reignofnether.tutorial;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class TutorialServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<TutorialServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "tutorial_serverbound"));

    @Override
    public CustomPacketPayload.Type<TutorialServerboundPacket> type() {
        return TYPE;
    }

    TutorialAction action;
    TutorialStage stage;

    public static void doServerAction(TutorialAction action) {
        PacketHandler.sendToServer(new TutorialServerboundPacket(action, TutorialStage.INTRO));
    }

    public static void saveStage(TutorialStage stage) {
        PacketHandler.sendToServer(new TutorialServerboundPacket(TutorialAction.SAVE_STAGE, stage));
    }

    // packet-handler functions
    public TutorialServerboundPacket(TutorialAction action, TutorialStage stage) {
        this.action = action;
        this.stage = stage;
    }

    public TutorialServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(TutorialAction.class);
        this.stage = buffer.readEnum(TutorialStage.class);
    }

    public void encode(RegistryFriendlyByteBuf buffer)  {
        buffer.writeEnum(this.action);
        buffer.writeEnum(this.stage);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            switch (action) {
                case SAVE_STAGE -> TutorialServerEvents.saveStage(stage);
                case SET_DAY_TIME -> TutorialServerEvents.setDayTime();
                case SET_NIGHT_TIME -> TutorialServerEvents.setNightTime();
                case SPAWN_ANIMALS -> TutorialServerEvents.spawnAnimals();
                case SPAWN_MONSTERS_A -> TutorialServerEvents.spawnMonstersA();
                case ATTACK_WITH_MONSTERS_A -> TutorialServerEvents.attackWithMonstersA();
                case SPAWN_MONSTERS_B -> TutorialServerEvents.spawnMonstersB();
                case ATTACK_WITH_MONSTERS_B -> TutorialServerEvents.attackWithMonstersB();
                case SPAWN_MONSTER_WORKERS -> TutorialServerEvents.spawnMonsterWorkers();
                case START_MONSTER_BASE -> TutorialServerEvents.startBuildingMonsterBase();
                case EXPAND_MONSTER_BASE_A -> TutorialServerEvents.expandMonsterBaseA();
                case EXPAND_MONSTER_BASE_B -> TutorialServerEvents.expandMonsterBaseB();
                case SPAWN_MONSTER_BASE_ARMY -> TutorialServerEvents.spawnMonsterBaseArmy();
                case SPAWN_FRIENDLY_ARMY -> TutorialServerEvents.spawnFriendlyArmy();
            }
        });
        return;
    }
}
