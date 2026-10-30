package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.building.addon.ItemShopAddon;
import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.items.ItemClientEvents;
import com.solegendary.reignofnether.items.ItemServerboundPacket;
import com.solegendary.reignofnether.items.StockedShopItem;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

// A buy button shown in an open item shop, purchasing the item for the shop's served unit.
public class UnitItemShopButton extends AbstractUnitItemButton {

    private StockedShopItem stockedShopItem;

    public UnitItemShopButton(StockedShopItem stockedShopItem) {
        super(
                "button_" + stockedShopItem.item.getItem().getDescriptionId(),
                Button.DEFAULT_ICON_SIZE,
                null,
                null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    if (ItemClientEvents.openItemShop != null && !ItemClientEvents.openItemShop.isDestroyedServerside) {
                        ItemShopPlacement bpl = ItemClientEvents.openItemShop;
                        ItemShopAddon itemShop = bpl.getBuilding().getActiveAddon(ItemShopAddon.class);
                        if (itemShop != null && bpl.getServedUnit() instanceof LivingEntity le) {
                            ItemServerboundPacket.buy(le.getId(), stockedShopItem.item.descId, bpl.originPos);
                        } else if (itemShop != null) {
                            HudClientEvents.showTempMessageI18n("itemshop.reignofnether.error.no_buyers");
                        }
                    }
                },
                null,
                List.of(),
                stockedShopItem.item,
                stockedShopItem.item.getNewItemStack()
        );
        this.iconItem = this.itemStack;
        this.stockedShopItem = stockedShopItem;
        this.emeraldValue = stockedShopItem.buyCost;
    }

    @Override
    public void refresh() {
        if (stockedShopItem.stock == 0)
            this.greyPercent = 1 - ((float) stockedShopItem.getTicksToNextRestock() / stockedShopItem.maxRestockTicks);
        else
            this.greyPercent = 0;
        this.bottomLeftText = () -> stockedShopItem.maxStock >= 0 ? String.valueOf(stockedShopItem.stock) : "";
    }
}