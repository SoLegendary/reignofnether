package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class BuildingClientboundPacket {

    // pos is used to identify the building object serverside
    public BlockPos pos;
    public String buildingName;
    public Rotation rotation;
    public String ownerName;

    public static void placeBuilding(String buildingName, BlockPos originPos, Rotation rotation, String ownerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new BuildingClientboundPacket(buildingName, originPos, rotation, ownerName));
    }

    public BuildingClientboundPacket(String buildingName, BlockPos pos, Rotation rotation, String ownerName) {
        this.buildingName = buildingName;
        this.pos = pos;
        this.rotation = rotation;
        this.ownerName = ownerName;
    }

    public BuildingClientboundPacket(FriendlyByteBuf buffer) {
        this.buildingName = buffer.readUtf();
        this.pos = buffer.readBlockPos();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.buildingName);
        buffer.writeBlockPos(this.pos);
        buffer.writeEnum(this.rotation);
        buffer.writeUtf(this.ownerName);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> {
                BuildingClientEvents.placeBuilding(this.buildingName, this.pos, this.rotation, this.ownerName);
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
