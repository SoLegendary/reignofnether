package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.building.BuildingUtils.findBuilding;

public class BuildingServerboundPacket {
    // pos is used to identify the building object serverside
    public String itemName; // name of the building or production item // PLACE, START_PRODUCTION, CANCEL_PRODUCTION
    public BlockPos buildingPos; // required for all actions (used to identify the relevant building)
    public BlockPos rallyPos; // required for all actions (used to identify the relevant building)
    public Rotation rotation; // PLACE
    public String ownerName; // PLACE
    public int repairAmount; // REPAIR
    public BuildingAction action;

    public static void placeBuilding(String itemName, BlockPos originPos, Rotation rotation, String ownerName) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.PLACE,
                itemName, originPos, BlockPos.ZERO, rotation, ownerName, 0));
    }
    public static void cancelBuilding(BlockPos buildingPos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.CANCEL,
                "", buildingPos, BlockPos.ZERO, Rotation.NONE, "", 0));
    }
    public static void repairBuilding(BlockPos buildingPos, int repairAmount) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.REPAIR,
                "", buildingPos, BlockPos.ZERO, Rotation.NONE, "", repairAmount));
    }
    public static void setRallyPoint(BlockPos buildingPos, BlockPos rallyPos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.SET_RALLY_POINT,
                "", buildingPos, rallyPos, Rotation.NONE, "", 0));
    }
    public static void startProduction(BlockPos buildingPos, String itemName) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.START_PRODUCTION,
                itemName, buildingPos, BlockPos.ZERO, Rotation.NONE, "", 0));
    }
    public static void cancelProduction(BlockPos buildingPos, String itemName, boolean frontItem) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                frontItem ? BuildingAction.CANCEL_PRODUCTION : BuildingAction.CANCEL_BACK_PRODUCTION,
                itemName, buildingPos, BlockPos.ZERO, Rotation.NONE, "", 0));
    }


    public BuildingServerboundPacket(BuildingAction action, String itemName, BlockPos buildingPos, BlockPos rallyPos, Rotation rotation, String ownerName, int repairAmount) {
        this.action = action;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
        this.rallyPos = rallyPos;
        this.rotation = rotation;
        this.ownerName = ownerName;
        this.repairAmount = repairAmount;
    }

    public BuildingServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemName = buffer.readUtf();
        this.buildingPos = buffer.readBlockPos();
        this.rallyPos = buffer.readBlockPos();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
        this.repairAmount = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.buildingPos);
        buffer.writeBlockPos(this.rallyPos);
        buffer.writeEnum(this.rotation);
        buffer.writeUtf(this.ownerName);
        buffer.writeInt(this.repairAmount);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            ProductionBuilding building = null;
            if (this.action != BuildingAction.PLACE) {
                building = (ProductionBuilding) findBuilding(BuildingServerEvents.getBuildings(), this.buildingPos);
                if (building == null)
                    return;
            }
            switch (this.action) {
                case PLACE -> BuildingServerEvents.placeBuilding(this.itemName, this.buildingPos, this.rotation, this.ownerName);
                case CANCEL -> {
                    BuildingServerEvents.cancelBuilding(building);
                }
                case REPAIR -> System.out.println("REPAIR");
                case SET_RALLY_POINT -> {
                    building.setRallyPoint(rallyPos);
                }
                case START_PRODUCTION -> {
                    ProductionBuilding.startProductionItem(building, this.itemName, this.buildingPos);
                    BuildingClientboundPacket.startProduction(buildingPos, itemName);
                }
                case CANCEL_PRODUCTION -> {
                    ProductionBuilding.cancelProductionItem(building, this.itemName, this.buildingPos, true);
                    BuildingClientboundPacket.cancelProduction(buildingPos, itemName, true);
                }
                case CANCEL_BACK_PRODUCTION -> {
                    ProductionBuilding.cancelProductionItem(building, this.itemName, this.buildingPos, false);
                    BuildingClientboundPacket.cancelProduction(buildingPos, itemName, false);
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
