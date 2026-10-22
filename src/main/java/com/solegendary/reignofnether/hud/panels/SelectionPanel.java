package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.NightSourceAddon;
import com.solegendary.reignofnether.building.buildings.monsters.SculkCatalyst;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.buildings.placements.SculkCatalystPlacement;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.PortraitRendererBuilding;
import com.solegendary.reignofnether.hud.PortraitRendererUnit;
import com.solegendary.reignofnether.hud.PortraitRendererUnit.Stat;
import com.solegendary.reignofnether.hud.buttons.AbstractUnitItemButton;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.buttons.UnitItemInventoryButton;
import com.solegendary.reignofnether.hud.effecticons.MobEffectIcon;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.items.ItemClientEvents;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.items.unititems.EmptyUnitItem;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.ui.Clip;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.NonUnitClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.LanguageUtil;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The middle of the HUD console: what is selected. One unit (portrait, health, stat cards, inventory),
 * an army (type stacks with counts plus the units of the active type), one building (portrait, health,
 * production queue), several buildings, or nothing.
 */
public final class SelectionPanel {

    private SelectionPanel() { }

    // sizes are the design's at half scale
    private static final int PORTRAIT_W = 75;
    private static final int COL_GAP = 9;
    private static final int STACK_W = 58;
    private static final int STACK_H = 31;
    private static final int TILE = 21;
    private static final int GAP = 5;
    private static final int HP_H = 11;
    private static final int STAT_H = 24;
    private static final int SLOT = 26;
    private static final int EFFECT = 17;
    private static final float NAME = Text.px(26);

    // where the portrait was drawn, so holding the mouse on it can follow the unit
    private static Rect portraitRect = Rect.EMPTY;

    public static Rect portraitRect() {
        return portraitRect;
    }

    // room for the name, bars and stat cards of one unit or building
    private static final int INFO_W = 220;

    /** A floating card sized to what is selected and centred on the screen; nothing when nothing is selected. */
    public static void render(HudFrame f) {
        portraitRect = Rect.EMPTY;
        Rect room = f.layout.selection();
        BuildingPlacement building = HudClientEvents.hudSelectedPlacement;
        LivingEntity entity = HudClientEvents.hudSelectedEntity;
        if (room.w() <= 0 || (building == null && entity == null))
            return;

        List<BuildingPlacement> buildings = BuildingClientEvents.getSelectedBuildings();
        boolean buildingStacks = building != null && buildings.size() >= 2
                && BuildingClientEvents.getPlayerToBuildingRelationship(building) == Relationship.OWNED;
        List<LivingEntity> army = building == null ? controllableSelection() : List.of();

        int want;
        if (buildingStacks)
            want = stacksWidth(buildings.stream().map(SelectionPanel::buildingName).distinct().count(), 0);
        else if (building != null)
            want = PORTRAIT_W + COL_GAP + INFO_W + 60;
        else if (army.size() >= 2)
            want = Math.max(stacksWidth(army.stream().map(HudClientEvents::getModifiedEntityName).distinct().count(), army.size()),
                    Text.bigWidth(I18n.get("hud.reignofnether.units_count", army.size()), Text.px(24)) + 70
                            + Text.smallWidth(I18n.get("hud.reignofnether.army_hint")));
        else {
            int rightW = rightColumnWidth(entity);
            want = PORTRAIT_W + COL_GAP + INFO_W + (rightW > 0 ? rightW + COL_GAP : 0);
        }
        int w = Math.min(room.w(), want);
        int x = Math.max(room.x(), Math.min(room.right() - w, (f.layout.width() - w) / 2));
        Rect r = new Rect(x, room.y(), w, room.h());
        int pad = HudLayout.CONSOLE_PAD;
        Rect card = new Rect(r.x() - pad, r.y() - pad, r.w() + pad * 2, r.h() + pad * 2);
        Theme.panel(f.g, card.x(), card.y(), card.w(), card.h(), Theme.FLOATING);
        f.zone(card);

        if (buildingStacks)
            renderBuildingStacks(f, r, buildings);
        else if (building != null)
            renderBuilding(f, r, building);
        else if (army.size() >= 2)
            renderArmy(f, r, army);
        else
            renderUnit(f, r, entity);
    }

    /** Wide enough for every type stack on one row and, for armies, a row of unit tiles. */
    private static int stacksWidth(long types, int units) {
        int stacks = (int) types * (STACK_W + GAP) - GAP;
        int tiles = units * (TILE + 4) - 4;
        return Math.max(300, Math.max(stacks, tiles));
    }

