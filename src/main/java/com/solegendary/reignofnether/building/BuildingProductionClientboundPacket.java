package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.building.BuildingUtils.findBuilding;

public class BuildingProductionClientboundPacket {
    public static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath("", "");

    // pos is used to identify the building object serverside
    public BuildingAction action;
    public BlockPos buildingPos;
    public String itemName;
    public float ticksLeft;

    // send only to players whose fog reveals at least one corner of this building
    private static void sendFiltered(BlockPos buildingPos, BuildingProductionClientboundPacket packet) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        BuildingPlacement b = findBuilding(false, buildingPos);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (b != null && FogOfWarServerEvents.canPlayerSeeBuilding(sp, b)) {
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), packet);
            }
        }
    }

    public static void startProduction(BlockPos buildingPos, String itemName) {
        sendFiltered(buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.START_PRODUCTION,
                        itemName,
                        buildingPos
                )
        );
    }

    public static void startProduction(BlockPos buildingPos, ProductionItem item, float ticksLeft) {
        sendFiltered(buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.START_PRODUCTION,
                        ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item).toString(),
                        buildingPos,
                        ticksLeft
                )
        );
    }

    public static void cancelProduction(BlockPos buildingPos, String itemName, boolean frontItem) {
        sendFiltered(buildingPos,
                new BuildingProductionClientboundPacket(frontItem
                        ? BuildingAction.CANCEL_PRODUCTION
                        : BuildingAction.CANCEL_BACK_PRODUCTION,
                        itemName,
                        buildingPos
                )
        );
    }

    public static void clearQueue(BlockPos buildingPos) {
        sendFiltered(buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.CLEAR_PRODUCTION, "", buildingPos)
        );
    }

    public static void completeProduction(BlockPos buildingPos) {
        sendFiltered(buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.COMPLETE_PRODUCTION, "", buildingPos)
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

    public BuildingProductionClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemName = buffer.readUtf();
        this.buildingPos = buffer.readBlockPos();
        this.ticksLeft = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.buildingPos);
        buffer.writeFloat(this.ticksLeft);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
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
                                ActiveProduction pItem = pBuilding.productionQueue.get(0);
                                if (!pItem.completed) {
                                    pItem.completed = true;
                                    pItem.item.onComplete.accept(pBuilding.level, pBuilding);
                                }
                                pBuilding.productionQueue.remove(pItem);
                            }
                        }
                    }
                    default -> { }
                }
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}