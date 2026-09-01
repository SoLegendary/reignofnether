package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.building.BuildingUtils.findBuilding;

public class BuildingProductionServerboundPacket {
    public String itemName; // resource location string of the ProductionItem
    public BlockPos buildingPos; // used to identify the relevant production building
    public BuildingAction action;

    public static void startProduction(ProductionItem item) {
        BuildingClientEvents.switchHudToIdlestBuilding();
        if (HudClientEvents.hudSelectedPlacement instanceof ProductionPlacement pp) {
            PacketHandler.INSTANCE.sendToServer(new BuildingProductionServerboundPacket(
                    BuildingAction.START_PRODUCTION,
                    ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item).toString(),
                    pp.originPos));
        }
    }

    public static void cancelProduction(BlockPos buildingPos, ProductionItem item, boolean frontItem) {
        PacketHandler.INSTANCE.sendToServer(new BuildingProductionServerboundPacket(
                frontItem ? BuildingAction.CANCEL_PRODUCTION : BuildingAction.CANCEL_BACK_PRODUCTION,
                ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item).toString(), buildingPos));
    }

    public static void requestSync(BlockPos buildingPos) {
        PacketHandler.INSTANCE.sendToServer(new BuildingProductionServerboundPacket(
                BuildingAction.REQUEST_PRODUCTION_SYNC, "", buildingPos));
    }

    public BuildingProductionServerboundPacket(BuildingAction action, String itemName, BlockPos buildingPos) {
        this.action = action;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
    }

    public BuildingProductionServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemName = buffer.readUtf();
        this.buildingPos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.buildingPos);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            BuildingPlacement building = findBuilding(false, this.buildingPos);
            if (building == null)
                return;

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("Sender for unit action packet was null");
                success.set(false);
                return;
            }
            else if (!player.getName().getString().equals(building.ownerName) &&
                    !SandboxServer.isAnyoneASandboxPlayer() &&
                    !AlliancesServerEvents.canControlAlly(player.getName().getString(), "")) {

                ReignOfNether.LOGGER.warn("BuildingProductionServerboundPacket: Tried to process packet from " + player.getName() + " for " + building.ownerName);
                success.set(false);
                return;
            }
            if (building instanceof ProductionPlacement pBuilding) {
                switch (this.action) {
                    case START_PRODUCTION -> {
                        boolean prodSuccess = pBuilding.startProductionItem(ReignOfNetherRegistries.PRODUCTION_ITEM.get(ResourceLocation.tryParse(this.itemName)));
                        if (prodSuccess)
                            BuildingProductionClientboundPacket.startProduction(buildingPos, ReignOfNetherRegistries.PRODUCTION_ITEM.get(ResourceLocation.tryParse(itemName)));
                    }
                    case CANCEL_PRODUCTION -> {
                        boolean cancelSuccess = pBuilding.cancelProductionItem(ReignOfNetherRegistries.PRODUCTION_ITEM.get(ResourceLocation.tryParse(this.itemName)), true);
                        if (cancelSuccess || pBuilding.productionQueue.isEmpty())
                            BuildingProductionClientboundPacket.cancelProduction(buildingPos, ReignOfNetherRegistries.PRODUCTION_ITEM.get(ResourceLocation.tryParse(itemName)), true);
                    }
                    case CANCEL_BACK_PRODUCTION -> {
                        boolean cancelSuccess = pBuilding.cancelProductionItem(ReignOfNetherRegistries.PRODUCTION_ITEM.get(ResourceLocation.tryParse(this.itemName)), false);
                        if (cancelSuccess || pBuilding.productionQueue.isEmpty())
                            BuildingProductionClientboundPacket.cancelProduction(buildingPos, ReignOfNetherRegistries.PRODUCTION_ITEM.get(ResourceLocation.tryParse(itemName)), false);
                    }
                    case REQUEST_PRODUCTION_SYNC -> {
                        for (ActiveProduction activeProd : pBuilding.productionQueue) {
                            BuildingProductionClientboundPacket.startProduction(
                                    buildingPos,
                                    activeProd.item,
                                    activeProd.ticksLeft
                            );
                        }
                    }
                    default -> { }
                }
            }
            ReignOfNether.LOGGER.info("[Building] {} performed {} (itemName: {}, pos: {})", player.getName(), this.action, this.itemName, this.buildingPos);
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}