    private static List<LivingEntity> controllableSelection() {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : UnitClientEvents.getSortedSelectedUnits())
            if (UnitClientEvents.getPlayerToEntityRelationship(e) == Relationship.OWNED
                    || NonUnitClientEvents.canControlAllMobs() || AlliancesClient.canControlAlly(e))
                list.add(e);
        return list;
    }

    // ------------------------------------------------------------------ names

    /** Capitalised display name of an entity, as used across the HUD. */
    public static String displayName(LivingEntity e) {
        if (e.hasCustomName() && e.getCustomName() != null)
            return e.getCustomName().getString();
        return MiscUtil.capitaliseAndSpace(HudClientEvents.getModifiedEntityName(e).replace("_", " "));
    }

    public static String buildingName(BuildingPlacement b) {
        if (b.getUpgradeLevel() > 0)
            return b.getUpgradedName();
        if (b.getBuilding() instanceof CustomBuilding cb)
            return cb.name;
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(b.getBuilding());
        return key != null ? LanguageUtil.getTranslation("buildings." + key.getNamespace() + "." + key.getPath()) : "";
    }

    private static ResourceLocation headIcon(LivingEntity e) {
        String name = MiscUtil.getEntityIconName(e);
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,
                "textures/mobheads/" + name + (e.isVehicle() && e instanceof Unit ? "_half.png" : ".png"));
    }

    // ------------------------------------------------------------------ portrait

    // ownerless portraits (neutral mobs, resources) get a plain blue backdrop
    private static final int NEUTRAL_TOP = 0xFF2A5F8C;
    private static final int NEUTRAL_BOTTOM = 0xFF132A40;

    private static void portraitFrame(HudFrame f, int x, int y, int w, int h, @Nullable String owner) {
        int colour = owner != null ? PlayerColors.getPlayerPortraitDisplayColorHex(owner) & 0xFFFFFF : 0;
        int top = colour == 0 ? NEUTRAL_TOP : 0xFF000000 | colour;
        int bottom = colour == 0 ? NEUTRAL_BOTTOM : 0xFF000000 | MiscUtil.shadeHexRGB(colour, 0.45f);
        Theme.outline(f.g, x, y, w, h, 1, Theme.INK);
        f.g.fillGradient(x, y, x + w, y + h, top, bottom);
        f.g.fill(x, y, x + w, y + 1, 0x24FFFFFF);
    }

    private static void drawEntityPortrait(HudFrame f, LivingEntity e, int x, int y, int w, int h) {
        portraitFrame(f, x, y, w, h, owner(e));
        var renderer = HudClientEvents.portraitRendererUnit;
        Clip.push(f.g, x, y, w, h);
        if (renderer.model != null && renderer.renderer != null)
            renderer.drawEntity(f.g, e, x, y, w, h);
        else
            MyRenderer.renderIcon(f.g, headIcon(e), x + (w - 32) / 2, y + (h - 32) / 2, 32);
        Clip.pop(f.g);
        portraitRect = new Rect(x, y, w, h);
        f.zone(portraitRect);
    }

    @Nullable
    private static String owner(LivingEntity e) {
        return e instanceof Unit u ? u.getOwnerName() : e instanceof Player p ? p.getName().getString() : null;
    }

    /** Name in big text, its tag right after it, and an optional label on the right. Returns the row height. */
    private static int nameRow(HudFrame f, String name, String tag, @Nullable String right, int rightColour, int x, int y, int w) {
        int rightW = right != null ? Text.smallWidth(right) + 8 : 0;
        int tagW = tag.isEmpty() ? 0 : Math.min(Text.smallWidth(tag), w / 2) + 6;
        String fitted = Text.fitBig(name, w - tagW - rightW, NAME);
        Text.big(f.g, fitted, x, y, Theme.TEXT, NAME);
        int h = Text.bigHeight(NAME);
        int tagY = y + h - 9;
        if (!tag.isEmpty())
            Text.small(f.g, Text.fitSmall(tag, tagW - 6), x + Text.bigWidth(fitted, NAME) + 6, tagY, Theme.ORANGE);
        if (right != null)
            Text.smallRight(f.g, right, x + w, tagY, rightColour);
        return h;
    }

    // ------------------------------------------------------------------ one unit

    private static void renderUnit(HudFrame f, Rect r, LivingEntity e) {
        drawEntityPortrait(f, e, r.x(), r.y(), PORTRAIT_W, r.h());
        if (e instanceof HeroUnit hero)
            levelBadge(f, r.x(), r.y(), hero.getHeroLevel());

        int rightW = rightColumnWidth(e);
        int ix = r.x() + PORTRAIT_W + COL_GAP;
        int iw = r.right() - ix - (rightW > 0 ? rightW + COL_GAP : 0);

        int by = r.y() + nameRow(f, displayName(e), unitTag(e), null, 0, ix, r.y(), iw) + 4;
        healthBar(f, ix, by, iw, HP_H, e.getHealth() + e.getAbsorptionAmount(),
                e.getMaxHealth() + (e.getAbsorptionAmount() > 0 ? MiscUtil.getMaxAbsorptionAmount(e) : 0));
        by += HP_H + 4;
        if (e instanceof HeroUnit hero)
            renderManaAndExperience(f, hero, ix, by, iw);

        if (e instanceof Unit unit)
            renderStatCards(f, unit, ix, r.bottom() - STAT_H, iw, STAT_H);

        if (rightW > 0)
            renderRightColumn(f, e, r.right() - rightW, r.y(), rightW, r.h());
    }

    /** What kind of unit and whose it is; the population cost is on the production button, not here. */
    private static String unitTag(LivingEntity e) {
        String kind = e instanceof HeroUnit ? I18n.get("hud.reignofnether.tag.hero")
                : e instanceof WorkerUnit ? I18n.get("hud.reignofnether.tag.worker")
                : ResourceSources.isHuntableAnimal(e) ? I18n.get("hud.reignofnether.tag.huntable") : "";
        String owner = owner(e);
        if (owner == null || owner.isBlank())
            return kind;
        String who = isMe(owner) ? I18n.get("hud.reignofnether.you") : owner.toUpperCase();
        return kind.isEmpty() ? who : kind + " · " + who;
    }

    private static boolean isMe(String owner) {
        var me = net.minecraft.client.Minecraft.getInstance().player;
        return me != null && owner.equals(me.getName().getString());
    }

    private static void levelBadge(HudFrame f, int x, int y, int level) {
        String s = I18n.get("hud.reignofnether.level_short", level);
        float scale = Text.px(15);
        int w = Text.bigWidth(s, scale) + 8;
        int h = Text.bigHeight(scale) + 3;
        Theme.orangeChip(f.g, x, y, w, h);
        f.g.fill(x, y + h - 1, x + w, y + h, Theme.ORANGE_DARK);
        Text.draw(f.g, s, x + 4, y + 2, Theme.ON_ORANGE);
    }

    private static void healthBar(HudFrame f, int x, int y, int w, int h, float hp, float max) {
        float frac = hp / Math.max(1f, max);
        Theme.bar(f.g, x, y, w, h, frac, Theme.hpColor(frac));
        String text = (hp >= 1 ? String.valueOf((int) hp) : String.format("%.1f", hp)) + " / " + (int) max;
        Text.smallShadowed(f.g, text, x + (w - Text.smallWidth(text)) / 2, y + (h - 8) / 2, Theme.TEXT);
    }

    /** Mana bar, then the experience bar at a fixed width, like the design. */
    private static void renderManaAndExperience(HudFrame f, HeroUnit hero, int x, int y, int w) {
        int h = 7;
        int xpW = Math.min(85, w / 3);
        int manaW = w - xpW - 5;
        Theme.bar(f.g, x, y, manaW, h, hero.getMana() / Math.max(1f, hero.getMaxMana()), Theme.MANA);
        String mana = (int) hero.getMana() + " / " + (int) hero.getMaxMana();
        Text.smallShadowed(f.g, mana, x + (manaW - Text.smallWidth(mana)) / 2, y + (h - 8) / 2, Theme.TEXT);

        int xx = x + manaW + 5;
        boolean max = hero.getHeroLevel() >= HeroUnit.MAX_LEVEL;
        float frac = max ? 1f : hero.getExpOnCurrentLevel() / (float) Math.max(1, hero.getExpToNextlevel());
        Theme.bar(f.g, xx, y, xpW, h, frac, Theme.XP);
        String xp = max ? I18n.get("hud.hero.reignofnether.max_level") : hero.getExpOnCurrentLevel() + " / " + hero.getExpToNextlevel() + " XP";
        Text.smallShadowed(f.g, Text.fitSmall(xp, xpW - 4), xx + (xpW - Math.min(xpW - 4, Text.smallWidth(xp))) / 2, y + (h - 8) / 2, Theme.TEXT);
        f.register(new ButtonBuilder("xp").build(), new Rect(xx, y, xpW, h), null, () -> max
                ? Tooltip.of(I18n.get("hud.hero.reignofnether.max_level"))
                : Tooltip.of(I18n.get("hud.hero.reignofnether.experience", hero.getExpOnCurrentLevel(), hero.getExpToNextlevel()))
                        .body(I18n.get("hud.hero.reignofnether.experience_warning")));
    }

    /** Icon and value per stat; the name and unit are in the tooltip, so nothing gets cut off. */
    private static void renderStatCards(HudFrame f, Unit unit, int x, int y, int w, int h) {
        List<Stat> stats = PortraitRendererUnit.computeStats(unit);
        if (stats.isEmpty())
            return;
        int gap = 4;
        int cw = (w - gap * (stats.size() - 1)) / stats.size();
        for (int i = 0; i < stats.size(); i++) {
            Stat s = stats.get(i);
            int cx = x + i * (cw + gap);
            Rect card = new Rect(cx, y, cw, h);
            Theme.card(f.g, cx, y, cw, h);
            if (f.hovered(card))
                f.g.fill(cx, y, cx + cw, y + h, Theme.HOVER);
            MyRenderer.renderIcon(f.g, s.icon(), cx + 5, y + (h - 10) / 2, 10);
            Text.draw(f.g, Text.fit(s.value(), cw - 22), cx + 18, y + (h - 8) / 2 + 1, s.colour());
            String name = I18n.get("hud.reignofnether.stat." + s.type().name().toLowerCase());
            String sub = statSub(s);
            f.register(new ButtonBuilder("stat").build(), card, null, () -> {
                Tooltip t = Tooltip.fromLines(unit.getStatTooltip(s.type()));
                return (t != null ? t : Tooltip.of(name)).status(sub.isEmpty() ? name : name + " · " + sub.toLowerCase(), Theme.MUTED);
            });
        }
    }

    private static String statSub(Stat s) {
        return switch (s.type()) {
            case ATTACK_DAMAGE -> I18n.get("hud.reignofnether.stat.sub.per_hit");
            case ATTACK_SPEED -> I18n.get("hud.reignofnether.stat.sub.per_sec");
            case RANGE -> I18n.get("hud.reignofnether.stat.sub.blocks");
            case ARMOUR -> I18n.get("hud.reignofnether.stat.sub.armour_" + PortraitRendererUnit.armourKind(s.colour()));
            case MOVEMENT_SPEED -> I18n.get("hud.reignofnether.stat.sub.speed");
            default -> "";
        };
    }

    // right column: effects, then inventory or what the unit carries

    private static int rightColumnWidth(LivingEntity e) {
        if (e instanceof UnitInventory && ItemClientEvents.shouldRenderUnitInventory(e))
            return 3 * SLOT + 2 * 4;
        if (e instanceof Unit u && !passiveIcons(u).isEmpty())
            return 3 * SLOT + 2 * 4;
        if (carried(e) != null)
            return 95;
        return 0;
    }

    private static List<Button> passiveIcons(Unit u) {
        List<Button> list = new ArrayList<>();
        for (Button b : u.getPassiveIcons())
            if (!b.isHidden.get())
                list.add(b);
        return list;
    }

    @Nullable
    private static Resources carried(LivingEntity e) {
        if (e instanceof Unit u && e instanceof Mob mob && mob.canPickUpLoot()) {
            Resources res = Resources.getTotalResourcesFromItems(u.getItems());
            return res.getTotalValue() > 0 ? res : null;
        }
        if (e instanceof Animal animal && ResourceSources.isHuntableAnimal(e))
            return PortraitRendererUnit.resourcesHeld(animal);
        return null;
    }

    private static void renderRightColumn(HudFrame f, LivingEntity e, int x, int y, int w, int h) {
        int cy = y;
        if (e instanceof Unit u) {
            List<Button> icons = passiveIcons(u);
            if (!icons.isEmpty()) {
                Text.small(f.g, I18n.get("hud.reignofnether.effects"), x, cy, Theme.LABEL);
                cy += 10;
                int ix = x;
                for (Button icon : icons) {
                    if (ix + EFFECT > x + w)
                        break;
                    effectTile(f, icon, ix, cy);
                    ix += EFFECT + 4;
                }
                cy += EFFECT + 6;
            }
        }
        if (e instanceof UnitInventory inv && ItemClientEvents.shouldRenderUnitInventory(e)) {
            Text.small(f.g, I18n.get("hud.reignofnether.inventory"), x, cy, Theme.LABEL);
            cy += 10;
            renderInventory(f, inv, x, cy);
            cy += 2 * (SLOT + 4) + 2;
            Resources res = carried(e);
            if (res != null && cy + 10 <= y + h)
                renderCarriedRow(f, e, res, x, cy, w);
            return;
        }
        Resources res = carried(e);
        if (res != null)
            renderCarried(f, e, res, x, cy, w, h - (cy - y));
    }

    private static void effectTile(HudFrame f, Button icon, int x, int y) {
        icon.refresh();
        int size = EFFECT;
        Theme.outline(f.g, x, y, size, size, 1, Theme.INK);
        f.g.fill(x, y, x + size, y + size, Theme.RAISED);
        String frame = icon.frameResource != null ? icon.frameResource.getPath() : "";
        int ring = frame.contains("diamond") ? Theme.BUFF : frame.contains("red") ? Theme.ERROR : Theme.FAINT;
        Theme.ring(f.g, x, y, size, size, 1, ring);
        ButtonCell.drawIcon(f.g, icon, x + 3, y + 3, size - 6);
        if (icon.greyPercent > 0 && icon instanceof MobEffectIcon) {
            int shade = Math.round(icon.greyPercent * size);
            f.g.fill(x, y, x + size, y + shade, 0x99000000);
        }
        f.register(icon, new Rect(x, y, size, size), null, null);
    }

    private static void renderInventory(HudFrame f, UnitInventory inv, int x, int y) {
        List<Button> slots = ItemClientEvents.inventoryButtons(inv);
        int size = SLOT;
        for (int i = 0; i < slots.size(); i++) {
            Button b = slots.get(i);
            int sx = x + (i % 3) * (size + 4);
            int sy = y + (i / 3) * (size + 4);
            Keybinding key = i < ItemClientEvents.hotkeys.size() ? ItemClientEvents.hotkeys.get(i) : null;
            if (b instanceof AbstractUnitItemButton item && item.unitItem instanceof EmptyUnitItem) {
                ButtonCell.renderEmpty(f.g, sx, sy, size, null);
                f.register(b, new Rect(sx, sy, size, size), null, null);
            } else {
                f.cell(b, sx, sy, size, b.hotkey != null ? key : null);
            }
        }
        Button hovered = null;
        HudFrame.Placed p = f.buttonAt(f.mouseX, f.mouseY);
        if (p != null && p.button() instanceof UnitItemInventoryButton)
            hovered = p.button();
        ItemClientEvents.setHoveredButton(hovered);
    }

    /** One line of carried resources under the inventory: icon and amount for each kind held. */
    private static void renderCarriedRow(HudFrame f, LivingEntity e, Resources res, int x, int y, int w) {
        int colour = e instanceof Unit u && Unit.atMaxResources(u) ? Theme.ERROR : Theme.TEXT;
        int[] values = { res.food, res.wood, res.ore, res.emerald };
        ResourceLocation[] icons = { ButtonCell.ICON_FOOD, ButtonCell.ICON_WOOD, ButtonCell.ICON_ORE, ButtonCell.ICON_EMERALD };
        int cx = x;
        for (int i = 0; i < values.length; i++) {
            String text = String.valueOf(values[i]);
            if (values[i] <= 0 || cx + 12 + Text.width(text) > x + w)
                continue;
            MyRenderer.renderIcon(f.g, icons[i], cx, y, 10);
            Text.draw(f.g, text, cx + 12, y + 1, colour);
            cx += 12 + Text.width(text) + 6;
        }
    }

    private static void renderCarried(HudFrame f, LivingEntity e, Resources res, int x, int y, int w, int h) {
        boolean full = e instanceof Unit u && Unit.atMaxResources(u);
        int colour = full ? Theme.ERROR : Theme.TEXT;
        Theme.card(f.g, x, y, w, h);
        int pad = 7;
        int cx = x + pad;
        int cy = y + pad;
        Text.small(f.g, I18n.get(e instanceof Animal ? "hud.reignofnether.food_value" : "hud.reignofnether.carrying"), cx, cy - 2, Theme.LABEL);
        cy += 9;
        float big = Text.px(26);
        int[] values = { res.food, res.wood, res.ore, res.emerald };
        ResourceLocation[] icons = { ButtonCell.ICON_FOOD, ButtonCell.ICON_WOOD, ButtonCell.ICON_ORE, ButtonCell.ICON_EMERALD };
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0)
                continue;
            MyRenderer.renderIcon(f.g, icons[i], cx, cy, 15);
            String text = e instanceof Unit u ? values[i] + "/" + u.getMaxResources() : String.valueOf(values[i]);
            Text.big(f.g, Text.fitBig(text, w - pad * 2 - 20, big), cx + 20, cy + (15 - Text.bigHeight(big)) / 2 + 1, colour, big);
            cy += 19;
        }
        if (e instanceof Unit u) {
            Theme.bar(f.g, cx, cy, w - pad * 2, 4, res.getTotalValue() / (float) Math.max(1, u.getMaxResources()), full ? Theme.ERROR : Theme.ORANGE);
            cy += 7;
            if (e instanceof WorkerUnit)
                for (String line : Text.wrapSmall(I18n.get(full ? "hud.reignofnether.carry_full" : "hud.reignofnether.carry_hint"), w - pad * 2)) {
                    if (cy + 7 > y + h - 2)
                        break;
                    Text.small(f.g, line, cx, cy, full ? Theme.ERROR : Theme.DIM);
                    cy += 7;
                }
        }
    }

    // ------------------------------------------------------------------ army

    private static void renderArmy(HudFrame f, Rect r, List<LivingEntity> army) {
        Map<String, List<LivingEntity>> types = new LinkedHashMap<>();
        int pop = 0;
        for (LivingEntity e : army) {
            types.computeIfAbsent(HudClientEvents.getModifiedEntityName(e), k -> new ArrayList<>()).add(e);
            if (e instanceof Unit u && u.getCost() != null)
                pop += u.getCost().population;
        }
        String active = HudClientEvents.getModifiedEntityName(HudClientEvents.hudSelectedEntity);

        String header = I18n.get("hud.reignofnether.units_count", army.size());
        float hs = Text.px(24);
        Text.big(f.g, header, r.x(), r.y(), Theme.TEXT, hs);
        int hh = Text.bigHeight(hs);
        Text.small(f.g, I18n.get("hud.reignofnether.pop_count", pop), r.x() + Text.bigWidth(header, hs) + 6, r.y() + hh - 9, Theme.ORANGE);
        if (!f.layout.compact())
            Text.smallRight(f.g, I18n.get("hud.reignofnether.army_hint"), r.right(), r.y() + hh - 9, Theme.DESC);

        int perRow = Math.max(1, (r.w() + GAP) / (STACK_W + GAP));
        int sy = r.y() + hh + 6;
        // keep room for at least one row of unit tiles under the stacks
        int stackRows = Math.max(1, (r.bottom() - sy - TILE - 6 + 5) / (STACK_H + 5));
        int maxStacks = perRow * stackRows;
        int i = 0;
        for (Map.Entry<String, List<LivingEntity>> type : types.entrySet()) {
            if (i >= maxStacks)
                break;
            renderStack(f, type.getValue(), type.getKey().equals(active), r.x() + (i % perRow) * (STACK_W + GAP), sy + (i / perRow) * (STACK_H + 5));
            i++;
        }
        int usedRows = (Math.min(types.size(), maxStacks) + perRow - 1) / perRow;
        int ty = sy + usedRows * (STACK_H + 5) + 1;
        List<LivingEntity> activeUnits = types.getOrDefault(active, List.of());
        if (r.bottom() - ty >= TILE)
            renderUnitTiles(f, activeUnits, r.x(), ty, r.w(), r.bottom() - ty);
    }

    private static void renderStack(HudFrame f, List<LivingEntity> units, boolean active, int x, int y) {
        LivingEntity first = units.get(0);
        Rect r = new Rect(x, y, STACK_W, STACK_H);
        Theme.cell(f.g, x, y, STACK_W, STACK_H, active ? Theme.CellState.ACTIVE : Theme.CellState.READY, f.hovered(r));
        MyRenderer.renderIcon(f.g, headIcon(first), x + 5, y + 5, 20);
        String count = "×" + units.size();
        Text.bigRight(f.g, count, x + STACK_W - 5, y + 5, active ? Theme.GOLD : Theme.TEXT, Text.px(20));
        float hp = 0;
        for (LivingEntity e : units)
            hp += e.getHealth() / Math.max(1, e.getMaxHealth());
        hp /= units.size();
        Theme.bar(f.g, x + 5, y + STACK_H - 6, STACK_W - 10, 3, hp, Theme.hpColor(hp));

        Button b = new ButtonBuilder(displayName(first))
                .entity(first)
                .onLeftClick(() -> {
                    if (Keybindings.ctrlMod.isDown())
                        keepOnly(units);
                    else if (Keybindings.shiftMod.isDown())
                        removeAll(units);
                    else
                        HudClientEvents.setHudSelectedEntity(first);
                })
                // on macOS Minecraft turns Ctrl+click into a right-click
                .onRightClick(() -> {
                    if (Keybindings.ctrlMod.isDown())
                        keepOnly(units);
                    else
                        removeAll(units);
                })
                .build();
        f.register(b, r, null, () -> Tooltip.of(displayName(first) + " ×" + units.size())
                .status(I18n.get("hud.reignofnether.stack_hint"), Theme.LABEL));
    }

    private static void keepOnly(List<LivingEntity> units) {
        UnitClientEvents.clearSelectedUnits();
        for (LivingEntity e : units)
            UnitClientEvents.addSelectedUnit(e);
        HudClientEvents.setHudSelectedEntity(units.get(0));
    }

    private static void removeAll(List<LivingEntity> units) {
        UnitClientEvents.getSelectedUnits().removeAll(units);
    }

    private static void renderUnitTiles(HudFrame f, List<LivingEntity> units, int x, int y, int w, int h) {
        int perRow = Math.max(1, (w + 4) / (TILE + 4));
        int rows = Math.max(1, (h + 4) / (TILE + 4));
        int capacity = perRow * rows;
        int shown = Math.min(units.size(), capacity);
        for (int i = 0; i < shown; i++) {
            LivingEntity e = units.get(i);
            int tx = x + (i % perRow) * (TILE + 4);
            int ty = y + (i / perRow) * (TILE + 4);
            Rect r = new Rect(tx, ty, TILE, TILE);
            boolean more = i == capacity - 1 && units.size() > capacity;
            Theme.cell(f.g, tx, ty, TILE, TILE, Theme.CellState.READY, f.hovered(r));
            if (more) {
                String n = "+" + (units.size() - capacity + 1);
                Text.drawCentered(f.g, n, r.centerX(), ty + 7, Theme.TEXT);
                break;
            }
            if (f.hovered(r))
                Theme.ring(f.g, tx, ty, TILE, TILE, 1, Theme.ORANGE);
            MyRenderer.renderIcon(f.g, headIcon(e), tx + 4, ty + 2, 12);
            float hp = e.getHealth() / Math.max(1, e.getMaxHealth());
            Theme.bar(f.g, tx + 2, ty + TILE - 5, TILE - 4, 3, hp, Theme.hpColor(hp));

            Button b = new ButtonBuilder(displayName(e))
                    .entity(e)
                    .onLeftClick(() -> {
                        if (Keybindings.shiftMod.isDown()) {
                            UnitClientEvents.getSelectedUnits().remove(e);
                        } else {
                            UnitClientEvents.clearSelectedUnits();
                            UnitClientEvents.addSelectedUnit(e);
                        }
                    })
                    .onRightClick(() -> {
                        if (Keybindings.ctrlMod.isDown())
                            keepOnly(List.of(e));
                        else
                            UnitClientEvents.getSelectedUnits().remove(e);
                    })
                    .build();
            f.register(b, r, null, () -> Tooltip.of(displayName(e))
                    .tag((int) e.getHealth() + " / " + (int) e.getMaxHealth(), Theme.hpColor(e.getHealth() / Math.max(1, e.getMaxHealth())))
                    .status(I18n.get("hud.reignofnether.tile_hint"), Theme.LABEL));
        }
    }

    // ------------------------------------------------------------------ buildings

    private static void renderBuilding(HudFrame f, Rect r, BuildingPlacement b) {
        portraitFrame(f, r.x(), r.y(), PORTRAIT_W, r.h(), b.ownerName);
        Clip.push(f.g, r.x(), r.y(), PORTRAIT_W, r.h());
        PortraitRendererBuilding.drawBlock(f.g, b.getBuilding().portraitBlock, r.x() + PORTRAIT_W / 2f, r.y() + r.h() / 2f - 6, 6.5f);
        Clip.pop(f.g);
        portraitRect = new Rect(r.x(), r.y(), PORTRAIT_W, r.h());
        f.zone(portraitRect);

        boolean owned = BuildingClientEvents.getPlayerToBuildingRelationship(b) == Relationship.OWNED;
        int ix = r.x() + PORTRAIT_W + COL_GAP;
        int iw = r.right() - ix;
        String rally = null;
        int rallyColour = Theme.DIM;
        if (b instanceof ProductionPlacement prod && owned && b.isBuilt) {
            boolean set = !prod.getRallyPoints().isEmpty() || prod.getRallyPointEntity() != null;
            rally = I18n.get(set ? "hud.reignofnether.rally_set" : "hud.reignofnether.no_rally");
            rallyColour = set ? Theme.OK : Theme.DIM;
        }
        int by = r.y() + nameRow(f, buildingName(b), buildingTag(b), rally, rallyColour, ix, r.y(), iw) + 4;
        healthBar(f, ix, by, iw, HP_H, b.getHealth(), b.getMaxHealth());
        by += HP_H + 5;

        if (b instanceof ProductionPlacement prod && (owned || !PlayerClientEvents.isRTSPlayer()))
            renderTraining(f, prod, owned, ix, by, iw, r.bottom() - by);
    }

    private static String buildingTag(BuildingPlacement b) {
        boolean own = BuildingClientEvents.getPlayerToBuildingRelationship(b) == Relationship.OWNED;
        StringBuilder sb = new StringBuilder();
        if (!b.isBuilt)
            sb.append(I18n.get("hud.reignofnether.tag.building_progress", (int) (b.getBlocksPlacedPercent() * 100)));
        else
            sb.append(I18n.get(b instanceof ProductionPlacement ? "hud.reignofnether.tag.production_building" : "hud.reignofnether.tag.building"));
        if (b instanceof SculkCatalystPlacement && b.isBuilt && b.getBuilding().hasActiveAddon(NightSourceAddon.class))
            sb.append(" · ").append(I18n.get("hud.buildings.reignofnether.sculk_catalyst.range",
                    b.getBuilding().getActiveAddon(NightSourceAddon.class).getNightRange(b), SculkCatalyst.MAX_NIGHT_RANGE));
        if (!b.ownerName.isBlank())
            sb.append(" · ").append(isMe(b.ownerName) ? I18n.get("hud.reignofnether.you") : b.ownerName.toUpperCase());
        return sb.toString();
    }

    private static void renderTraining(HudFrame f, ProductionPlacement prod, boolean owned, int x, int y, int w, int h) {
        Theme.card(f.g, x, y, w, h);
        int px = x + 7;
        int pw = w - 14;
        List<ActiveProduction> queue = prod.productionQueue;
        Text.small(f.g, I18n.get("hud.reignofnether.training"), px, y + 5, Theme.LABEL);
        Text.smallRight(f.g, I18n.get("hud.reignofnether.queue_count", queue.size()), px + pw, y + 5, Theme.FAINT);
        if (queue.isEmpty()) {
            Text.draw(f.g, Text.fit(I18n.get("hud.reignofnether.nothing_in_production"), pw), px, y + 18, Theme.LABEL);
            return;
        }

        ActiveProduction current = queue.get(0);
        int cs = Math.min(32, h - 18);
        int cy = y + 15;
        Button cancelFirst = current.item.getCancelButton(prod, true);
        queueCell(f, cancelFirst, px, cy, cs, owned, true, 0);

        float total = current.item.getCost(true, prod.ownerName).ticks;
        float done = total <= 0 ? 1f : 1f - current.ticksLeft / total;
        boolean blocked = !current.item.isBelowPopulationSupply(prod);
        int tx = px + cs + 7;
        int tw = Math.min(140, px + pw - tx);
        String left = blocked ? I18n.get("hud.reignofnether.paused")
                : I18n.get("hud.reignofnether.time_left", TopLeftPanel.formatTime((int) Math.ceil(current.ticksLeft / 20f)));
        int leftW = Text.smallWidth(left);
        Text.draw(f.g, Text.fit(cancelFirst.name, tw - leftW - 6), tx, cy + 2, Theme.TEXT);
        Text.smallRight(f.g, left, tx + tw, cy + 2, blocked ? Theme.ERROR : Theme.GOLD);
        Theme.bar(f.g, tx, cy + 13, tw, 6, done, blocked ? Theme.ERROR : Theme.ORANGE);
        String note = blocked ? I18n.get("hud.reignofnether.paused_population") : I18n.get("hud.reignofnether.in_production");
        Text.small(f.g, Text.fitSmall(note.toUpperCase(), tw), tx, cy + 22, blocked ? Theme.ERROR : Theme.DIM);

        int qx = tx + tw + 7;
        int qs = Math.min(25, cs);
        for (int i = 1; i < queue.size(); i++) {
            if (qx + qs > px + pw)
                break;
            queueCell(f, queue.get(i).item.getCancelButton(prod, false), qx, cy + (cs - qs) / 2, qs, owned, false, i + 1);
            qx += qs + 4;
        }
    }

    /** A queued item; clicking cancels it (refunding the cost). */
    private static void queueCell(HudFrame f, Button cancel, int x, int y, int size, boolean owned, boolean current, int number) {
        Rect r = new Rect(x, y, size, size);
        boolean hover = owned && f.hovered(r);
        Theme.outline(f.g, x, y, size, size, 1, Theme.INK);
        f.g.fill(x, y, x + size, y + size, hover ? Theme.CANCEL_HOVER : Theme.RAISED);
        if (current || hover)
            Theme.ring(f.g, x, y, size, size, 1, hover ? Theme.ERROR : Theme.ORANGE);
        ButtonCell.drawIcon(f.g, cancel, x + size / 5, y + size / 5, size - 2 * (size / 5));
        if (owned)
            Theme.cross(f.g, x + size - 6, y + 2, 4, Theme.DANGER_TEXT);
        if (number > 0)
            Text.small(f.g, String.valueOf(number), x + 2, y + size - 8, Theme.FAINT);
        if (!owned) {
            cancel.onLeftClick = null;
            cancel.onRightClick = null;
        }
        f.register(cancel, r, null, () -> Tooltip.of(I18n.get("hud.reignofnether.cancel_item", cancel.name))
                .status(I18n.get(current ? "hud.reignofnether.in_production" : "hud.reignofnether.queued"), Theme.MUTED));
    }

    private static void renderBuildingStacks(HudFrame f, Rect r, List<BuildingPlacement> selected) {
        Map<String, List<BuildingPlacement>> types = new LinkedHashMap<>();
        for (BuildingPlacement b : selected)
            types.computeIfAbsent(buildingName(b), k -> new ArrayList<>()).add(b);
        String active = buildingName(HudClientEvents.hudSelectedPlacement);
        float hs = Text.px(24);
        Text.big(f.g, I18n.get("hud.reignofnether.buildings_count", selected.size()), r.x(), r.y(), Theme.TEXT, hs);
        int sy = r.y() + Text.bigHeight(hs) + 6;

        int perRow = Math.max(1, (r.w() + GAP) / (STACK_W + GAP));
        int i = 0;
        for (Map.Entry<String, List<BuildingPlacement>> type : types.entrySet()) {
            int y = sy + (i / perRow) * (STACK_H + 5);
            if (y + STACK_H > r.bottom())
                break;
            List<BuildingPlacement> group = type.getValue();
            int x = r.x() + (i % perRow) * (STACK_W + GAP);
            Rect sr = new Rect(x, y, STACK_W, STACK_H);
            boolean on = type.getKey().equals(active);
            Theme.cell(f.g, x, y, STACK_W, STACK_H, on ? Theme.CellState.ACTIVE : Theme.CellState.READY, f.hovered(sr));
            MyRenderer.renderIcon(f.g, group.get(0).getBuilding().icon, x + 5, y + 5, 20);
            String count = "×" + group.size();
            Text.bigRight(f.g, count, x + STACK_W - 5, y + 5, on ? Theme.GOLD : Theme.TEXT, Text.px(20));
            float hp = 0;
            for (BuildingPlacement b : group)
                hp += b.getHealth() / (float) Math.max(1, b.getMaxHealth());
            hp /= group.size();
            Theme.bar(f.g, x + 5, y + STACK_H - 6, STACK_W - 10, 3, hp, Theme.hpColor(hp));
            Button b = new ButtonBuilder(type.getKey())
                    .onLeftClick(() -> {
                        if (on) {
                            BuildingClientEvents.clearSelectedBuildings();
                            group.forEach(BuildingClientEvents::addSelectedBuilding);
                        } else {
                            HudClientEvents.hudSelectedPlacement = group.get(0);
                        }
                    })
                    .build();
            f.register(b, sr, null, () -> Tooltip.of(type.getKey() + " ×" + group.size()));
            i++;
        }
    }
}
