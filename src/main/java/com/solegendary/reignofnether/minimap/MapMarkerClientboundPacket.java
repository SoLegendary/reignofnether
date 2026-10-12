package com.solegendary.reignofnether.minimap;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class MapMarkerClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<MapMarkerClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "map_marker_clientbound"));

    @Override
    public CustomPacketPayload.Type<MapMarkerClientboundPacket> type() {
        return TYPE;
    }
    private final int x;
    private final int z;
    private final String playerName;

    public MapMarkerClientboundPacket(int x, int z, String playerName) {
        this.x = x;
        this.z = z;
        this.playerName = playerName;
    }

    public MapMarkerClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.x = buffer.readInt();
        this.z = buffer.readInt();
        this.playerName = buffer.readUtf();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.x);
        buffer.writeInt(this.z);
        buffer.writeUtf(this.playerName);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                MinimapClientEvents.addMapMarker(x, z, playerName);
            });
        });
        return;
    }
}

