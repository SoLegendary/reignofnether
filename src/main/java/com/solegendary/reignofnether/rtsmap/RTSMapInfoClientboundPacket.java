package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class RTSMapInfoClientboundPacket {

    private final RTSMapInfoAction action;
    private final String value;

    public static void sendValue(RTSMapInfoAction action, String value) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new RTSMapInfoClientboundPacket(action, value));
    }

    public RTSMapInfoClientboundPacket(RTSMapInfoAction action, String value) {
        this.action = action;
        this.value = value;
    }

    public RTSMapInfoClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(RTSMapInfoAction.class);
        this.value = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.value);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
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
        ctx.get().setPacketHandled(true);
        return true;
    }
}
