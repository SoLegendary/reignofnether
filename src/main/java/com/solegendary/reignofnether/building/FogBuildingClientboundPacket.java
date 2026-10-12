package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class FogBuildingClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<FogBuildingClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "fog_building_clientbound"));

    @Override
    public CustomPacketPayload.Type<FogBuildingClientboundPacket> type() {
        return TYPE;
    }

    private final BlockPos pos;

    public static void removeFogQueuedBuilding(BlockPos pos) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new FogBuildingClientboundPacket(pos)
        );
    }

    public FogBuildingClientboundPacket(BlockPos pos) {
        this.pos = pos;
    }

    public FogBuildingClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
    }

    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        BuildingClientEvents.removeFogQueuedBuilding(pos);
                    });
        });
        return;
    }
}
