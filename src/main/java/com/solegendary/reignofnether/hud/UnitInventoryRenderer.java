package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.gui.GuiGraphics;

public class UnitInventoryRenderer {

    public static boolean shouldRender(Unit unit) {
        return unit instanceof HeroUnit;
    }

    private static final Button EMPTY_SLOT_BUTTON = new ButtonBuilder("Empty inventory slot")
            .isEnabled(() -> false)
            .build();

    private static final int BUTTON_WIDTH = 22;
    public static final int INV_WIDTH = BUTTON_WIDTH * 2;
    public static final int INV_HEIGHT = BUTTON_WIDTH * 3;

    public static RectZone render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, Unit unit) {

        UnitItems.MERCHANT_CHESTPLATE.button.render(guiGraphics, x, y, mouseX, mouseY);
        UnitItems.MERCHANT_TRIDENT.button.render(guiGraphics, x + BUTTON_WIDTH, y, mouseX, mouseY);
        UnitItems.MERCHANT_GOLDEN_APPLE.button.render(guiGraphics, x, y+ BUTTON_WIDTH, mouseX, mouseY);
        UnitItems.MERCHANT_SWORD.button.render(guiGraphics, x + BUTTON_WIDTH, y+ BUTTON_WIDTH, mouseX, mouseY);
        UnitItems.MERCHANT_CHESTPLATE.button.render(guiGraphics, x, y+INV_WIDTH, mouseX, mouseY);
        EMPTY_SLOT_BUTTON.render(guiGraphics, x + BUTTON_WIDTH, y + INV_WIDTH, mouseX, mouseY);

        return RectZone.getZoneByLW(x, y, INV_WIDTH, INV_HEIGHT);
    }
}
