package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.buildings.piglins.Portal;
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
    public BuildingAction action;
    public BlockPos buildingPos;
    public String itemName;
    public Rotation rotation;
    public String ownerName;
    public int blocksPlaced; // for syncing out-of-view clientside buildings
    public int numQueuedBlocks; // used for delaying destroy checks clientside

    public static void placeBuilding(BlockPos buildingPos, String itemName, Rotation rotation, String ownerName, int numQueuedBlocks) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.PLACE,
                    itemName, buildingPos, rotation, ownerName, 0, numQueuedBlocks));
    }
    public static void destroyBuilding(BlockPos buildingPos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new BuildingClientboundPacket(BuildingAction.DESTROY,
                        "", buildingPos, Rotation.NONE, "", 0, 0));
    }
    public static void syncBuilding(BlockPos buildingPos, int blocksPlaced) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new BuildingClientboundPacket(BuildingAction.SYNC_BLOCKS,
                        "", buildingPos, Rotation.NONE, "", blocksPlaced, 0));
    }
    public static void startProduction(BlockPos buildingPos, String itemName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(
            BuildingAction.START_PRODUCTION,
            itemName, buildingPos, Rotation.NONE, "", 0, 0));
    }
    public static void cancelProduction(BlockPos buildingPos, String itemName, boolean frontItem) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new BuildingClientboundPacket(
                        frontItem ? BuildingAction.CANCEL_PRODUCTION : BuildingAction.CANCEL_BACK_PRODUCTION,
                        itemName, buildingPos, Rotation.NONE, "", 0, 0));
    }
    public static void changePortal(BlockPos buildingPos, String portalType) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new BuildingClientboundPacket(
                        BuildingAction.CHANGE_PORTAL, portalType, buildingPos,
                        Rotation.NONE, "", 0, 0));
    }

    public BuildingClientboundPacket(BuildingAction action, String itemName, BlockPos buildingPos, Rotation rotation,
                                     String ownerName, int blocksPlaced, int numQueuedBlocks) {
        this.action = action;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
        this.rotation = rotation;
        this.ownerName = ownerName;
        this.blocksPlaced = blocksPlaced;
        this.numQueuedBlocks = numQueuedBlocks;
    }

    public BuildingClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemName = buffer.readUtf();
        this.buildingPos = buffer.readBlockPos();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
        this.blocksPlaced = buffer.readInt();
        this.numQueuedBlocks = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.buildingPos);
        buffer.writeEnum(this.rotation);
        buffer.writeUtf(this.ownerName);
        buffer.writeInt(this.blocksPlaced);
        buffer.writeInt(this.numQueuedBlocks);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> {
                Building building = null;
                if (this.action != BuildingAction.PLACE) {
                    building = findBuilding(true, this.buildingPos);
                    if (building == null)
                        return;
                }
                switch (action) {
                    case PLACE -> BuildingClientEvents.placeBuilding(this.itemName, this.buildingPos, this.rotation, this.ownerName, this.numQueuedBlocks);
                    case DESTROY -> BuildingClientEvents.destroyBuilding(this.buildingPos);
                    case SYNC_BLOCKS -> BuildingClientEvents.syncBuildingBlocks(building, this.blocksPlaced);
                    case START_PRODUCTION -> {
                        ProductionBuilding.startProductionItem((ProductionBuilding) building, this.itemName, this.buildingPos);
                    }
                    case CANCEL_PRODUCTION -> {
                        ProductionBuilding.cancelProductionItem((ProductionBuilding) building, this.itemName, this.buildingPos, true);
                    }
                    case CANCEL_BACK_PRODUCTION -> {
                        ProductionBuilding.cancelProductionItem((ProductionBuilding) building, this.itemName, this.buildingPos, false);
                    }
                    case CHANGE_PORTAL -> {
                        if (building instanceof Portal portal)
                            portal.changeStructure(Portal.PortalType.valueOf(itemName));
                    }
                }
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
