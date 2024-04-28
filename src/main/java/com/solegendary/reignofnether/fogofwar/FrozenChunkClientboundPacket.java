package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.building.BuildingAction;
import com.solegendary.reignofnether.building.BuildingClientboundPacket;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class FrozenChunkClientboundPacket {

    BlockPos buildingOrigin;

    public static void setBuildingDestroyedServerside(BlockPos buildingOrigin) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new FrozenChunkClientboundPacket(buildingOrigin));
    }

    // packet-handler functions
    public FrozenChunkClientboundPacket(BlockPos buildingOrigin) {
        this.buildingOrigin = buildingOrigin;
    }

    public FrozenChunkClientboundPacket(FriendlyByteBuf buffer) {
        this.buildingOrigin = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.buildingOrigin);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        FogOfWarClientEvents.setBuildingDestroyedServerside(buildingOrigin);
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}