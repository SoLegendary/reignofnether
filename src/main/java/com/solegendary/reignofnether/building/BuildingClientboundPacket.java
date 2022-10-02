package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.registrars.PacketHandler;
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
    public BuildingAction action;

    public static void placeBuilding(BlockPos pos, String buildingName, Rotation rotation, String ownerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.PLACE,
                    pos,  buildingName, rotation, ownerName));
    }
    public static void destroyBuilding(BlockPos pos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.CANCEL,
                    pos, "", Rotation.NONE, ""));
    }

    public BuildingClientboundPacket(BuildingAction action, BlockPos pos, String buildingName, Rotation rotation, String ownerName) {
        this.action = action;
        this.pos = pos;
        this.buildingName = buildingName;
        this.rotation = rotation;
        this.ownerName = ownerName;
    }

    public BuildingClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.pos = buffer.readBlockPos();
        this.buildingName = buffer.readUtf();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBlockPos(this.pos);
        buffer.writeUtf(this.buildingName);
        buffer.writeEnum(this.rotation);
        buffer.writeUtf(this.ownerName);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> {
                switch (action) {
                    case PLACE -> BuildingClientEvents.placeBuilding(this.buildingName, this.pos, this.rotation, this.ownerName);
                    case CANCEL -> BuildingClientEvents.destroyBuilding(this.pos);
                }
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
