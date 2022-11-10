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

import static com.solegendary.reignofnether.building.BuildingUtils.findBuilding;

public class BuildingClientboundPacket {

    // pos is used to identify the building object serverside
    public BlockPos buildingPos;
    public String itemName;
    public Rotation rotation;
    public String ownerName;
    public BuildingAction action;

    public static void placeBuilding(BlockPos buildingPos, String itemName, Rotation rotation, String ownerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.PLACE,
                    itemName, buildingPos, rotation, ownerName));
    }
    public static void destroyBuilding(BlockPos buildingPos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.CANCEL,
                    "", buildingPos, Rotation.NONE, ""));
    }
    public static void startProduction(BlockPos buildingPos, String itemName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(
            BuildingAction.START_PRODUCTION,
            itemName, buildingPos, Rotation.NONE, ""));
    }
    public static void cancelProduction(BlockPos buildingPos, String itemName, boolean frontItem) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(
            frontItem ? BuildingAction.CANCEL_PRODUCTION : BuildingAction.CANCEL_BACK_PRODUCTION,
            itemName, buildingPos, Rotation.NONE, ""));
    }

    public BuildingClientboundPacket(BuildingAction action, String itemName, BlockPos buildingPos, Rotation rotation, String ownerName) {
        this.action = action;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
        this.rotation = rotation;
        this.ownerName = ownerName;
    }

    public BuildingClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemName = buffer.readUtf();
        this.buildingPos = buffer.readBlockPos();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.buildingPos);
        buffer.writeEnum(this.rotation);
        buffer.writeUtf(this.ownerName);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> {
                ProductionBuilding building = null;
                if (this.action != BuildingAction.PLACE) {
                    building = (ProductionBuilding) findBuilding(BuildingClientEvents.getBuildings(), this.buildingPos);

                    if (building == null)
                        return;
                }
                switch (action) {
                    case PLACE -> BuildingClientEvents.placeBuilding(this.itemName, this.buildingPos, this.rotation, this.ownerName);
                    case CANCEL -> BuildingClientEvents.destroyBuilding(this.buildingPos);
                    case START_PRODUCTION -> {
                        ProductionBuilding.startProductionItem(building, this.itemName, this.buildingPos);
                    }
                    case CANCEL_PRODUCTION -> {
                        ProductionBuilding.cancelProductionItem(building, this.itemName, this.buildingPos, true);
                    }
                    case CANCEL_BACK_PRODUCTION -> {
                        ProductionBuilding.cancelProductionItem(building, this.itemName, this.buildingPos, false);
                    }
                }
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
