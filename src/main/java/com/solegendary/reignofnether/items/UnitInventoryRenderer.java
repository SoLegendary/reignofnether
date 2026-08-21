package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.hud.RectZone;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class UnitInventoryRenderer {

    public static boolean shouldRender(Unit unit) {
        return unit instanceof UnitInventory inv &&
                (unit instanceof HeroUnit ||
                !inv.getAllItems().isEmpty());
    }

    private static final int BUTTON_WIDTH = 22;
    public static final int INV_WIDTH = BUTTON_WIDTH * 2;
    public static final int INV_HEIGHT = BUTTON_WIDTH * 3;

    public static RectZone render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, UnitInventory inv) {
        ItemClientEvents.renderedButtons.clear();
        for (int i = 0; i < inv.getAllItems().size(); i++) {
            ItemStack itemStack = inv.getAllItems().get(i);
            UnitItem unitItem = ItemUtil.getUnitItem(itemStack.getItem());
            UnitItem emptyItem = ItemUtil.getUnitItem(Items.AIR);
            if (unitItem != null) {
                ItemClientEvents.renderedButtons.add(unitItem.getButton(i, (Unit) inv)); // TODO: this is the same button instance being returned, do we need to make UnitItems separate instances too?
            } else if (emptyItem != null) {
                ItemClientEvents.renderedButtons.add(emptyItem.getButton(i, (Unit) inv));
            }
        }
        int i = 0;
        for (Button button : ItemClientEvents.renderedButtons) {
            int xi = i % 2 == 0 ? x : x + BUTTON_WIDTH;
            int yi = y + ((i / 2) * BUTTON_WIDTH);
            button.render(guiGraphics, xi, yi, mouseX, mouseY);
            i += 1;
        }

        for (Button button : ItemClientEvents.renderedButtons)
            if (button.isMouseOver(mouseX, mouseY))
                button.renderTooltip(guiGraphics, mouseX, mouseY);

        return RectZone.getZoneByLW(x, y, INV_WIDTH, INV_HEIGHT);
    }
}
