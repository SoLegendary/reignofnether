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
            new BuildingClientboundPacket(pos,  buildingName, rotation, ownerName, BuildingAction.PLACE));
    }
    public static void destroyBuilding(BlockPos pos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(pos, "", Rotation.NONE, "", BuildingAction.CANCEL));
    }

    public BuildingClientboundPacket(BlockPos pos, String buildingName, Rotation rotation, String ownerName, BuildingAction action) {
        this.pos = pos;
        this.buildingName = buildingName;
        this.rotation = rotation;
        this.ownerName = ownerName;
        this.action = action;
    }

    public BuildingClientboundPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.buildingName = buffer.readUtf();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
        this.action = buffer.readEnum(BuildingAction.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeUtf(this.buildingName);
        buffer.writeEnum(this.rotation);
        buffer.writeUtf(this.ownerName);
        buffer.writeEnum(this.action);
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
