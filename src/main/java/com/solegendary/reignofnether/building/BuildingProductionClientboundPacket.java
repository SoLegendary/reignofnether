package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
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
    public ResourceLocation itemKey;
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

    public static void startProduction(BlockPos buildingPos, ProductionItem item) {
        sendFiltered(buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.START_PRODUCTION,
                        ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item),
                        buildingPos
                )
        );
    }

    public static void startProduction(BlockPos buildingPos, ProductionItem item, float ticksLeft) {
        sendFiltered(buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.START_PRODUCTION,
                        ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item),
                        buildingPos,
                        ticksLeft
                )
        );
    }

    public static void cancelProduction(BlockPos buildingPos, ProductionItem item, boolean frontItem) {
        sendFiltered(buildingPos,
                new BuildingProductionClientboundPacket(frontItem
                        ? BuildingAction.CANCEL_PRODUCTION
                        : BuildingAction.CANCEL_BACK_PRODUCTION,
                        ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item),
                        buildingPos
                )
        );
    }

    public static void clearQueue(BlockPos buildingPos) {
        sendFiltered(buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.CLEAR_PRODUCTION, EMPTY, buildingPos)
        );
    }

    public static void completeProduction(BlockPos buildingPos) {
        sendFiltered(buildingPos,
                new BuildingProductionClientboundPacket(BuildingAction.COMPLETE_PRODUCTION, EMPTY, buildingPos)
        );
    }

    public BuildingProductionClientboundPacket(
            BuildingAction action,
            ResourceLocation itemKey,
            BlockPos buildingPos
    ) {
        this.action = action;
        this.itemKey = itemKey;
        this.buildingPos = buildingPos;
        this.ticksLeft = -1;
    }

    public BuildingProductionClientboundPacket(
            BuildingAction action,
            ResourceLocation itemKey,
            BlockPos buildingPos,
            float ticksLeft
    ) {
        this.action = action;
        this.itemKey = itemKey;
        this.buildingPos = buildingPos;
        this.ticksLeft = ticksLeft;
    }

    public BuildingProductionClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemKey = buffer.readResourceLocation();
        this.buildingPos = buffer.readBlockPos();
        this.ticksLeft = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeResourceLocation(this.itemKey);
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
                switch (action) {
                    case START_PRODUCTION -> {
                        ((ProductionPlacement) building).startProductionItem(
                                ReignOfNetherRegistries.PRODUCTION_ITEM.get(itemKey),
                                ticksLeft
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
                    default -> { }
                }
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}