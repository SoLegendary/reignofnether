package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.buildings.piglins.PortalCivilian;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.buildings.placements.StockpilePlacement;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.building.BuildingUtils.findBuilding;

public class BuildingServerboundPacket {
    public String itemName; // name of the building // PLACE
    public BlockPos buildingPos; // required for all actions (used to identify the relevant building)
    public BlockPos rallyPos;
    public Rotation rotation; // PLACE
    public String ownerName; // PLACE
    public int[] builderUnitIds;
    public BuildingAction action;
    public Boolean isDiagonalBridge;

    // does auth check against ownerName or against the existing building.ownerName?
    // if not in either list (eg. check stockpile, request replacement), no auth is needed
    private final static List<BuildingAction> newBuildingAuthActions = List.of(
            BuildingAction.PLACE,
            BuildingAction.PLACE_AND_QUEUE,
            BuildingAction.PLACE_CUSTOM,
            BuildingAction.PLACE_AND_QUEUE_CUSTOM
    );
    private final static List<BuildingAction> existingBuildingAuthActions = List.of(
            BuildingAction.DESTROY,
            BuildingAction.SET_RALLY_POINT,
            BuildingAction.ADD_RALLY_POINT,
            BuildingAction.SET_RALLY_POINT_ENTITY,
            BuildingAction.CHANGE_PORTAL
    );

