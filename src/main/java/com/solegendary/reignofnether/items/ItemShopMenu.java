package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.ItemShopAddon;
import com.solegendary.reignofnether.hud.RectZone;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.*;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

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

    /**
     * Top-level entry point: draws the panel background, title, close button, and the
     * item grid, sized to fit however many items are currently stocked.
     *
     * @param bpl  the shop's BuildingPlacement
     * @param shop the same building, already known to implement ItemShopAddon
     * @param x    top-left x of the menu panel
     * @param y    top-left y of the menu panel
     * @return every Button created, so the caller can route clicks/hotkeys/tooltips to them
     *         (see integration note above)
     */
    public static RectZone renderFrame(GuiGraphics guiGraphics, ItemShopAddon shop, int x, int y) {
        BuildingPlacement bpl = ItemClientEvents.openItemShop;
        HashMap<UnitItem, Integer> itemsAndStock = bpl.getDataStorage().getData(ItemShopAddon.ITEMS_AND_STOCK);
        if (itemsAndStock == null)
            itemsAndStock = new HashMap<>();

        int contentHeight = getMenuHeight(itemsAndStock.size());
        int panelWidth = MENU_WIDTH + PANEL_PADDING;
        int panelHeight = contentHeight + PANEL_PADDING;

        MyRenderer.renderFrameWithBg(guiGraphics, x, y, panelWidth, panelHeight, PANEL_BG_COLOUR);
        return RectZone.getZoneByLW(x, y, panelWidth, panelHeight);
    }

    public static List<Button> renderButtons(GuiGraphics guiGraphics, ItemShopAddon shop, int x, int y, int mouseX, int mouseY) {
        BuildingPlacement bpl = ItemClientEvents.openItemShop;
        HashMap<UnitItem, Integer> itemsAndStock = bpl.getDataStorage().getData(ItemShopAddon.ITEMS_AND_STOCK);
        if (itemsAndStock == null)
            itemsAndStock = new HashMap<>();

        ArrayList<Button> allButtons = new ArrayList<>();
        int contentX = x + PANEL_INSET;
        int contentY = y + PANEL_INSET;
        allButtons.add(renderTitleAndCloseButton(guiGraphics, contentX, contentY, mouseX, mouseY));
        allButtons.addAll(renderShopItemButtons(guiGraphics, bpl, shop, itemsAndStock, contentX, contentY + HEADER_HEIGHT, mouseX, mouseY));
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
            HashMap<UnitItem, Integer> itemsAndStock,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        ArrayList<Button> buttons = new ArrayList<>();

        // MOCK: HashMap iteration order is undefined; sort by display name for a stable,
        // predictable grid. A real implementation likely wants an explicit shop-slot-order
        // field on UnitItem/ItemShopAddon instead of sorting alphabetically here.
        List<Map.Entry<UnitItem, Integer>> sortedEntries = new ArrayList<>(itemsAndStock.entrySet());
        sortedEntries.sort(Comparator.comparing(a -> a.getKey().getName().getString()));

        int i = 0;
        for (Map.Entry<UnitItem, Integer> entry : sortedEntries) {
            UnitItem item = entry.getKey();
            int stock = entry.getValue();

            int col = i % ITEMS_PER_ROW;
            int row = i / ITEMS_PER_ROW;
            int buttonX = x + col * ITEM_SLOT_SIZE;
            int buttonY = y + row * ITEM_SLOT_SIZE;

            Button itemButton = buildShopItemButton(bpl, item, stock);
            renderButton(guiGraphics, itemButton, buttonX, buttonY, mouseX, mouseY);
            buttons.add(itemButton);

            i += 1;
        }

        return buttons;
    }

    private static Button buildShopItemButton(BuildingPlacement bpl, UnitItem item, int stock) {
        boolean outOfStock = stock <= 0;

        List<FormattedCharSequence> tooltips = new ArrayList<>();
        tooltips.add(fcs(item.getName().getString(), true));
        // MOCK: assumes lang keys for cost/stock lines exist; none were specified.
        tooltips.add(fcs(I18n.get("itemshop.reignofnether.tooltip.cost", item.buyCost)));
        tooltips.add(fcs(I18n.get("itemshop.reignofnether.tooltip.stock", stock)));

        Button itemButton = new ButtonBuilder(item.getName().getString() + " Shop Item")
                .iconResource(item.iconRl)
                .isEnabled(() -> !outOfStock)
                .onLeftClick(() -> buyItem(bpl, item))
                .tooltipLines(tooltips)
                .build();

        // greys the whole button out once stock hits 0, same convention Button already
        // uses elsewhere (greyWhenDisabled defaults to true, kept explicit here for clarity)
        itemButton.greyWhenDisabled = true;
        return itemButton;
    }

    // MOCK: ItemShopAddon.buyItem(bpl, item, unit) is server-authoritative (it early-returns
    // on the client side per the code you provided), so purchasing needs to go through a
    // serverbound packet - none exists yet for this addon. ItemShopServerboundPacket below
    // is a placeholder call showing the shape that packet would need (compare to how
    // CustomBuildingServerboundPacket.customiseBuilding(...) or ItemServerboundPacket.sell(...)
    // are used elsewhere in the provided code).
    private static void buyItem(BuildingPlacement bpl, UnitItem item) {
        if (MC.player == null)
            return;

        Unit buyer = getActiveShopUnit();
        if (buyer == null)
            return;

        // ItemShopServerboundPacket.buyItem(MC.player.getName().getString(), bpl.originPos, item.uuid);
        throw new UnsupportedOperationException(
                "MOCK: wire this up to a real ItemShopServerboundPacket.buyItem(...) call once that packet exists"
        );
    }

    // MOCK: no API was provided for "the unit currently making purchases at this shop".
    // Standing in with the same preselected-unit lookup ItemClientEvents uses for its
    // give-item flow; a real implementation may instead want the player's single selected
    // unit, or a unit already garrisoned/standing at the shop.
    private static Unit getActiveShopUnit() {
        if (!UnitClientEvents.getPreselectedUnits().isEmpty())
            return (Unit) UnitClientEvents.getPreselectedUnits().get(0);
        return null;
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