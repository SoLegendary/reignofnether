package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
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
    public int[] builderUnitIds;
    public BuildingAction action;

    public static void placeBuilding(String itemName, BlockPos originPos, Rotation rotation, String ownerName, int[] builderUnitIds) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.PLACE,
                itemName, originPos, BlockPos.ZERO, rotation, ownerName, builderUnitIds));
    }
    public static void cancelBuilding(BlockPos buildingPos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.CANCEL,
                "", buildingPos, BlockPos.ZERO, Rotation.NONE, "", new int[0]));
    }
    public static void setRallyPoint(BlockPos buildingPos, BlockPos rallyPos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.SET_RALLY_POINT,
                "", buildingPos, rallyPos, Rotation.NONE, "", new int[0]));
    }
    public static void startProduction(BlockPos buildingPos, String itemName) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.START_PRODUCTION,
                itemName, buildingPos, BlockPos.ZERO, Rotation.NONE, "", new int[0]));
    }
    public static void cancelProduction(BlockPos buildingPos, String itemName, boolean frontItem) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                frontItem ? BuildingAction.CANCEL_PRODUCTION : BuildingAction.CANCEL_BACK_PRODUCTION,
                itemName, buildingPos, BlockPos.ZERO, Rotation.NONE, "", new int[0]));
    }


    public BuildingServerboundPacket(BuildingAction action, String itemName, BlockPos buildingPos, BlockPos rallyPos, Rotation rotation, String ownerName, int[] builderUnitIds) {
        this.action = action;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
        this.rallyPos = rallyPos;
        this.rotation = rotation;
        this.ownerName = ownerName;
        this.builderUnitIds = builderUnitIds;
    }

    public BuildingServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemName = buffer.readUtf();
        this.buildingPos = buffer.readBlockPos();
        this.rallyPos = buffer.readBlockPos();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
        this.builderUnitIds = buffer.readVarIntArray();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.buildingPos);
        buffer.writeBlockPos(this.rallyPos);
        buffer.writeEnum(this.rotation);
        buffer.writeUtf(this.ownerName);
        buffer.writeVarIntArray(this.builderUnitIds);
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
                case PLACE -> BuildingServerEvents.placeBuilding(this.itemName, this.buildingPos, this.rotation, this.ownerName, this.builderUnitIds);
                case CANCEL -> {
                    BuildingServerEvents.cancelBuilding(building);
                }
                case SET_RALLY_POINT -> {
                    building.setRallyPoint(rallyPos);
                }
                case START_PRODUCTION -> {
                    boolean prodSuccess = ProductionBuilding.startProductionItem(building, this.itemName, this.buildingPos);
                    if (prodSuccess)
                        BuildingClientboundPacket.startProduction(buildingPos, itemName);
                    System.out.println("(server) START_PRODUCTION");
                }
                case CANCEL_PRODUCTION -> {
                    boolean prodSuccess = ProductionBuilding.cancelProductionItem(building, this.itemName, this.buildingPos, true);
                    if (prodSuccess)
                        BuildingClientboundPacket.cancelProduction(buildingPos, itemName, true);
                    System.out.println("(server) CANCEL_PRODUCTION");
                }
                case CANCEL_BACK_PRODUCTION -> {
                    boolean prodSuccess = ProductionBuilding.cancelProductionItem(building, this.itemName, this.buildingPos, false);
                    if (prodSuccess)
                        BuildingClientboundPacket.cancelProduction(buildingPos, itemName, false);
                    System.out.println("(server) CANCEL_BACK_PRODUCTION");
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
