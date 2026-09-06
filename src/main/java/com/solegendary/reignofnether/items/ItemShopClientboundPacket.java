package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ItemShopClientboundPacket {

    private final BlockPos buildingPos;
    private final ArrayList<UUID> uuids;
    private final ArrayList<Integer> buyCosts;
    private final ArrayList<Integer> maxStocks;
    private final ArrayList<Integer> stocks;
    private final ArrayList<Integer> maxRestockTicks;
    private final ArrayList<Integer> restockTicks;

    public static void syncItemShopStock(BlockPos buildingPos, ArrayList<StockedShopItem> itemsAndStock) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new ItemShopClientboundPacket(buildingPos, itemsAndStock)
        );
    }

    public ItemShopClientboundPacket(BlockPos buildingPos, ArrayList<StockedShopItem> itemsAndStock) {
        this.buildingPos = buildingPos;
        this.uuids = new ArrayList<>();
        this.buyCosts = new ArrayList<>();
        this.maxStocks = new ArrayList<>();
        this.stocks = new ArrayList<>();
        this.maxRestockTicks = new ArrayList<>();
        this.restockTicks = new ArrayList<>();

        for (StockedShopItem stock : itemsAndStock) {
            uuids.add(stock.item.uuid);
            buyCosts.add(stock.getBuyCost());
            maxStocks.add(stock.maxStock);
            stocks.add(stock.stock);
            maxRestockTicks.add(stock.maxRestockTicks);
            restockTicks.add(stock.getTicksToNextRestock());
        }
    }

    public ItemShopClientboundPacket(FriendlyByteBuf buffer) {
        this.buildingPos = buffer.readBlockPos();
        int size = buffer.readInt();
        this.uuids = new ArrayList<>();
        this.buyCosts = new ArrayList<>();
        this.maxStocks = new ArrayList<>();
        this.stocks = new ArrayList<>();
        this.maxRestockTicks = new ArrayList<>();
        this.restockTicks = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            uuids.add(buffer.readUUID());
            buyCosts.add(buffer.readInt());
            maxStocks.add(buffer.readInt());
            stocks.add(buffer.readInt());
            maxRestockTicks.add(buffer.readInt());
            restockTicks.add(buffer.readInt());
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(buildingPos);
        buffer.writeInt(uuids.size());
        for (int i = 0; i < uuids.size(); i++) {
            buffer.writeUUID(uuids.get(i));
            buffer.writeInt(buyCosts.get(i));
            buffer.writeInt(maxStocks.get(i));
            buffer.writeInt(stocks.get(i));
            buffer.writeInt(maxRestockTicks.get(i));
            buffer.writeInt(restockTicks.get(i));
        }
    }

    // client-side packet-consuming function
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        ArrayList<StockedShopItem> itemsAndStock = new ArrayList<>();
                        for (int i = 0; i < uuids.size(); i++) {
                            UnitItem unitItem = ItemUtil.getUnitItem(uuids.get(i));
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
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}