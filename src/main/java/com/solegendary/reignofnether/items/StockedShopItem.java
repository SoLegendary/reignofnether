package com.solegendary.reignofnether.items;

public class StockedShopItem {

    public final UnitItem item;
    public int buyCost; // may be adjusted for various reasons
    public final int maxStock; // <0 == infinite stock
    public int stock;
    public final int maxRestockTicks;
    private int restockTicks;
    public final int startingStock;

    public StockedShopItem(UnitItem unitItem, int maxStock, int maxRestockTicks) {
        this.item = unitItem;
        this.buyCost = unitItem.buyCost;
        this.maxStock = maxStock;
        this.stock = maxStock;
        this.maxRestockTicks = maxRestockTicks;
        this.restockTicks = maxRestockTicks;
        this.startingStock = maxStock;
    }

    public StockedShopItem(UnitItem unitItem, int buyCost, int maxStock, int startingStock, int maxRestockTicks) {
        this.item = unitItem;
        this.buyCost = buyCost;
        this.maxStock = maxStock;
        this.stock = startingStock;
        this.startingStock = startingStock;
        this.maxRestockTicks = maxRestockTicks;
        this.restockTicks = maxRestockTicks;
    }

    public StockedShopItem(UnitItem unitItem, int buyCost, int maxStock, int stock, int startingStock, int maxRestockTicks, int restockTicks) {
        this.item = unitItem;
        this.buyCost = buyCost;
        this.maxStock = maxStock;
        this.stock = stock;
        this.startingStock = startingStock;
        this.maxRestockTicks = maxRestockTicks;
        this.restockTicks = restockTicks;
    }

    public int getBuyCost() {
        return buyCost;
    }

    public int getTicksToNextRestock() {
        return restockTicks;
    }

    public void tick() {
        if (maxStock > 0 && stock < maxStock) {
            restockTicks -= 1;
            if (restockTicks <= 0) {
                restockTicks = maxRestockTicks;
                stock += 1;
            }
        }
    }

    public void reset() {
        this.stock = startingStock;
        this.restockTicks = maxRestockTicks;
    }
}