    public static void placeBuilding(Building building, BlockPos originPos, Rotation rotation,
                                     String ownerName, int[] builderUnitIds, boolean isDiagonalBridge) {
        BuildingAction action = BuildingAction.PLACE_CUSTOM;
        String itemName = building.structureName;
        if (!(building instanceof CustomBuilding)) {
            action = BuildingAction.PLACE;
            itemName = ReignOfNetherRegistries.BUILDING.getKey(building).toString();
        }
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(action, itemName,
                originPos, BlockPos.ZERO, rotation, ownerName, builderUnitIds, isDiagonalBridge));
    }
    public static void placeAndQueueBuilding(Building building, BlockPos originPos, Rotation rotation,
                                             String ownerName, int[] builderUnitIds, boolean isDiagonalBridge) {
        BuildingAction action = BuildingAction.PLACE_AND_QUEUE_CUSTOM;
        String itemName = building.structureName;
        if (!(building instanceof CustomBuilding)) {
            action = BuildingAction.PLACE_AND_QUEUE;
            itemName = ReignOfNetherRegistries.BUILDING.getKey(building).toString();
        }
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(action, itemName,
                originPos, BlockPos.ZERO, rotation, ownerName, builderUnitIds, isDiagonalBridge));
    }
    public static void cancelBuilding(BlockPos buildingPos, String ownerName) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.DESTROY,
                "", buildingPos, BlockPos.ZERO, Rotation.NONE, ownerName, new int[0], false));
    }
    public static void setRallyPoint(BlockPos buildingPos, BlockPos rallyPos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.SET_RALLY_POINT,
                "", buildingPos, rallyPos, Rotation.NONE, "", new int[0], false));
    }
    public static void setAttackRallyPoint(BlockPos buildingPos, BlockPos rallyPos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.SET_ATTACK_RALLY_POINT,
                "", buildingPos, rallyPos, Rotation.NONE, "", new int[0], false));
    }
    public static void addRallyPoint(BlockPos buildingPos, BlockPos rallyPos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.ADD_RALLY_POINT,
                "", buildingPos, rallyPos, Rotation.NONE, "", new int[0], false));
    }
    public static void addAttackRallyPoint(BlockPos buildingPos, BlockPos rallyPos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.ADD_ATTACK_RALLY_POINT,
                "", buildingPos, rallyPos, Rotation.NONE, "", new int[0], false));
    }
    public static void setRallyPointEntity(BlockPos buildingPos, int entityId) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.SET_RALLY_POINT_ENTITY,
                "", buildingPos, BlockPos.ZERO, Rotation.NONE, "", new int[]{ entityId }, false));
    }
    public static void checkStockpileChests(BlockPos chestPos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.CHECK_STOCKPILE_CHEST,
                "", chestPos, BlockPos.ZERO, Rotation.NONE, "", new int[0], false));
    }
    public static void requestReplacement(BlockPos buildingPos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingServerboundPacket(
                BuildingAction.REQUEST_REPLACEMENT,
                "", buildingPos, BlockPos.ZERO, Rotation.NONE, "", new int[0], false));
    }

    public BuildingServerboundPacket(BuildingAction action, String itemName, BlockPos buildingPos, BlockPos rallyPos,
                                     Rotation rotation, String ownerName, int[] builderUnitIds, boolean isDiagonalBridge) {
        this.action = action;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
        this.rallyPos = rallyPos;
        this.rotation = rotation;
        this.ownerName = ownerName;
        this.builderUnitIds = builderUnitIds;
        this.isDiagonalBridge = isDiagonalBridge;
    }

    public BuildingServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemName = buffer.readUtf();
        this.buildingPos = buffer.readBlockPos();
        this.rallyPos = buffer.readBlockPos();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
        this.builderUnitIds = buffer.readVarIntArray();
        this.isDiagonalBridge = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.buildingPos);
        buffer.writeBlockPos(this.rallyPos);
        buffer.writeEnum(this.rotation);
        buffer.writeUtf(this.ownerName);
        buffer.writeVarIntArray(this.builderUnitIds);
        buffer.writeBoolean(this.isDiagonalBridge);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            BuildingPlacement building = null;
            if (!List.of(BuildingAction.PLACE, BuildingAction.PLACE_AND_QUEUE, BuildingAction.PLACE_CUSTOM, BuildingAction.PLACE_AND_QUEUE_CUSTOM).contains(this.action)) {
                building = findBuilding(false, this.buildingPos);
                if (building == null)
                    return;
            }

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("Sender for unit action packet was null");
                success.set(false);
                return;
            }
            else if (((newBuildingAuthActions.contains(this.action) &&
                    !player.getName().getString().equals(ownerName)) ||
                    (existingBuildingAuthActions.contains(this.action) && building != null &&
                            !player.getName().getString().equals(building.ownerName))) &&
                    !SandboxServer.isAnyoneASandboxPlayer() &&
                    !AlliancesServerEvents.canControlAlly(player.getName().getString(), ownerName)) {

                ReignOfNether.LOGGER.warn("BuildingServerboundPacket: Tried to process packet from " + player.getName() + " for " + ownerName);
                success.set(false);
                return;
            }
            ReignOfNether.LOGGER.info("[Building] {} performed {} for {} (itemName: {}, pos: {})", player.getName(), this.action, this.ownerName, this.itemName, this.buildingPos);
            switch (this.action) {
                case PLACE -> {
                    BuildingServerEvents.placeBuilding(ReignOfNetherRegistries.BUILDING.get(ResourceLocation.tryParse(this.itemName)), this.buildingPos, this.rotation, this.ownerName, this.builderUnitIds, false, isDiagonalBridge, false, false);
                }
                case PLACE_AND_QUEUE -> {
                    BuildingServerEvents.placeBuilding(ReignOfNetherRegistries.BUILDING.get(ResourceLocation.tryParse(this.itemName)), this.buildingPos, this.rotation, this.ownerName, this.builderUnitIds, true, isDiagonalBridge, false, false);
                }
                case PLACE_CUSTOM -> {
                    BuildingServerEvents.placeBuilding(CustomBuildingServerEvents.getCustomBuilding(this.itemName), this.buildingPos, this.rotation, this.ownerName, this.builderUnitIds, false, isDiagonalBridge, false, false);
                }
                case PLACE_AND_QUEUE_CUSTOM -> {
                    BuildingServerEvents.placeBuilding(CustomBuildingServerEvents.getCustomBuilding(this.itemName), this.buildingPos, this.rotation, this.ownerName, this.builderUnitIds, true, isDiagonalBridge, false, false);
                }
                case DESTROY -> {
                    BuildingServerEvents.cancelBuilding(building, this.ownerName);
                }
                case SET_RALLY_POINT -> {
                    if (building instanceof ProductionPlacement productionBuilding) {
                        productionBuilding.setRallyPoint(rallyPos);
                        productionBuilding.attackRally = false;
                    }
                }
                case SET_ATTACK_RALLY_POINT -> {
                    if (building instanceof ProductionPlacement productionBuilding) {
                        productionBuilding.setRallyPoint(rallyPos);
                        productionBuilding.attackRally = true;
                    }
                }
                case ADD_RALLY_POINT -> {
                    if (building instanceof ProductionPlacement productionBuilding) {
                        productionBuilding.addRallyPoint(rallyPos);
                        productionBuilding.attackRally = false;
                    }
                }
                case ADD_ATTACK_RALLY_POINT -> {
                    if (building instanceof ProductionPlacement productionBuilding) {
                        productionBuilding.addRallyPoint(rallyPos);
                        productionBuilding.attackRally = true;
                    }
                }
                case SET_RALLY_POINT_ENTITY -> {
                    if (building instanceof ProductionPlacement productionBuilding) {
                        Entity e = building.level.getEntity(this.builderUnitIds[0]);
                        if (e instanceof LivingEntity le)
                            productionBuilding.setRallyPointEntity(le);
                    }
                }
                case CHECK_STOCKPILE_CHEST -> {
                    if (building instanceof StockpilePlacement stockpile)
                        stockpile.checkAndConsumeChestItems();
                    else if (building instanceof PortalPlacement portal && portal.getBuilding() instanceof PortalCivilian)
                        portal.checkAndConsumeChestItems();
                }
                case REQUEST_REPLACEMENT -> {
                    BuildingServerEvents.replaceClientBuilding(buildingPos);
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}