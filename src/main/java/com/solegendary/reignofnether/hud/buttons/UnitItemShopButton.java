package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.building.addon.ItemShopAddon;
import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.items.ItemClientEvents;
import com.solegendary.reignofnether.items.ItemServerboundPacket;
import com.solegendary.reignofnether.items.StockedShopItem;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.GuiGraphics;
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
<<<<<<< HEAD
                            ItemServerboundPacket.buy(le.getId(), stockedShopItem.item.descId, bpl.originPos);
=======
                            ItemServerboundPacket.buy(le.getId(), stockedShopItem.item.uuid, bpl.originPos);
>>>>>>> 918672b73971eb87df5c33546072a6f1f635c7b6
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
        this.emeraldValue = stockedShopItem.item.buyCost;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (stockedShopItem.stock == 0) {
            this.greyPercent = 1 - ((float) stockedShopItem.getTicksToNextRestock() / stockedShopItem.maxRestockTicks);
        } else {
            this.greyPercent = 0;
        }
        super.render(guiGraphics, x, y, mouseX, mouseY);

        if (stockedShopItem.maxStock >= 0) {
            MyRenderer.drawScaledString(guiGraphics,
                    MC.font,
                    String.valueOf(stockedShopItem.stock),
                    x + 3,
                    y + 3,
                    0xFFFFFF,
                    0.75f);
        }
    }
}