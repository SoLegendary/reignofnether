package com.solegendary.reignofnether.fogofwar;

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

public class FrozenChunkClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<FrozenChunkClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "frozen_chunk_clientbound"));

    @Override
    public CustomPacketPayload.Type<FrozenChunkClientboundPacket> type() {
        return TYPE;
    }

    FrozenChunkAction action;
    BlockPos blockPos;

    public static void setBuildingDestroyedServerside(BlockPos buildingOrigin) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new FrozenChunkClientboundPacket(FrozenChunkAction.SET_BUILDING_DESTROYED, buildingOrigin));
    }

    public static void setBuildingBuiltServerside(BlockPos buildingOrigin) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new FrozenChunkClientboundPacket(FrozenChunkAction.SET_BUILDING_BUILT, buildingOrigin));
    }

    // packet-handler functions
    public FrozenChunkClientboundPacket(FrozenChunkAction action, BlockPos blockPos) {
        this.action = action;
        this.blockPos = blockPos;
    }

    public FrozenChunkClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(FrozenChunkAction.class);
        this.blockPos = buffer.readBlockPos();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBlockPos(this.blockPos);
    }

    // client-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (action) {
                            case SET_BUILDING_DESTROYED -> FogOfWarClientEvents.setBuildingDestroyedServerside(blockPos);
                            case SET_BUILDING_BUILT -> FogOfWarClientEvents.setBuildingBuiltServerside(blockPos);
                        }
                    });
        });
        return;
    }
}