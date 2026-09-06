package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.ItemShopAddon;
import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.hud.RectZone;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.UnitItemShopButton;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Renders the HUD menu for a building implementing {@link ItemShopAddon}: a title, a close
 * button, and a tightly-packed grid of purchasable-item buttons whose panel grows/shrinks
 * to fit however many items the shop is currently stocked with.
 *
 * Modeled directly on CustomBuildingMenu's layout/structure (renderXButton(evt, x, y) methods
 * that return the Button(s) they created, plus a private renderButton() helper that also
 * handles tooltip rendering).
 *
 * ------------------------------------------------------------------------------------------
 * INTENDED INTEGRATION (not wired up here, since ItemClientEvents' screen-render/mouse-press
 * plumbing for shop buildings wasn't provided) -- this mirrors how CustomBuildingMenu is
 * presumably driven from its own client-events class:
 *
 *   // in some ItemShopClientEvents (MOCK - does not exist yet):
 *   //   - track which BuildingPlacement's shop menu is currently open (like
 *   //     CustomBuildingClientEvents.getCustomBuildingToEdit())
 *   //   - on ScreenEvent.Render.Post: call ItemShopMenu.render(evt, bpl, x, y) and collect
 *   //     the returned buttons into a list
 *   //   - on ScreenEvent.MouseButtonPressed.Post / KeyPressed: call
 *   //     button.checkClicked(...) / checkPressed(...) on that collected list, exactly as
 *   //     ItemClientEvents.onMousePress()/onKeyRelease() do for `renderedButtons`
 * ------------------------------------------------------------------------------------------
 */
public class ItemShopMenu {

    private static final Minecraft MC = Minecraft.getInstance();

    // ---- layout constants (MOCK: exact panel dimensions/positioning were not specified) ----
    private static final int TITLE_X_OFFSET = 6;
    private static final int TITLE_Y_OFFSET = 6;
    private static final int HEADER_HEIGHT = 22;               // space reserved for the title row before items start
    private static final int ITEM_SLOT_SIZE = Button.DEFAULT_ICON_FRAME_SIZE; // 22px - buttons placed edge-to-edge, i.e. "tightly-packed"
    private static final int MENU_WIDTH = ITEM_SLOT_SIZE * 6;  // MOCK: fits 6 items per row; not specified, pick a sensible grid width
    private static final int ITEMS_PER_ROW = MENU_WIDTH / ITEM_SLOT_SIZE;

    // panel background matches GlobalProductionQueueRenderer.renderQueue(): a MyRenderer
    // frame-with-bg (proper corner/edge frame texture, not a flat fill), same bg colour,
    // and the same 5px-per-side inset ("+10" total) between the frame and its contents.
    private static final int PANEL_BG_COLOUR = 0xA0000000;
    private static final int PANEL_PADDING = 10;
    private static final int PANEL_INSET = PANEL_PADDING / 2;

    public static RectZone renderFrame(GuiGraphics guiGraphics, ItemShopAddon shop, int x, int y) {
        ItemShopPlacement bpl = ItemClientEvents.openItemShop;
        ArrayList<StockedShopItem> stocks = bpl.getDataStorage().getData(ItemShopAddon.STOCKED_ITEMS);
        if (stocks == null)
            stocks = new ArrayList<>();

        int contentHeight = getMenuHeight(stocks.size());
        int panelWidth = MENU_WIDTH + PANEL_PADDING;
        int panelHeight = contentHeight + PANEL_PADDING;

        MyRenderer.renderFrameWithBg(guiGraphics, x, y, panelWidth, panelHeight, PANEL_BG_COLOUR);
        return RectZone.getZoneByLW(x, y, panelWidth, panelHeight);
    }

    public static List<Button> renderButtons(GuiGraphics guiGraphics, ItemShopAddon shop, int x, int y, int mouseX, int mouseY) {
        ItemShopPlacement bpl = ItemClientEvents.openItemShop;
        ArrayList<StockedShopItem> stocks = bpl.getDataStorage().getData(ItemShopAddon.STOCKED_ITEMS);
        if (stocks == null)
            stocks = new ArrayList<>();

        ArrayList<Button> allButtons = new ArrayList<>();
        int contentX = x + PANEL_INSET;
        int contentY = y + PANEL_INSET;
        allButtons.add(renderTitleAndCloseButton(guiGraphics, contentX, contentY, mouseX, mouseY));
        allButtons.addAll(renderShopItemButtons(guiGraphics, bpl, shop, stocks, contentX, contentY + HEADER_HEIGHT, mouseX, mouseY));
        return allButtons;
    }

    /** Draws the "Item Shop" title top-left and a close button top-right; returns the close button. */
    private static Button renderTitleAndCloseButton(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        guiGraphics.drawString(
                MC.font,
                "Item Shop", // title text as specified; not pulled through I18n since no lang key was given for it
                x + TITLE_X_OFFSET,
                y + TITLE_Y_OFFSET,
                0xFFFFFF
        );

        Button closeButton = new Button(
                "Close Item Shop Menu",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross_square.png"),
                null,
                () -> false,
                () -> false,
                () -> true,
                () -> ItemClientEvents.openItemShop = null,
                null,
                List.of()
        );
        closeButton.frameResource = null;
        renderButton(guiGraphics, closeButton, x + MENU_WIDTH - Button.itemIconSize - TITLE_X_OFFSET, y, mouseX, mouseY);
        return closeButton;
    }

    /** Tightly-packed grid of one button per stocked item, wrapping to a new row every ITEMS_PER_ROW items. */
    private static List<Button> renderShopItemButtons(
            GuiGraphics guiGraphics,
            BuildingPlacement bpl,
            ItemShopAddon shop,
            ArrayList<StockedShopItem> stocks,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        ArrayList<Button> buttons = new ArrayList<>();

        int i = 0;
        for (StockedShopItem stockedItem : stocks) {

            int col = i % ITEMS_PER_ROW;
            int row = i / ITEMS_PER_ROW;
            int buttonX = x + col * ITEM_SLOT_SIZE;
            int buttonY = y + row * ITEM_SLOT_SIZE;

            Button itemButton = new UnitItemShopButton(stockedItem);
            renderButton(guiGraphics, itemButton, buttonX, buttonY, mouseX, mouseY);
            buttons.add(itemButton);

            i += 1;
        }
        return buttons;
    }

    /** Total panel height needed to fit itemCount items in the grid, plus the header row. */
    public static int getMenuHeight(int itemCount) {
        int rows = Math.max(1, (int) Math.ceil(itemCount / (double) ITEMS_PER_ROW));
        return HEADER_HEIGHT + rows * ITEM_SLOT_SIZE;
    }

    public static int getMenuWidth() {
        return MENU_WIDTH;
    }

    // same helper pattern as CustomBuildingMenu.renderButton(): renders a button unless
    // hidden, and renders its tooltip when moused over
    private static void renderButton(GuiGraphics guiGraphics, Button button, int x, int y, int mouseX, int mouseY) {
        if (!button.isHidden.get()) {
            button.render(guiGraphics, x, y, mouseX, mouseY);
            if (button.isMouseOver(mouseX, mouseY) && button.tooltipLines != null)
                button.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }
}