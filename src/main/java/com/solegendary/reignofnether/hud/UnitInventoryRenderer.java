package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.buttons.UnitItemButton;
import com.solegendary.reignofnether.items.HeroExperienceBottleItem;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.items.unititems.HeroExperienceBottle;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;

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

    private static final ArrayList<Button> renderedButtons = new ArrayList<>();

    public static RectZone render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, Unit unit) {
        renderedButtons.clear();
        renderedButtons.add(new HeroExperienceBottle().getButton());
        renderedButtons.add(UnitItems.MERCHANT_TRIDENT.getButton());
        renderedButtons.add(UnitItems.MERCHANT_GOLDEN_APPLE.getButton());
        renderedButtons.add(UnitItems.MERCHANT_SWORD.getButton());
        renderedButtons.add(UnitItems.MERCHANT_CHESTPLATE.getButton());
        renderedButtons.add(EMPTY_SLOT_BUTTON);

        int i = 0;
        for (Button button : renderedButtons) {
            int xi = i % 2 == 0 ? x : x + BUTTON_WIDTH;
            int yi = y + ((i / 2) * BUTTON_WIDTH);
            button.render(guiGraphics, xi, yi, mouseX, mouseY);
            i += 1;
        }

        for (Button button : renderedButtons)
            if (button.isMouseOver(mouseX, mouseY))
                button.renderTooltip(guiGraphics, mouseX, mouseY);

        return RectZone.getZoneByLW(x, y, INV_WIDTH, INV_HEIGHT);
    }
}
