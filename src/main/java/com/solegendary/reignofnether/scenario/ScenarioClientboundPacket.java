package com.solegendary.reignofnether.scenario;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class ScenarioClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<ScenarioClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "scenario_clientbound"));

    @Override
    public CustomPacketPayload.Type<ScenarioClientboundPacket> type() {
        return TYPE;
    }

    public ScenarioAction action;
    public CompoundTag roleNbt;

    public ScenarioClientboundPacket(ScenarioAction action, CompoundTag roleNbt) {
        this.action = action;
        this.roleNbt = roleNbt;
    }

    public ScenarioClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(ScenarioAction.class);
        this.roleNbt = buffer.readNbt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeNbt(this.roleNbt);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {

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
            });
        });
        return;
    }
}
