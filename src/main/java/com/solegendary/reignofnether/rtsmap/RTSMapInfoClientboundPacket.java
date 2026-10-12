package com.solegendary.reignofnether.rtsmap;

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

public class RTSMapInfoClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<RTSMapInfoClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "rts_map_info_clientbound"));

    @Override
    public CustomPacketPayload.Type<RTSMapInfoClientboundPacket> type() {
        return TYPE;
    }

    private final RTSMapInfoAction action;
    private final String value;

    public static void sendValue(RTSMapInfoAction action, String value) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new RTSMapInfoClientboundPacket(action, value));
    }

    public RTSMapInfoClientboundPacket(RTSMapInfoAction action, String value) {
        this.action = action;
        this.value = value;
    }

    public RTSMapInfoClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(RTSMapInfoAction.class);
        this.value = buffer.readUtf();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.value);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                switch (action) {
                    case SET_MODE -> RTSMapInfoClientEvents.selectedMode = value;
                    case ADD_MODE -> {
                        if (!RTSMapInfoClientEvents.modeNames.contains(value))
                            RTSMapInfoClientEvents.modeNames.add(value);
                    }
                    case SET_MAP_NAME -> RTSMapInfoClientEvents.mapName = value;
                    case SET_DESCRIPTION -> RTSMapInfoClientEvents.description = value;
                    case ADD_AUTHOR -> RTSMapInfoClientEvents.authors.add(value);
                    case SET_VERSION -> RTSMapInfoClientEvents.version = value;
                }
            });
        });
    }
}
