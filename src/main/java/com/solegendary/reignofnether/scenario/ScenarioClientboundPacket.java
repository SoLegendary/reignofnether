package com.solegendary.reignofnether.scenario;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ScenarioClientboundPacket {

    public ScenarioAction action;
    public CompoundTag roleNbt;

    public ScenarioClientboundPacket(ScenarioAction action, CompoundTag roleNbt) {
        this.action = action;
        this.roleNbt = roleNbt;
    }

    public ScenarioClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(ScenarioAction.class);
        this.roleNbt = buffer.readNbt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeNbt(this.roleNbt);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {

                switch (this.action) {
                    case LOAD_SCENARIO_ROLE -> {
                        int index = roleNbt.getInt("index");
                        for (int i = 0; i < ScenarioClientEvents.scenarioRoles.size(); i++) {
                            if (ScenarioClientEvents.scenarioRoles.get(i).index == index) {
                                ScenarioClientEvents.scenarioRoles.get(i).nbt = roleNbt;
                                ScenarioClientEvents.scenarioRoles.get(i).unpackNbt();
                                break;
                            }
                        }
                    }
                }
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
