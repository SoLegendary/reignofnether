package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class CustomBuildingClientboundPacket {

    // pos is used to identify the building object serverside
    public String name;
    public BlockPos originPos;
    public BlockPos structurePos;
    public BlockPos structureSize;

    public static void registerCustomBuilding(
            String name,
            BlockPos originPos,
            BlockPos structurePos,
            Vec3i structureSize
    ) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new CustomBuildingClientboundPacket(
            name, originPos, structurePos, new BlockPos(structureSize)
        ));
    }

    public CustomBuildingClientboundPacket(
            String name,
            BlockPos originPos,
            BlockPos structurePos,
            BlockPos structureSize
    ) {
        this.name = name;
        this.originPos = originPos;
        this.structurePos = structurePos;
        this.structureSize = structureSize;
    }

    public CustomBuildingClientboundPacket(FriendlyByteBuf buffer) {
        this.name = buffer.readUtf();
        this.originPos = buffer.readBlockPos();
        this.structurePos = buffer.readBlockPos();
        this.structureSize = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.name);
        buffer.writeBlockPos(this.originPos);
        buffer.writeBlockPos(this.structurePos);
        buffer.writeBlockPos(this.structureSize);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                CustomBuildingClientEvents.registerCustomBuilding(name, originPos, structurePos, structureSize);
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
