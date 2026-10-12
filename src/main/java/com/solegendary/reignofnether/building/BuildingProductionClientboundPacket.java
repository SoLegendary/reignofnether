package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.function.Supplier;

import static com.solegendary.reignofnether.building.BuildingUtils.findBuilding;

public class BuildingProductionClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<BuildingProductionClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "building_production_clientbound"));

    @Override
    public CustomPacketPayload.Type<BuildingProductionClientboundPacket> type() {
        return TYPE;
    }
    public static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath("", "");

    // pos is used to identify the building object serverside
    public BuildingAction action;
    public BlockPos buildingPos;
    public String itemName;
    public float ticksLeft;

    // send only to players whose fog reveals at least one corner of this building
    private static void sendFiltered(String ownerName, BlockPos buildingPos, BuildingProductionClientboundPacket packet) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        BuildingPlacement b = findBuilding(false, buildingPos);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (ownerName.equals(sp.getName().getString()) ||
                (b != null && FogOfWarServerEvents.canPlayerSeeBuilding(sp, b))) {
                PacketHandler.send(PacketHandler.toPlayer(() -> sp), packet);
            }
        }
    }

    public static void startProduction(String ownerName, BlockPos buildingPos, String itemName) {
        sendFiltered(ownerName, buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.START_PRODUCTION,
                        itemName,
                        buildingPos
                )
        );
    }

    public static void startProduction(String ownerName, BlockPos buildingPos, ProductionItem item, float ticksLeft) {
        sendFiltered(ownerName, buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.START_PRODUCTION,
                        ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item).toString(),
                        buildingPos,
                        ticksLeft
                )
        );
    }

    public static void cancelProduction(String ownerName, BlockPos buildingPos, String itemName, boolean frontItem) {
        sendFiltered(ownerName, buildingPos,
                new BuildingProductionClientboundPacket(frontItem
                        ? BuildingAction.CANCEL_PRODUCTION
                        : BuildingAction.CANCEL_BACK_PRODUCTION,
                        itemName,
                        buildingPos
                )
        );
    }

    public static void clearQueue(String ownerName, BlockPos buildingPos) {
        sendFiltered(ownerName, buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.CLEAR_PRODUCTION, "", buildingPos)
        );
    }

    public static void completeProduction(String ownerName, BlockPos buildingPos, String itemName) {
        sendFiltered(ownerName, buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.COMPLETE_PRODUCTION, itemName, buildingPos)
        );
    }

    public BuildingProductionClientboundPacket(
            BuildingAction action,
            String itemName,
            BlockPos buildingPos
    ) {
        this.action = action;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
        this.ticksLeft = -1;
    }

    public BuildingProductionClientboundPacket(
            BuildingAction action,
            String itemName,
            BlockPos buildingPos,
            float ticksLeft
    ) {
        this.action = action;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
        this.ticksLeft = ticksLeft;
    }

    public BuildingProductionClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemName = buffer.readUtf();
        this.buildingPos = buffer.readBlockPos();
        this.ticksLeft = buffer.readFloat();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.buildingPos);
        buffer.writeFloat(this.ticksLeft);
    }

    // client-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                BuildingPlacement building = findBuilding(true, this.buildingPos);
                if (building == null) {
                    return;
                }

                ProductionItem productionItem;
                if (building instanceof CustomBuildingPlacement cbp) {
                    productionItem = cbp.getProductionItem(this.itemName);
                } else {
                    productionItem = ReignOfNetherRegistries.PRODUCTION_ITEM.get(ResourceLocation.tryParse(this.itemName));
                }

                switch (action) {
                    case START_PRODUCTION -> {
                        ((ProductionPlacement) building).startProductionItem(
                                productionItem,
                                ticksLeft
                        );
                    }
                    case CANCEL_PRODUCTION -> {
                        ((ProductionPlacement) building).cancelProductionItem(
                                productionItem,
                                true
                        );
                    }
                    case CANCEL_BACK_PRODUCTION -> {
                        ((ProductionPlacement) building).cancelProductionItem(
                                productionItem,
                                false
                        );
                    }
                    case CLEAR_PRODUCTION -> {
                        if (building instanceof ProductionPlacement pBuilding) {
                            pBuilding.productionQueue.clear();
                        }
                    }
                    case COMPLETE_PRODUCTION -> {
                        if (building instanceof ProductionPlacement pBuilding) {
                            if (!pBuilding.productionQueue.isEmpty()) {
                                for (ActiveProduction pItem : pBuilding.productionQueue) {
                                    if (pItem.item.getItemName().equals(this.itemName) && !pItem.completed) {
                                        pItem.complete(pBuilding);
                                        break;
                                    }
                                }
                                pBuilding.productionQueue.removeIf(pItem -> pItem.completed);
                            }
                        }
                    }
                    default -> { }
                }
            });
        });
        return;
    }
}