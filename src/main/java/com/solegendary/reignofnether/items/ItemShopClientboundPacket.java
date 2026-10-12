package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.items.StockedShopItem;
import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.ItemShopClientboundPacket;
import com.solegendary.reignofnether.items.ItemClientEvents;

public class ItemShopClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<ItemShopClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "item_shop_clientbound"));

    @Override
    public CustomPacketPayload.Type<ItemShopClientboundPacket> type() {
        return TYPE;
    }

    private final BlockPos buildingPos;
    private final ArrayList<String> descIds;
    private final ArrayList<Integer> buyCosts;
    private final ArrayList<Integer> maxStocks;
    private final ArrayList<Integer> stocks;
    private final ArrayList<Integer> maxRestockTicks;
    private final ArrayList<Integer> restockTicks;

    public static void syncItemShopStock(BlockPos buildingPos, ArrayList<StockedShopItem> itemsAndStock) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new ItemShopClientboundPacket(buildingPos, itemsAndStock)
        );
    }

    public ItemShopClientboundPacket(BlockPos buildingPos, ArrayList<StockedShopItem> itemsAndStock) {
        this.buildingPos = buildingPos;
        this.descIds = new ArrayList<>();
        this.buyCosts = new ArrayList<>();
        this.maxStocks = new ArrayList<>();
        this.stocks = new ArrayList<>();
        this.maxRestockTicks = new ArrayList<>();
        this.restockTicks = new ArrayList<>();

        for (StockedShopItem stock : itemsAndStock) {
            descIds.add(stock.item.descId);
            buyCosts.add(stock.getBuyCost());
            maxStocks.add(stock.maxStock);
            stocks.add(stock.stock);
            maxRestockTicks.add(stock.maxRestockTicks);
            restockTicks.add(stock.getTicksToNextRestock());
        }
    }

    public ItemShopClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.buildingPos = buffer.readBlockPos();
        int size = buffer.readInt();
        this.descIds = new ArrayList<>();
        this.buyCosts = new ArrayList<>();
        this.maxStocks = new ArrayList<>();
        this.stocks = new ArrayList<>();
        this.maxRestockTicks = new ArrayList<>();
        this.restockTicks = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            descIds.add(buffer.readUtf());
            buyCosts.add(buffer.readInt());
            maxStocks.add(buffer.readInt());
            stocks.add(buffer.readInt());
            maxRestockTicks.add(buffer.readInt());
            restockTicks.add(buffer.readInt());
        }
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(buildingPos);
        buffer.writeInt(descIds.size());
        for (int i = 0; i < descIds.size(); i++) {
            buffer.writeUtf(descIds.get(i));
            buffer.writeInt(buyCosts.get(i));
            buffer.writeInt(maxStocks.get(i));
            buffer.writeInt(stocks.get(i));
            buffer.writeInt(maxRestockTicks.get(i));
            buffer.writeInt(restockTicks.get(i));
        }
    }

    // client-side packet-consuming function
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        ArrayList<StockedShopItem> itemsAndStock = new ArrayList<>();
                        for (int i = 0; i < descIds.size(); i++) {
                            UnitItem unitItem = ItemUtil.getUnitItem(descIds.get(i));
                            if (unitItem == null)
                                continue;
                            itemsAndStock.add(new StockedShopItem(
                                    unitItem,
                                    buyCosts.get(i),
                                    maxStocks.get(i),
                                    stocks.get(i),
                                    maxRestockTicks.get(i),
                                    restockTicks.get(i)
                            ));
                        }
                        ItemClientEvents.setStockedShopItems(buildingPos, itemsAndStock);
                    });
        });
        return;
    }
}