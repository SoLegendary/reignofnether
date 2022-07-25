package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class BuildingServerboundPacket {

    private static Minecraft MC = Minecraft.getInstance();

    // pos is used to identify the building object serverside
    public BlockPos pos; // PLACE, CANCEL, DESTROY, REPAIR
    public String buildingName; // PLACE
    public Rotation rotation; // PLACE
    public int repairAmount; // REPAIR
    public BuildingAction action;

    public static void placeBuilding(String buildingName, BlockPos originPos, Rotation rotation) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(BuildingAction.PLACE, buildingName, originPos, rotation, 0));
    }
    public static void repairBuilding(BlockPos pos, int repairAmount) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(BuildingAction.REPAIR, null, pos, null, repairAmount));
    }
    public static void destroyBuilding(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(BuildingAction.DESTROY, null, pos, null, 0));
    }
    public static void cancelBuilding(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(BuildingAction.CANCEL, null, pos, null, 0));
    }

    public BuildingServerboundPacket(BuildingAction action, String buildingName, BlockPos pos, Rotation rotation, int repairAmount) {
        this.action = action;
        this.buildingName = buildingName;
        this.pos = pos;
        this.rotation = rotation;
        this.repairAmount = repairAmount;
    }

    public BuildingServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.buildingName = buffer.readUtf();
        this.pos = buffer.readBlockPos();
        this.rotation = buffer.readEnum(Rotation.class);
        this.repairAmount = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.buildingName);
        buffer.writeBlockPos(this.pos);
        buffer.writeEnum(this.rotation);
        buffer.writeInt(this.repairAmount);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            if (this.action == BuildingAction.PLACE)
                BuildingServerEvents.placeBuilding(this.buildingName, this.pos, this.rotation);
            else if (this.action == BuildingAction.CANCEL)
                System.out.println("CANCEL");
            else if (this.action == BuildingAction.DESTROY)
                BuildingServerEvents.destroyBuilding(this.pos);
            else if (this.action == BuildingAction.REPAIR)
                System.out.println("REPAIR");

            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
