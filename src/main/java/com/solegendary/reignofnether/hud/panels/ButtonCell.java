package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Theme.CellState;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Draws a legacy {@link Button} as a cell of the new HUD: state colours, cost chip, cooldown, hotkey chip
 * and rank pips. The Button stays the data model (action, hotkey, tooltip); only the drawing lives here.
 */
public final class ButtonCell {

    private ButtonCell() { }

    public static final ResourceLocation ICON_FOOD = rl("textures/icons/items/wheat.png");
    public static final ResourceLocation ICON_WOOD = rl("textures/icons/items/wood.png");
    public static final ResourceLocation ICON_ORE = rl("textures/icons/items/iron_ore.png");
    public static final ResourceLocation ICON_EMERALD = rl("textures/icons/items/emerald.png");
    public static final ResourceLocation ICON_MANA = rl("textures/icons/items/lapis.png");
    public static final ResourceLocation ICON_CLOCK = rl("textures/icons/items/clock.png");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, path);
    }

    /** What the player is missing to afford something: resource icon and amount, or null if affordable. */
    public record Shortfall(ResourceLocation icon, int amount) { }

    @Nullable
    public static Shortfall shortfall(@Nullable ResourceCost cost) {
        Resources own = ResourcesClientEvents.getOwnResources();
        if (cost == null || own == null)
            return null;
        if (own.food < cost.food) return new Shortfall(ICON_FOOD, cost.food - own.food);
        if (own.wood < cost.wood) return new Shortfall(ICON_WOOD, cost.wood - own.wood);
        if (own.ore < cost.ore) return new Shortfall(ICON_ORE, cost.ore - own.ore);
        if (own.emerald < cost.emerald) return new Shortfall(ICON_EMERALD, cost.emerald - own.emerald);
        return null;
    }

    public static CellState stateOf(Button b) {
        if (!b.isEnabled.get())
            return CellState.LOCKED;
        if (b instanceof AbilityButton ab && ab.getCooldownTicks() > 0)
            return CellState.COOLDOWN;
        if (b.cost != null && shortfall(b.cost.get()) != null)
            return CellState.POOR;
        if (b instanceof AbilityButton ab && ab.ability instanceof HeroAbility ha && ha.manaCost > 0
                && ab.getUnit() instanceof HeroUnit hero && hero.getMana() < ha.manaCost)
            return CellState.POOR;
        if (b.isSelected.get())
            return CellState.ACTIVE;
        return CellState.READY;
    }

    public static void render(GuiGraphics g, Button b, int x, int y, int size, @Nullable String key, boolean hovered) {
        b.refresh();
        CellState state = stateOf(b);
        Theme.cell(g, x, y, size, size, state, hovered);

        int icon = Math.max(8, Math.round(size * 0.56f));
        int ix = x + (size - icon) / 2;
        int iy = y + (size - icon) / 2;
        drawIcon(g, b, ix, iy, icon);

        if (state == CellState.LOCKED) {
            g.fill(x, y, x + size, y + size, 0x99000000);
            Theme.lock(g, x + (size - 9) / 2, y + (size - 9) / 2, 9, Theme.DESC);
        } else if (state == CellState.POOR) {
            g.fill(x, y, x + size, y + size, 0x55251619);
        }

        if (state == CellState.COOLDOWN && b instanceof AbilityButton ab) {
            float max = ab.ability.cooldownMax;
            float left = ab.getCooldownTicks();
            int shade = Math.round(size * Math.min(1f, left / Math.max(1f, max)));
            g.fill(x, y, x + size, y + shade, 0xD1080710);
            g.fill(x, y + shade, x + size, y + size, 0x1F080710);
            String secs = String.valueOf((int) Math.ceil(left / 20f));
            Text.bigCentered(g, secs, x + size / 2, y + (size - 9) / 2, Theme.GOLD, Text.px(20));
        } else if (b.greyPercent > 0 && !(b instanceof AbilityButton)) {
            // production progress and similar: the cleared part grows from the top
            int clear = Math.round(b.greyPercent * size);
            g.fill(x, y + clear, x + size, y + size, 0x99000000);
        }

        if (b.isFlashing.get())
            g.fill(x, y, x + size, y + size, (int) (0x60 * MiscUtil.getOscillatingFloat(0, 1)) << 24 | 0xFFFFFF);

        if (key != null && size >= 18)
            Theme.keyCap(g, key, x + 1, y + 1, state == CellState.ACTIVE ? Theme.GOLD
                    : state == CellState.EMPTY ? Theme.FAINT : Theme.MUTED);

        drawCostChip(g, b, x, y, size, state);
        drawCharges(g, b, x, y, size);
        drawRankPips(g, b, x, y, size);

        if (b.bottomLeftText != null) {
            String t = b.bottomLeftText.get();
            if (t != null && !t.isEmpty())
                Text.smallShadowed(g, t, x + 2, y + size - 9, b.bottomLeftTextColor | 0xFF000000);
        }
    }

    public static void renderEmpty(GuiGraphics g, int x, int y, int size, @Nullable String key) {
        Theme.cell(g, x, y, size, size, CellState.EMPTY, false);
        if (key != null && size >= 18)
            Theme.keyCap(g, key, x + 1, y + 1, Theme.FAINT);
    }

    public static void drawIcon(GuiGraphics g, Button b, int x, int y, int size) {
        if (!b.playerNameForHeadIcon.isBlank()) {
            MyRenderer.renderPlayerHead(g, b.playerNameForHeadIcon, x, y, size, b.bgIconResource);
            return;
        }
        if (b.bgIconResource != null)
            MyRenderer.renderIcon(g, b.bgIconResource, x, y, size);
        if (b.iconResource != null && b.iconResource.getPath().startsWith("textures/gui/icons/")) {
            // pixelarticons: a pixel-exact size, tinted like the rest of the HUD's text
            int s = Math.max(6, Math.round(size / 6f) * 6);
            Icon.drawTexture(g, b.iconResource, x + (size - s) / 2, y + (size - s) / 2, s, Theme.BODY);
        } else if (b.iconResource != null)
            MyRenderer.renderIcon(g, b.iconResource, x, y, size);
        if (b.iconItem != null)
            MyRenderer.renderItem(g, b.iconItem, x - 1, y - 1, size / 16f);
    }

    private static void drawCostChip(GuiGraphics g, Button b, int x, int y, int size, CellState state) {
        if (state == CellState.COOLDOWN || size < 24)
            return;
        ResourceLocation icon = null;
        int amount = 0;
        int colour = Theme.BODY;

        if (b instanceof AbilityButton ab && ab.ability instanceof HeroAbility ha && ha.manaCost > 0) {
            icon = ICON_MANA;
            amount = ha.manaCost;
            colour = state == CellState.POOR ? Theme.ERROR : Theme.MANA_TEXT;
        } else if (b.cost != null) {
            ResourceCost cost = b.cost.get();
            Shortfall s = shortfall(cost);
            if (s != null) {
                icon = s.icon();
                amount = s.amount();
                colour = Theme.ERROR;
            } else if (cost != null) {
                int max = Math.max(Math.max(cost.food, cost.wood), Math.max(cost.ore, cost.emerald));
                if (max <= 0)
                    return;
                amount = max;
                icon = max == cost.food ? ICON_FOOD : max == cost.wood ? ICON_WOOD : max == cost.ore ? ICON_ORE : ICON_EMERALD;
            }
        }
        if (icon == null)
            return;
        String text = compact(amount);
        int w = Text.smallWidth(text) + 8;
        int cx = x + size - w - 1;
        int cy = y + size - 8;
        g.fill(cx, cy, x + size - 1, y + size - 1, 0xCC0B0A0F);
        MyRenderer.renderIcon(g, icon, cx + 1, cy + 1, 5);
        Text.small(g, text, cx + 7, cy - 1, colour);
    }

    private static void drawCharges(GuiGraphics g, Button b, int x, int y, int size) {
        if (!(b instanceof AbilityButton ab))
            return;
        int charges = ab.getCharges();
        if (charges < 0)
            return;
        int colour = charges >= ab.ability.maxCharges ? Theme.OK : charges <= 0 ? Theme.ERROR : charges == 1 ? Theme.YELLOW : Theme.TEXT;
        String s = String.valueOf(charges);
        Text.smallShadowed(g, s, x + size - Text.smallWidth(s) - 2, y, colour);
    }

    private static void drawRankPips(GuiGraphics g, Button b, int x, int y, int size) {
        if (!(b instanceof AbilityButton ab) || !(ab.ability instanceof HeroAbility ha) || !(ab.getUnit() instanceof HeroUnit hero))
            return;
        int rank = ha.getRank(hero);
        for (int i = 0; i < ha.maxRank; i++) {
            int px = x + 2 + i * 4;
            g.fill(px, y + size - 4, px + 3, y + size - 1, i < rank ? Theme.ORANGE : Theme.FAINT);
        }
    }

    public static String compact(int amount) {
        return amount >= 1000 ? (amount / 100) / 10f + "k" : String.valueOf(amount);
    }

    /** Tooltip for a command card cell: the button's own lines plus a state line and the hotkey. */
    public static Tooltip tooltipFor(Button b, @Nullable String key) {
        Tooltip t = Tooltip.fromLines(b.tooltipLines);
        if (t == null)
            t = Tooltip.of(b.name);
        switch (stateOf(b)) {
            case LOCKED -> t.status(I18n.get("hud.reignofnether.cell.locked"), Theme.ERROR);
            case COOLDOWN -> {
                if (b instanceof AbilityButton ab)
                    t.status(I18n.get("hud.reignofnether.cell.ready_in", (int) Math.ceil(ab.getCooldownTicks() / 20f)), Theme.MUTED);
            }
            case POOR -> {
                Shortfall s = b.cost != null ? shortfall(b.cost.get()) : null;
                if (s != null)
                    t.status(I18n.get("hud.reignofnether.cell.need_more", s.amount(), resourceName(s.icon())), Theme.ERROR);
                else
                    t.status(I18n.get("hud.buttons.reignofnether.not_enough_mana"), Theme.ERROR);
            }
            default -> { }
        }
        if (key != null && !key.isEmpty())
            t.key(key);
        return t;
    }

    private static String resourceName(ResourceLocation icon) {
        String key = icon == ICON_FOOD ? "food" : icon == ICON_WOOD ? "wood" : icon == ICON_ORE ? "ore" : "emerald";
        return I18n.get("resources.reignofnether." + key).toLowerCase();
    }
}
