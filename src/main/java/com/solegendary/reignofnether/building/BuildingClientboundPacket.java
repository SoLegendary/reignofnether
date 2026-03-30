package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.building.BuildingUtils.findBuilding;

public class BuildingClientboundPacket {
    public static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath("", "");

    // pos is used to identify the building object serverside
    public BuildingAction action;
    public BlockPos buildingPos;
    public ResourceLocation itemKey;
    public String itemName;
    public Rotation rotation;
    public String ownerName;
    public int scenarioRoleIndex;
    public int blocksPlaced; // for syncing out-of-view clientside buildings
    public int numQueuedBlocks; // used for delaying destroy checks clientside
    public boolean isDiagonalBridge;
    public int upgradeLevel;
    public boolean isBuilt;
    public PortalPlacement.PortalType portalType;
    public BlockPos portalDestination;
    public boolean forPlayerLoggingIn; // is this placement for someone logging in currently joined?

    public static void placeBuilding(
        BlockPos buildingPos,
        Building building,
        Rotation rotation,
        String ownerName,
        int scenarioRoleIndex,
        int numQueuedBlocks,
        boolean isDiagonalBridge,
        int upgradeLevel,
        boolean isBuilt,
        PortalPlacement.PortalType portalType,
        BlockPos portalDestination,
        boolean forPlayerLoggingIn
    ) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new BuildingClientboundPacket(
            building instanceof CustomBuilding ? BuildingAction.PLACE_CUSTOM : BuildingAction.PLACE,
            building instanceof CustomBuilding ? EMPTY : ReignOfNetherRegistries.BUILDING.getKey(building),
            building.name,
            buildingPos,
            rotation,
            ownerName,
            scenarioRoleIndex,
            0,
            numQueuedBlocks,
            isDiagonalBridge,
            upgradeLevel,
            isBuilt,
            portalType,
            portalDestination,
            forPlayerLoggingIn
        ));
    }

    public static void syncBuilding(BlockPos buildingPos, int blocksPlaced, String ownerName, int scenarioRoleIndex) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.SYNC_BLOCKS_AND_OWNER,
                EMPTY,
                "",
                buildingPos,
                Rotation.NONE,
                ownerName,
                scenarioRoleIndex,
                blocksPlaced,
                0,
                false,
                0,
                false,
                PortalPlacement.PortalType.BASIC,
                new BlockPos(0,0,0),
                false
            )
        );
    }

    public static void startProduction(BlockPos buildingPos, ProductionItem item) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.START_PRODUCTION,
                ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item),
                "",
                buildingPos
            )
        );
    }

    public static void cancelProduction(BlockPos buildingPos, ProductionItem item, boolean frontItem) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(frontItem
                                          ? BuildingAction.CANCEL_PRODUCTION
                                          : BuildingAction.CANCEL_BACK_PRODUCTION,
                ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item),
                "",
                buildingPos
            )
        );
    }

    public static void changePortal(BlockPos buildingPos, PortalPlacement.PortalType type) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.CHANGE_PORTAL,
                EMPTY,
                "",
                buildingPos,
                Rotation.NONE,
                "",
                0,
                0,
                0,
                false,
                0,
                false,
                type,
                new BlockPos(0,0,0),
                false
            )
        );
    }

    public static void clearQueue(BlockPos buildingPos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.CLEAR_PRODUCTION, EMPTY,"", buildingPos)
        );
    }

    public static void completeProduction(BlockPos buildingPos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.COMPLETE_PRODUCTION, EMPTY,"", buildingPos)
        );
    }

    public static void removeBuilding(BlockPos buildingPos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new BuildingClientboundPacket(BuildingAction.REMOVE, EMPTY, "", buildingPos)
        );
    }

    public BuildingClientboundPacket(
            BuildingAction action,
            ResourceLocation itemKey,
            String itemName,
            BlockPos buildingPos
    ) {
        this.action = action;
        this.itemKey = itemKey;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
        this.rotation = Rotation.NONE;
        this.ownerName = "";
        this.scenarioRoleIndex = 0;
        this.blocksPlaced = 0;
        this.numQueuedBlocks = 0;
        this.isDiagonalBridge = false;
        this.isBuilt = false;
        this.upgradeLevel = 0;
        this.portalType = PortalPlacement.PortalType.BASIC;
        this.portalDestination = new BlockPos(0,0,0);
        this.forPlayerLoggingIn = false;
    }

    public BuildingClientboundPacket(
        BuildingAction action,
        ResourceLocation itemKey,
        String itemName,
        BlockPos buildingPos,
        Rotation rotation,
        String ownerName,
        int scenarioRoleIndex,
        int blocksPlaced,
        int numQueuedBlocks,
        boolean isDiagonalBridge,
        int upgradeLevel,
        boolean isBuilt,
        PortalPlacement.PortalType portalType,
        BlockPos portalDestination,
        boolean forPlayerLoggingIn
    ) {
        this.action = action;
        this.itemKey = itemKey;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
        this.rotation = rotation;
        this.ownerName = ownerName;
        this.scenarioRoleIndex = scenarioRoleIndex;
        this.blocksPlaced = blocksPlaced;
        this.numQueuedBlocks = numQueuedBlocks;
        this.isDiagonalBridge = isDiagonalBridge;
        this.isBuilt = isBuilt;
        this.upgradeLevel = upgradeLevel;
        this.portalType = portalType;
        this.portalDestination = portalDestination;
        this.forPlayerLoggingIn = forPlayerLoggingIn;
    }

    public BuildingClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemKey = buffer.readResourceLocation();
        this.itemName = buffer.readUtf();
        this.buildingPos = buffer.readBlockPos();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
        this.scenarioRoleIndex = buffer.readInt();
        this.blocksPlaced = buffer.readInt();
        this.numQueuedBlocks = buffer.readInt();
        this.isDiagonalBridge = buffer.readBoolean();
        this.isBuilt = buffer.readBoolean();
        this.upgradeLevel = buffer.readInt();
        this.portalType = buffer.readEnum(PortalPlacement.PortalType.class);
        this.portalDestination = buffer.readBlockPos();
        this.forPlayerLoggingIn = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeResourceLocation(this.itemKey);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.buildingPos);
        buffer.writeEnum(this.rotation);
        buffer.writeUtf(this.ownerName);
        buffer.writeInt(this.scenarioRoleIndex);
        buffer.writeInt(this.blocksPlaced);
        buffer.writeInt(this.numQueuedBlocks);
        buffer.writeBoolean(this.isDiagonalBridge);
        buffer.writeBoolean(this.isBuilt);
        buffer.writeInt(this.upgradeLevel);
        buffer.writeEnum(this.portalType);
        buffer.writeBlockPos(this.portalDestination);
        buffer.writeBoolean(this.forPlayerLoggingIn);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                BuildingPlacement building = null;
                if (this.action != BuildingAction.PLACE &&
                    this.action != BuildingAction.PLACE_CUSTOM) {
                    building = findBuilding(true, this.buildingPos);
                    if (building == null) {

                        // if the client was missing a building, replace it
                        if (this.action == BuildingAction.SYNC_BLOCKS_AND_OWNER) {
                            BuildingServerboundPacket.requestReplacement(this.buildingPos);
                            //ReignOfNether.LOGGER.warn("Missing building");
                        }
                        return;
                    }
                }
                switch (action) {
                    case PLACE -> BuildingClientEvents.placeBuilding(
                            ReignOfNetherRegistries.BUILDING.get(this.itemKey),
                            this.buildingPos,
                            this.rotation,
                            this.ownerName,
                            this.numQueuedBlocks,
                            this.isDiagonalBridge,
                            this.upgradeLevel,
                            this.isBuilt,
                            this.portalType,
                            this.portalDestination,
                            this.forPlayerLoggingIn
                    );
                    case PLACE_CUSTOM -> BuildingClientEvents.placeBuilding(
                            CustomBuildingClientEvents.getCustomBuilding(this.itemName),
                            this.buildingPos,
                            this.rotation,
                            this.ownerName,
                            this.numQueuedBlocks,
                            this.isDiagonalBridge,
                            this.upgradeLevel,
                            this.isBuilt,
                            this.portalType,
                            this.portalDestination,
                            this.forPlayerLoggingIn
                    );
                    case SYNC_BLOCKS_AND_OWNER -> BuildingClientEvents.syncBuilding(building, this.blocksPlaced, this.ownerName, this.scenarioRoleIndex);
                    case START_PRODUCTION -> {
                        ((ProductionPlacement) building).startProductionItem(
                            ReignOfNetherRegistries.PRODUCTION_ITEM.get(itemKey)
                        );
                    }
                    case CANCEL_PRODUCTION -> {
                        ((ProductionPlacement) building).cancelProductionItem(
                                ReignOfNetherRegistries.PRODUCTION_ITEM.get(itemKey),
                            true
                        );
                    }
                    case CANCEL_BACK_PRODUCTION -> {
                        ((ProductionPlacement) building).cancelProductionItem(
                                ReignOfNetherRegistries.PRODUCTION_ITEM.get(itemKey),
                            false
                        );
                    }
                    case CHANGE_PORTAL -> {
                        if (building instanceof PortalPlacement portal) {
                            portal.changePortalStructure(portalType);
                        }
                    }
                    case CLEAR_PRODUCTION -> {
                        if (building instanceof ProductionPlacement pBuilding) {
                            if (!pBuilding.productionQueue.isEmpty()) {
                                ActiveProduction pItem = pBuilding.productionQueue.get(0);
                                if (!pItem.completed) {
                                    pItem.completed = true;
                                    pItem.item.onComplete.accept(pBuilding.level, pBuilding);
                                }
                                pBuilding.productionQueue.clear();
                            }
                        }
                    }
                    case COMPLETE_PRODUCTION -> {
                        if (building instanceof ProductionPlacement pBuilding) {
                            if (!pBuilding.productionQueue.isEmpty()) {
                                ActiveProduction pItem = pBuilding.productionQueue.get(0);
                                if (!pItem.completed) {
                                    pItem.completed = true;
                                    pItem.item.onComplete.accept(pBuilding.level, pBuilding);
                                }
                                pBuilding.productionQueue.remove(pItem);
                            }
                        }
                    }
                    case REMOVE -> {
                        BuildingClientEvents.removeBuilding(buildingPos);
                    }
                }
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
