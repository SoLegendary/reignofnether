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
    public String itemName;
    public Rotation rotation;
    public String ownerName;
    public BuildingAction action;

    public static void placeBuilding(BlockPos pos, String itemName, Rotation rotation, String ownerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.PLACE,
                    itemName, pos, rotation, ownerName));
    }
    public static void destroyBuilding(BlockPos pos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.CANCEL,
                    "", pos, Rotation.NONE, ""));
    }
    public static void startProduction(BlockPos pos, String itemName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(
            BuildingAction.START_PRODUCTION,
            itemName, pos, Rotation.NONE, ""));
    }
    public static void cancelProduction(BlockPos pos, String itemName, boolean frontItem) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(
            frontItem ? BuildingAction.CANCEL_PRODUCTION : BuildingAction.CANCEL_BACK_PRODUCTION,
            itemName, pos, Rotation.NONE, ""));
    }

    public BuildingClientboundPacket(BuildingAction action, String itemName, BlockPos pos, Rotation rotation, String ownerName) {
        this.action = action;
        this.itemName = itemName;
        this.pos = pos;
        this.rotation = rotation;
        this.ownerName = ownerName;
    }

    public BuildingClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemName = buffer.readUtf();
        this.pos = buffer.readBlockPos();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.itemName);
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
                switch (action) {
                    case PLACE -> BuildingClientEvents.placeBuilding(this.itemName, this.pos, this.rotation, this.ownerName);
                    case CANCEL -> BuildingClientEvents.destroyBuilding(this.pos);
                    case START_PRODUCTION -> ProductionBuilding.startProductionItem(BuildingClientEvents.getBuildings(), this.itemName, this.pos);
                    case CANCEL_PRODUCTION -> ProductionBuilding.cancelProductionItem(BuildingClientEvents.getBuildings(), this.itemName, this.pos, true);
                    case CANCEL_BACK_PRODUCTION -> ProductionBuilding.cancelProductionItem(BuildingClientEvents.getBuildings(), this.itemName, this.pos, false);
                }
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
