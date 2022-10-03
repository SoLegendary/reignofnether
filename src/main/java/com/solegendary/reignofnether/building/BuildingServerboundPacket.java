package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class BuildingServerboundPacket {
    // pos is used to identify the building object serverside
    public BlockPos pos; // required for all actions (used to identify the relevant building)
    public String itemName; // name of the building or production item // PLACE, START_PRODUCTION, CANCEL_PRODUCTION
    public Rotation rotation; // PLACE
    public String ownerName; // PLACE
    public int repairAmount; // REPAIR
    public BuildingAction action;

    public static void placeBuilding(String itemName, BlockPos originPos, Rotation rotation, String ownerName) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.PLACE,
                itemName, originPos, rotation, ownerName, 0));
    }
    public static void cancelBuilding(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.CANCEL,
                "", pos, Rotation.NONE, "", 0));
    }
    public static void repairBuilding(BlockPos pos, int repairAmount) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.REPAIR,
                "", pos, Rotation.NONE, "", repairAmount));
    }
    public static void startProduction(BlockPos pos, String itemName) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.START_PRODUCTION,
                itemName, pos, Rotation.NONE, "", 0));
    }
    public static void cancelProduction(BlockPos pos, String itemName, boolean frontItem) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                frontItem ? BuildingAction.CANCEL_PRODUCTION : BuildingAction.CANCEL_BACK_PRODUCTION,
                itemName, pos, Rotation.NONE, "", 0));
    }


    public BuildingServerboundPacket(BuildingAction action, String itemName, BlockPos pos, Rotation rotation, String ownerName, int repairAmount) {
        this.action = action;
        this.itemName = itemName;
        this.pos = pos;
        this.rotation = rotation;
        this.ownerName = ownerName;
        this.repairAmount = repairAmount;
    }

    public BuildingServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemName = buffer.readUtf();
        this.pos = buffer.readBlockPos();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
        this.repairAmount = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.pos);
        buffer.writeEnum(this.rotation);
        buffer.writeUtf(this.ownerName);
        buffer.writeInt(this.repairAmount);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            switch (this.action) {
                case PLACE -> BuildingServerEvents.placeBuilding(this.itemName, this.pos, this.rotation, this.ownerName);
                case CANCEL -> System.out.println("CANCEL");
                case REPAIR -> System.out.println("REPAIR");
                case START_PRODUCTION -> {
                    ProductionBuilding.startProductionItem(BuildingServerEvents.getBuildings(), this.itemName, this.pos);
                    BuildingClientboundPacket.startProduction(pos, itemName);
                }
                case CANCEL_PRODUCTION -> {
                    ProductionBuilding.cancelProductionItem(BuildingServerEvents.getBuildings(), this.itemName, this.pos, true);
                    BuildingClientboundPacket.cancelProduction(pos, itemName, true);
                }
                case CANCEL_BACK_PRODUCTION -> {
                    ProductionBuilding.cancelProductionItem(BuildingServerEvents.getBuildings(), this.itemName, this.pos, false);
                    BuildingClientboundPacket.cancelProduction(pos, itemName, false);
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
