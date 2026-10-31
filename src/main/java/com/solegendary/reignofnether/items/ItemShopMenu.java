package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.addon.ItemShopAddon;
import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.buttons.UnitItemShopButton;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.capitaliseAndSpace;

/** Item shop pop-over shown above the HUD console while a shop (or the hero it serves) is selected. */
public class ItemShopMenu {

    private static final int SLOT = HudLayout.CELL;
    private static final int GAP = 3;
    private static final int PER_ROW = 6;
    private static final int PAD = 6;
    private static final int HEADER = 16;

    /** Draws the menu with its bottom-left corner at (x, bottomY). */
    public static void render(HudFrame f, ItemShopPlacement shop, int x, int bottomY) {
        ArrayList<StockedShopItem> stocks = shop.getDataStorage().getData(ItemShopAddon.STOCKED_ITEMS);
        if (stocks == null)
            stocks = new ArrayList<>();

        int rows = Math.max(1, (stocks.size() + PER_ROW - 1) / PER_ROW);
        int w = PAD * 2 + PER_ROW * SLOT + (PER_ROW - 1) * GAP;
        int h = PAD * 2 + HEADER + rows * SLOT + (rows - 1) * GAP;
        int y = bottomY - h;
        Theme.popup(f.g, x, y, w, h, Theme.EMERALD);
        f.zone(x, y, w, h);
        Text.heading(f.g, I18n.get("itemshop.reignofnether.title"), x + PAD, y + PAD + 1, Theme.GOLD, Text.px(18));

        int right = x + w - PAD;
        Button close = new ButtonBuilder("Close Item Shop Menu")
                .onLeftClick(() -> ItemClientEvents.openItemShop = null)
                .build();
        Rect closeRect = new Rect(right - 11, y + PAD, 11, 11);
        Theme.cross(f.g, closeRect, f.hovered(closeRect) ? Theme.DANGER_TEXT : Theme.MUTED);
        f.register(close, closeRect, null, () -> Tooltip.of(I18n.get("itemshop.reignofnether.close")));

        Unit served = shop.getServedUnit();
        if (served instanceof LivingEntity servedEntity) {
            String name = capitaliseAndSpace(HudClientEvents.getModifiedEntityName(servedEntity));
            String icon = MiscUtil.getEntityIconName(servedEntity) + (servedEntity.isVehicle() ? "_half.png" : ".png");
            Rect servingRect = new Rect(closeRect.x() - 15, y + PAD - 1, 13, 13);
            MyRenderer.renderIcon(f.g, ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/" + icon),
                    servingRect.x(), servingRect.y(), 13);
            Button serving = new ButtonBuilder("Serving Unit").onLeftClick(shop::cycleServedUnit).build();
            f.register(serving, servingRect, null, () -> Tooltip.of(I18n.get("itemshop.reignofnether.serving", name))
                    .status(I18n.get("itemshop.reignofnether.serving_hint"), Theme.LABEL));
        }

        List<Button> buttons = new ArrayList<>();
        for (StockedShopItem stocked : stocks)
            buttons.add(new UnitItemShopButton(stocked));
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            if (b.isHidden.get())
                continue;
            int bx = x + PAD + (i % PER_ROW) * (SLOT + GAP);
            int by = y + PAD + HEADER + (i / PER_ROW) * (SLOT + GAP);
            f.cell(b, bx, by, SLOT, null);
        }
    }
}
