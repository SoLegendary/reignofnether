package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.building.BuildingUtils.findBuilding;

public class BuildingProductionServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<BuildingProductionServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "building_production_serverbound"));

    @Override
    public CustomPacketPayload.Type<BuildingProductionServerboundPacket> type() {
        return TYPE;
    }
    public String itemName; // resource location string of the ProductionItem
    public BlockPos buildingPos; // used to identify the relevant production building
    public BuildingAction action;

    public static void startProduction(ProductionItem item) {
        BuildingClientEvents.switchHudToIdlestBuilding();
        if (HudClientEvents.hudSelectedPlacement instanceof ProductionPlacement pp) {
            String prodItemName;
            if (pp instanceof CustomBuildingPlacement) {
                prodItemName = item.getItemName();
            } else {
                prodItemName = ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item).toString();
            }
            if (prodItemName != null) {
                PacketHandler.sendToServer(new BuildingProductionServerboundPacket(
                        BuildingAction.START_PRODUCTION,
                        prodItemName,
                        pp.originPos));
            }
        }
    }

    public static void cancelProduction(BlockPos buildingPos, ProductionItem item, boolean frontItem) {
        String prodItemName;
        if (HudClientEvents.hudSelectedPlacement instanceof ProductionPlacement pp &&
                pp instanceof CustomBuildingPlacement) {
            prodItemName = item.getItemName();
        } else {
            prodItemName = ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item).toString();
        }
        if (prodItemName != null) {
            PacketHandler.sendToServer(new BuildingProductionServerboundPacket(
                    frontItem ? BuildingAction.CANCEL_PRODUCTION : BuildingAction.CANCEL_BACK_PRODUCTION,
                    prodItemName, buildingPos));
        }
    }

    public static void requestSync(BlockPos buildingPos) {
        PacketHandler.sendToServer(new BuildingProductionServerboundPacket(
                BuildingAction.REQUEST_PRODUCTION_SYNC, "", buildingPos));
    }

    public BuildingProductionServerboundPacket(BuildingAction action, String itemName, BlockPos buildingPos) {
        this.action = action;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
    }

    public BuildingProductionServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemName = buffer.readUtf();
        this.buildingPos = buffer.readBlockPos();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.buildingPos);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            BuildingPlacement building = findBuilding(false, this.buildingPos);
            if (building == null)
                return;

            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                ReignOfNether.LOGGER.warn("Sender for unit action packet was null");
                return;
            }
            else if (!player.getName().getString().equals(building.ownerName) &&
                    !SandboxServer.isAnyoneASandboxPlayer() &&
                    !AlliancesServerEvents.canControlAlly(player.getName().getString(), "")) {

                // Every client asks for a production sync for each building it learns about, owned or not
                // (BuildingClientEvents), so a dropped sync request is the expected case, not an attempt to
                // control someone else's production. Only the latter is worth a warning.
                if (this.action == BuildingAction.REQUEST_PRODUCTION_SYNC)
                    ReignOfNether.LOGGER.debug("BuildingProductionServerboundPacket: ignoring out-of-ownership sync from " + player.getName() + " for " + building.ownerName);
                else
                    ReignOfNether.LOGGER.warn("BuildingProductionServerboundPacket: Tried to process packet from " + player.getName() + " for " + building.ownerName);
                return;
            }
            if (building instanceof ProductionPlacement pBuilding) {
                if (this.action == BuildingAction.REQUEST_PRODUCTION_SYNC) {
                    for (ActiveProduction activeProd : pBuilding.productionQueue) {
                        BuildingProductionClientboundPacket.startProduction(
                                pBuilding.ownerName,
                                buildingPos,
                                activeProd.item,
                                activeProd.ticksLeft
                        );
                    }
                } else {
                    ProductionItem productionItem;
                    if (pBuilding instanceof CustomBuildingPlacement cbp) {
                        productionItem = cbp.getProductionItem(this.itemName);
                    } else {
                        productionItem = ReignOfNetherRegistries.PRODUCTION_ITEM.get(ResourceLocation.tryParse(this.itemName));
                    }
                    if (productionItem != null) {
                        switch (this.action) {
                            case START_PRODUCTION -> {
                                boolean prodSuccess = pBuilding.startProductionItem(productionItem);
                                if (prodSuccess)
                                    BuildingProductionClientboundPacket.startProduction(pBuilding.ownerName, buildingPos, this.itemName);
                            }
                            case CANCEL_PRODUCTION -> {
                                boolean cancelSuccess = pBuilding.cancelProductionItem(productionItem, true);
                                if (cancelSuccess || pBuilding.productionQueue.isEmpty())
                                    BuildingProductionClientboundPacket.cancelProduction(pBuilding.ownerName, buildingPos, this.itemName, true);
                            }
                            case CANCEL_BACK_PRODUCTION -> {
                                boolean cancelSuccess = pBuilding.cancelProductionItem(productionItem, false);
                                if (cancelSuccess || pBuilding.productionQueue.isEmpty())
                                    BuildingProductionClientboundPacket.cancelProduction(pBuilding.ownerName, buildingPos, this.itemName, false);
                            }
                            default -> { }
                        }
                    }
                }
            }
            ReignOfNether.LOGGER.info("[Building] {} performed {} (itemName: {}, pos: {})", player.getName(), this.action, this.itemName, this.buildingPos);
        });
        return;
    }
}