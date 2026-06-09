package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.buildings.shared.Market;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.player.RTSPlayerSaveData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class MarketTradeServerboundPacket {

    private final ResourceName from;
    private final ResourceName to;

    public MarketTradeServerboundPacket(ResourceName from, ResourceName to) {
        this.from = from;
        this.to = to;
    }

    public MarketTradeServerboundPacket(FriendlyByteBuf buffer) {
        this.from = buffer.readEnum(ResourceName.class);
        this.to = buffer.readEnum(ResourceName.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.from);
        buffer.writeEnum(this.to);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("MarketTradeServerboundPacket: Sender was null");
                return;
            }
            if (RTSPlayer.tradeIndex(from, to) < 0) {
                ReignOfNether.LOGGER.warn("MarketTradeServerboundPacket: invalid pair {} -> {}", from, to);
                return;
            }
            String ownerName = player.getName().getString();
            RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(ownerName);
            if (rtsPlayer == null) {
                return;
            }

            boolean ownsFinishedMarket = false;
            for (BuildingPlacement b : BuildingServerEvents.getBuildings()) {
                if (b.ownerName.equals(ownerName) && b.isBuilt && b.getBuilding() instanceof Market) {
                    ownsFinishedMarket = true;
                    break;
                }
            }
            if (!ownsFinishedMarket) {
                return;
            }

            if (!ResourcesServerEvents.canAfford(ownerName, from, Market.TRADE_CHUNK)) {
                return;
            }

            int payout = rtsPlayer.getTradeRate(from, to);
            int dFood = 0, dWood = 0, dOre = 0;
            switch (from) {
                case FOOD -> dFood -= Market.TRADE_CHUNK;
                case WOOD -> dWood -= Market.TRADE_CHUNK;
                case ORE -> dOre -= Market.TRADE_CHUNK;
                default -> { return; }
            }
            switch (to) {
                case FOOD -> dFood += payout;
                case WOOD -> dWood += payout;
                case ORE -> dOre += payout;
                default -> { return; }
            }
            ResourcesServerEvents.addSubtractResources(new Resources(ownerName, dFood, dWood, dOre));

            rtsPlayer.applyTrade(from, to);

            ServerLevel overworld = player.getServer() != null ? player.getServer().getLevel(Level.OVERWORLD) : null;
            if (overworld != null) {
                RTSPlayerSaveData.getInstance(overworld).save();
            }

            MarketTradeClientboundPacket.sendRates(player, rtsPlayer.tradeRates);
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
