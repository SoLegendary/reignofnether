package com.solegendary.reignofnether.items;

public class StockedShopItem {

    public final UnitItem item;
    public int buyCost; // may be adjusted for various reasons
    public final int maxStock; // <0 == infinite stock
    public int stock;
    public final int maxRestockTicks;
    private int restockTicks;

    public StockedShopItem(UnitItem unitItem, int maxStock, int maxRestockTicks) {
        this.item = unitItem;
        this.buyCost = unitItem.buyCost;
        this.maxStock = maxStock;
        this.stock = maxStock;
        this.maxRestockTicks = maxRestockTicks;
        this.restockTicks = maxRestockTicks;
    }

    public StockedShopItem(UnitItem unitItem, int buyCost, int maxStock, int stock, int maxRestockTicks, int restockTicks) {
        this.item = unitItem;
        this.buyCost = buyCost;
        this.maxStock = maxStock;
        this.stock = stock;
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
}
