package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.alliance.SharingClient;
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
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
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
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.NonUnitClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import com.solegendary.reignofnether.util.LanguageUtil;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    private static final int TILE = HudLayout.TILE_SMALL;
    private static final int GAP = 5;
    private static final int HP_H = 11;
    private static final int STAT_H = 24;
    private static final int SLOT = HudLayout.CELL;
    private static final int EFFECT = 25;
    private static final float NAME = Text.px(20); // medium: the body size

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
        // grouped once per frame: the names cost several translation lookups each
        Map<String, List<BuildingPlacement>> buildingTypes = buildingStacks ? groupBy(buildings, SelectionPanel::buildingName) : Map.of();
        Map<String, List<LivingEntity>> armyTypes = !buildingStacks && building == null && army.size() >= 2
                ? groupBy(army, HudClientEvents::getModifiedEntityName) : Map.of();

        int want;
        if (buildingStacks)
            want = stacksWidth(buildingTypes.size(), 0);
        else if (building != null)
            want = PORTRAIT_W + COL_GAP + INFO_W + 60;
        else if (army.size() >= 2)
            want = stacksWidth(armyTypes.size(), army.size());
        else {
            int rightW = rightColumnWidth(entity);
            want = PORTRAIT_W + COL_GAP + INFO_W + (rightW > 0 ? rightW + COL_GAP : 0);
        }
        int w = Math.min(room.w(), want);
        int x = Math.max(room.x(), Math.min(room.right() - w, (f.layout.width() - w) / 2));
        Rect r = new Rect(x, room.y(), w, room.h());
        int pad = HudLayout.CONSOLE_PAD;
        Rect card = new Rect(r.x() - pad, r.y() - pad, r.w() + pad * 2, r.h() + pad * 2);
        Theme.panel(f.g, card.x(), card.y(), card.w(), card.h(), Theme.floating());
        f.zone(card);

        if (buildingStacks)
            renderBuildingStacks(f, r, buildings, buildingTypes);
        else if (building != null)
            renderBuilding(f, r, building);
        else if (army.size() >= 2)
            renderArmy(f, r, army, armyTypes);
        else {
            renderUnit(f, r, entity);
            if (entity instanceof Unit u)
                renderEffects(f, u, card);
        }
    }

    /** Effects float in a row just above the card, so gaining or losing one never resizes it. */
    private static void renderEffects(HudFrame f, Unit u, Rect card) {
        int ix = card.x();
        int iy = card.y() - EFFECT - 3;
        for (Button icon : passiveIcons(u)) {
            if (ix + EFFECT > card.right()) {
                ix = card.x();
                iy -= EFFECT + 3;
            }
            effectTile(f, (LivingEntity) u, icon, ix, iy);
            ix += EFFECT + 3;
        }
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
                    || NonUnitClientEvents.canControlAllMobs() || SharingClient.canControl(e))
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
        // these professions change the head (hat, eyepatch); the others only the body
        if (e instanceof VillagerUnit v && switch (v.getUnitProfession()) { case FARMER, LUMBERJACK, HUNTER -> true; default -> false; })
            name += "_" + v.getUnitProfession().name().toLowerCase(Locale.ROOT);
        return MiscUtil.mobHeadIcon(e.isVehicle() && e instanceof Unit ? name + "_half" : name);
    }

    // ------------------------------------------------------------------ portrait

    // ownerless portraits (neutral mobs, resources) get a plain blue backdrop
    private static final int NEUTRAL_TOP = 0x2A5F8C;
    private static final int NEUTRAL_BOTTOM = 0x132A40;

    private static void portraitFrame(HudFrame f, int x, int y, int w, int h, @Nullable String owner) {
        int colour = owner != null ? PlayerColors.getPlayerPortraitDisplayColorHex(owner) & 0xFFFFFF : 0;
        int alpha = Theme.floating() == Theme.FLOATING ? 0x80000000 : 0xFF000000; // see-through, unless panels are solid
        int top = alpha | (colour == 0 ? NEUTRAL_TOP : colour);
        int bottom = alpha | (colour == 0 ? NEUTRAL_BOTTOM : MiscUtil.shadeHexRGB(colour, 0.45f) & 0xFFFFFF);
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
        if (e instanceof HeroUnit hero) {
            int badgeW = levelBadge(f, r.x(), r.y(), hero.getHeroLevel());
            if (hero.getSkillPoints() > 0 && UnitClientEvents.getPlayerToEntityRelationship(e) == Relationship.OWNED)
                skillPointBadge(f, r.x() + badgeW + 2, r.y(), hero.getSkillPoints());
        }

        int rightW = rightColumnWidth(e);
        int ix = r.x() + PORTRAIT_W + COL_GAP;
        int iw = r.right() - ix - (rightW > 0 ? rightW + COL_GAP : 0);

        int by = r.y() + nameRow(f, displayName(e), unitTag(e), null, 0, ix, r.y(), iw) + 4;
        healthBar(f, ix, by, iw, HP_H, e.getHealth() + e.getAbsorptionAmount(),
                e.getMaxHealth() + (e.getAbsorptionAmount() > 0 ? MiscUtil.getMaxAbsorptionAmount(e) : 0));
        by += HP_H + 4;
        if (e instanceof HeroUnit hero) {
            // spectators can't see the command card, so the empty band shows the hero's abilities instead
            boolean spectator = SpectatorHud.isActive();
            renderManaAndExperience(f, hero, ix, by, iw, !spectator);
            if (spectator)
                renderHeroAbilities(f, hero, ix, by + 7 + 4, iw, r.bottom() - STAT_H - GAP - (by + 7 + 4));
        } else if (e instanceof VillagerUnit v)
            renderProfessionExperience(f, v, ix, by, iw);

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

    /** Returns its width. */
    private static int levelBadge(HudFrame f, int x, int y, int level) {
        return badge(f, I18n.get("hud.reignofnether.level_short", level), x, y);
    }

    private static void skillPointBadge(HudFrame f, int x, int y, int points) {
        String s = "+" + points;
        int w = badge(f, s, x, y);
        f.register(new ButtonBuilder("skill points").build(), new Rect(x, y, w, Text.bigHeight(Text.px(15)) + 3), null,
                () -> Tooltip.of(I18n.get("hud.reignofnether.skill_points", points)));
    }

    private static int badge(HudFrame f, String s, int x, int y) {
        float scale = Text.px(15);
        int w = Text.bigWidth(s, scale) + 8;
        int h = Text.bigHeight(scale) + 3;
        Theme.orangeChip(f.g, x, y, w, h);
        f.g.fill(x, y + h - 1, x + w, y + h, Theme.ORANGE_DARK);
        Text.draw(f.g, s, x + 4, y + 2, Theme.ON_ORANGE);
        return w;
    }

    private static void healthBar(HudFrame f, int x, int y, int w, int h, float hp, float max) {
        float frac = hp / Math.max(1f, max);
        Theme.bar(f.g, x, y, w, h, frac, Theme.hpColor(frac));
        String text = (hp >= 1 ? String.valueOf((int) hp) : String.format("%.1f", hp)) + " / " + (int) max;
        barText(f, text, x, y, w, h, frac);
    }

    /** Bar label, centred: dark over the filled part, light over the empty part, split at the fill edge. */
    private static void barText(HudFrame f, String text, int x, int y, int w, int h, float frac) {
        String shown = Text.fitSmall(text, w - 4);
        int tx = x + (w - Text.smallWidth(shown)) / 2;
        int ty = y + Math.floorDiv(h - 9, 2);
        int filled = Math.round(w * Math.max(0, Math.min(1, frac)));
        if (filled > 0) {
            Clip.push(f.g, x, y - 4, filled, h + 8);
            Text.small(f.g, shown, tx, ty, Theme.ON_ORANGE);
            Clip.pop(f.g);
        }
        if (filled < w) {
            Clip.push(f.g, x + filled, y - 4, w - filled, h + 8);
            Text.small(f.g, shown, tx, ty, Theme.TEXT);
            Clip.pop(f.g);
        }
    }

    /**
     * A villager's way to a profession, then to veteran: the profession it is learning (the one with the most
     * progress) until it has one, then its experience towards veteran.
     */
    private static void renderProfessionExperience(HudFrame f, VillagerUnit v, int x, int y, int w) {
        int h = HP_H;
        VillagerUnitProfession p = professionShown(v);
        String label;
        float frac;
        if (p == VillagerUnitProfession.NONE) {
            label = I18n.get("hud.reignofnether.profession.none");
            frac = 0;
        } else if (v.isVeteran()) {
            label = I18n.get("hud.reignofnether.profession.veteran");
            frac = 1;
        } else {
            boolean learning = v.getUnitProfession() == VillagerUnitProfession.NONE;
            int need = learning ? VillagerUnit.getExpReq(p) / 2 : VillagerUnit.getExpReq(p);
            int exp = Math.min(v.getExp(p), need);
            label = I18n.get(learning ? "hud.reignofnether.profession.learning" : "hud.reignofnether.profession.to_veteran",
                    professionName(p).toUpperCase(Locale.ROOT), exp, need);
            frac = exp / (float) need;
        }
        Theme.bar(f.g, x, y, w, h, frac, Theme.XP);
        barText(f, label, x, y, w, h, frac);
        f.register(new ButtonBuilder("profession").build(), new Rect(x, y, w, h), null, () -> professionTooltip(v));
    }

    /** Its profession, or the one it is closest to learning; NONE before any work. */
    public static VillagerUnitProfession professionShown(VillagerUnit v) {
        if (v.getUnitProfession() != VillagerUnitProfession.NONE)
            return v.getUnitProfession();
        VillagerUnitProfession best = VillagerUnitProfession.NONE;
        float bestFrac = 0;
        for (VillagerUnitProfession p : VillagerUnitProfession.values()) {
            float frac = v.getExp(p) / (float) VillagerUnit.getExpReq(p);
            if (p != VillagerUnitProfession.NONE && frac > bestFrac) {
                best = p;
                bestFrac = frac;
            }
        }
        return best;
    }

    private static String professionName(VillagerUnitProfession p) {
        return I18n.get("units.reignofnether." + p.name().toLowerCase(Locale.ROOT));
    }

    /** What the profession does, what veteran adds and how it is earned; shared with the effects row icon. */
    public static Tooltip professionTooltip(VillagerUnit v) {
        VillagerUnitProfession p = professionShown(v);
        if (p == VillagerUnitProfession.NONE)
            return Tooltip.of(I18n.get("hud.reignofnether.profession.none_title"))
                    .body(I18n.get("hud.reignofnether.profession.none_desc"));
        String key = "hud.reignofnether.profession." + p.name().toLowerCase(Locale.ROOT);
        boolean has = v.getUnitProfession() == p;
        int req = VillagerUnit.getExpReq(p);
        String status = v.isVeteran() ? I18n.get("hud.reignofnether.profession.is_veteran")
                : has ? I18n.get("hud.reignofnether.profession.veteran_at", req)
                : I18n.get("hud.reignofnether.profession.learn_at", professionName(p), req / 2);
        Tooltip t = Tooltip.of(professionName(p))
                .tag(I18n.get("hud.reignofnether.tag.passive"), Theme.XP)
                .body(I18n.get(key + ".bonus"))
                .body(I18n.get(key + ".veteran"))
                .status(I18n.get(key + ".xp"), Theme.MUTED)
                .status(status, v.isVeteran() ? Theme.GOLD : Theme.LABEL);
        // still learning: the bar only shows the closest profession, so list the progress in each one
        if (!has)
            for (VillagerUnitProfession other : VillagerUnitProfession.values())
                if (other != VillagerUnitProfession.NONE && v.getExp(other) > 0)
                    t.status(I18n.get("hud.reignofnether.profession.progress", professionName(other),
                            Math.min(v.getExp(other), VillagerUnit.getExpReq(other) / 2), VillagerUnit.getExpReq(other) / 2), Theme.MUTED);
        return t;
    }

    /** Mana bar, then the experience bar: on its own line below, or beside it at a fixed width. */
    private static void renderManaAndExperience(HudFrame f, HeroUnit hero, int x, int y, int w, boolean xpOwnLine) {
        int h = 7;
        int xpW = xpOwnLine ? w : Math.min(85, w / 3);
        int manaW = xpOwnLine ? w : w - xpW - 5;
        float manaFrac = hero.getMana() / Math.max(1f, hero.getMaxMana());
        Theme.bar(f.g, x, y, manaW, h, manaFrac, Theme.MANA);
        barText(f, (int) hero.getMana() + " / " + (int) hero.getMaxMana(), x, y, manaW, h, manaFrac);

        int xx = xpOwnLine ? x : x + manaW + 5;
        if (xpOwnLine)
            y += h + 4;
        boolean max = hero.getHeroLevel() >= HeroUnit.MAX_LEVEL;
        float frac = max ? 1f : hero.getExpOnCurrentLevel() / (float) Math.max(1, hero.getExpToNextlevel());
        Theme.bar(f.g, xx, y, xpW, h, frac, Theme.XP);
        String xp = max ? I18n.get("hud.hero.reignofnether.max_level") : hero.getExpOnCurrentLevel() + " / " + hero.getExpToNextlevel() + " XP";
        barText(f, xp, xx, y, xpW, h, frac);
        f.register(new ButtonBuilder("xp").build(), new Rect(xx, y, xpW, h), null, () -> max
                ? Tooltip.of(I18n.get("hud.hero.reignofnether.max_level"))
                : Tooltip.of(I18n.get("hud.hero.reignofnether.experience", hero.getExpOnCurrentLevel(), hero.getExpToNextlevel()))
                        .body(I18n.get("hud.hero.reignofnether.experience_warning")));
    }

    /** Read-only row of the hero's abilities with cooldowns; registered for the tooltip only, so not clickable. */
    private static void renderHeroAbilities(HudFrame f, HeroUnit hero, int x, int y, int w, int h) {
        int size = Math.min(h, SLOT);
        if (size < 12)
            return;
        int cx = x;
        for (Button b : hero.getAbilityButtons()) {
            if (!(b instanceof AbilityButton ab && ab.ability instanceof HeroAbility) || cx + size > x + w)
                continue;
            Rect cell = new Rect(cx, y + (h - size) / 2, size, size);
            ButtonCell.render(f.g, b, cell.x(), cell.y(), size, null, f.hovered(cell));
            f.register(new ButtonBuilder("ability").build(), cell, null, () -> ButtonCell.tooltipFor(b, null));
            cx += size + 3;
        }
    }

    /** Icon and value per stat; the name and unit are in the tooltip, so nothing gets cut off. */
    private static void renderStatCards(HudFrame f, Unit unit, int x, int y, int w, int h) {
        List<Stat> stats = PortraitRendererUnit.computeStats(unit);
        if (stats.isEmpty())
            return;
        int gap = 3;
        int cw = (w - gap * (stats.size() - 1)) / stats.size();
        for (int i = 0; i < stats.size(); i++) {
            Stat s = stats.get(i);
            int cx = x + i * (cw + gap);
            Rect card = new Rect(cx, y, cw, h);
            Theme.card(f.g, cx, y, cw, h);
            if (f.hovered(card))
                f.g.fill(cx, y, cx + cw, y + h, Theme.HOVER);
            MyRenderer.renderIcon(f.g, s.icon(), cx + 3, y + (h - 10) / 2, 10);
            Text.draw(f.g, Text.fit(s.value(), cw - 16), cx + 14, y + (h - 8) / 2 + 1, s.colour());
            String name = I18n.get("hud.reignofnether.stat." + s.type().name().toLowerCase());
            String statUnit = statUnit(s);
            f.register(new ButtonBuilder("stat").build(), card, null, () -> {
                Tooltip t = Tooltip.fromLines(unit.getStatTooltip(s.type()));
                t = t != null ? t : Tooltip.of(name);
                return statUnit.isEmpty() ? t : t.status(statUnit, Theme.MUTED);
            });
        }
    }

    /** The stat's unit, where its tooltip title doesn't already say it. */
    private static String statUnit(Stat s) {
        return switch (s.type()) {
            case ATTACK_DAMAGE -> I18n.get("hud.reignofnether.stat.sub.per_hit");
            case RANGE -> I18n.get("hud.reignofnether.stat.sub.blocks");
            default -> "";
        };
    }

    // right column: inventory or what the unit carries

    private static int rightColumnWidth(LivingEntity e) {
        if (e instanceof UnitInventory && ItemClientEvents.shouldRenderUnitInventory(e))
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

    private static void effectTile(HudFrame f, LivingEntity e, Button icon, int x, int y) {
        icon.refresh();
        int size = EFFECT;
        Theme.outline(f.g, x, y, size, size, 1, Theme.INK);
        f.g.fill(x, y, x + size, y + size, Theme.RAISED);
        String frame = icon.frameResource != null ? icon.frameResource.getPath() : "";
        int ring = frame.contains("diamond") ? Theme.BUFF : frame.contains("red") ? Theme.ERROR : Theme.FAINT;
        Theme.ring(f.g, x, y, size, size, 1, ring);
        ButtonCell.drawIcon(f.g, icon, x + 3, y + 3, size - 6);
        // timed effects: the ring goes dark clockwise from the top as the effect runs out
        if (icon instanceof MobEffectIcon mei && mei.duration >= MobEffectIcon.MIN_DURATION_SHOWN) {
            // whichever moved further: the client's copy of the effect, or real time since the icon appeared
            var live = e.getEffect(mei.effect);
            if (live == null || !live.isInfiniteDuration()) {
                float byLive = live != null ? 1f - live.getDuration() / (float) Math.max(mei.duration, live.getDuration()) : 0f;
                float byTime = (System.currentTimeMillis() - mei.createdMs) / (mei.duration * 50f);
                clockRing(f.g, x, y, size, Math.max(byLive, byTime)); // Theme.ring draws on the tile's outer edge
            }
        }
        f.register(icon, new Rect(x, y, size, size), null, null);
    }

    /** Darkens the first `elapsed` fraction of a 1-wide square ring of side s, clockwise from its top middle. */
    private static void clockRing(GuiGraphics g, int x, int y, int s, float elapsed) {
        int side = s - 1;
        int n = 4 * side;
        int dark = Math.round(Math.max(0, Math.min(1, elapsed)) * n);
        for (int i = 0; i < dark; i++) {
            int p = (i + s / 2) % n;
            int px, py;
            if (p < side) { px = p; py = 0; }
            else if (p < 2 * side) { px = side; py = p - side; }
            else if (p < 3 * side) { px = side - (p - 2 * side); py = side; }
            else { px = 0; py = side - (p - 3 * side); }
            g.fill(x + px, y + py, x + px + 1, y + py + 1, Theme.INK);
        }
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

    // in the order of Resources' food, wood, ore, emerald
    private static final ResourceLocation[] RESOURCE_ICONS = { ButtonCell.ICON_FOOD, ButtonCell.ICON_WOOD, ButtonCell.ICON_ORE, ButtonCell.ICON_EMERALD };

    /** One line of carried resources under the inventory: icon and amount for each kind held. */
    private static void renderCarriedRow(HudFrame f, @Nullable LivingEntity e, Resources res, int x, int y, int w) {
        int colour = e instanceof Unit u && Unit.atMaxResources(u) ? Theme.ERROR : Theme.TEXT;
        int[] values = { res.food, res.wood, res.ore, res.emerald };
        int cx = x;
        for (int i = 0; i < values.length; i++) {
            String text = String.valueOf(values[i]);
            if (values[i] <= 0 || cx + 12 + Text.width(text) > x + w)
                continue;
            MyRenderer.renderIcon(f.g, RESOURCE_ICONS[i], cx, y, 10);
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
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0)
                continue;
            MyRenderer.renderIcon(f.g, RESOURCE_ICONS[i], cx, cy, 15);
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

    private static <T> Map<String, List<T>> groupBy(List<T> items, java.util.function.Function<T, String> name) {
        Map<String, List<T>> types = new LinkedHashMap<>();
        for (T t : items)
            types.computeIfAbsent(name.apply(t), k -> new ArrayList<>()).add(t);
        return types;
    }

    private static void renderArmy(HudFrame f, Rect r, List<LivingEntity> army, Map<String, List<LivingEntity>> types) {
        int pop = 0;
        for (LivingEntity e : army)
            if (e instanceof Unit u && u.getCost() != null)
                pop += u.getCost().population;
        String active = HudClientEvents.getModifiedEntityName(HudClientEvents.hudSelectedEntity);

        String header = I18n.get("hud.reignofnether.units_count", army.size());
        float hs = Text.px(24);
        Text.big(f.g, header, r.x(), r.y(), Theme.TEXT, hs);
        int hh = Text.bigHeight(hs);
        String popText = I18n.get("hud.reignofnether.pop_count", pop);
        int px = r.x() + Text.bigWidth(header, hs) + 6;
        Text.small(f.g, popText, px, r.y() + hh - 9, Theme.ORANGE);
        // what the whole selection carries, so workers can be sent to drop off early
        Resources held = new Resources("", 0, 0, 0, 0);
        for (LivingEntity e : army) {
            Resources c = e instanceof Unit ? carried(e) : null;
            if (c != null)
                held.changeInstantly(c.food, c.wood, c.ore, c.emerald);
        }
        int hx = px + Text.smallWidth(popText) + 10;
        String help = "[?]";
        int helpW = Text.smallWidth(help);
        Rect helpRect = new Rect(r.right() - helpW, r.y(), helpW, hh);
        if (held.getTotalValue() > 0)
            renderCarriedRow(f, null, held, hx, r.y() + hh - 11, helpRect.x() - 4 - hx);
        Text.smallRight(f.g, help, r.right(), r.y() + hh - 9, f.hovered(helpRect) ? Theme.TEXT : Theme.DESC);
        f.register(new ButtonBuilder("army hint").build(), helpRect, null, () ->
                Tooltip.of(I18n.get("hud.reignofnether.hotkeys")).body(I18n.get("hud.reignofnether.army_hint")));

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
        UnitClientEvents.removeSelectedUnits(units);
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
                            UnitClientEvents.removeSelectedUnits(List.of(e));
                        } else {
                            UnitClientEvents.clearSelectedUnits();
                            UnitClientEvents.addSelectedUnit(e);
                        }
                    })
                    .onRightClick(() -> {
                        if (Keybindings.ctrlMod.isDown())
                            keepOnly(List.of(e));
                        else
                            UnitClientEvents.removeSelectedUnits(List.of(e));
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
        PortraitRendererBuilding.drawBlock(f.g, b.getBuilding().portraitBlock, r.x() + PORTRAIT_W / 2f, r.y() + r.h() / 2f - 6, 11f);
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

        // top: the unit in production, its name, time left and a bar across the whole card
        ActiveProduction current = queue.get(0);
        int cs = 24;
        int cy = y + 15;
        Button cancelFirst = current.item.getCancelButton(prod, true);
        queueCell(f, cancelFirst, px, cy, cs, owned, true, 0);

        float total = current.item.getCost(true, prod.ownerName).ticks;
        float done = total <= 0 ? 1f : 1f - current.ticksLeft / total;
        boolean blocked = !current.item.isBelowPopulationSupply(prod);
        int tx = px + cs + 6;
        int tw = px + pw - tx;
        String left = blocked ? I18n.get("hud.reignofnether.paused_population")
                : I18n.get("hud.reignofnether.time_left", TimeUtils.formatSeconds((int) Math.ceil(current.ticksLeft / 20f)));
        int leftW = Text.smallWidth(left);
        Text.draw(f.g, Text.fit(cancelFirst.name, tw - leftW - 6), tx, cy - 1, Theme.TEXT);
        Text.smallRight(f.g, left, tx + tw, cy, blocked ? Theme.ERROR : Theme.GOLD);
        Theme.bar(f.g, tx, cy + cs - 5, tw, 5, done, blocked ? Theme.ERROR : Theme.ORANGE);

        // bottom: the rest of the queue in cells as big as the height allows (two rows if both stay clickable),
        // as many columns as fit
        final int gap = 3;
        int gy = cy + cs + 5;
        int avail = y + h - 5 - gy;
        int rows = avail >= 2 * 18 + gap ? 2 : 1;
        int qs = Math.max(8, Math.min(27, (avail - (rows - 1) * gap) / rows));
        int cols = Math.max(1, (pw + gap) / (qs + gap));
        int queued = queue.size() - 1;
        for (int slot = 0; slot < Math.min(queued, cols * rows); slot++) {
            int cx = px + (slot % cols) * (qs + gap);
            int ry = gy + (slot / cols) * (qs + gap);
            if (slot == cols * rows - 1 && queued > cols * rows) {
                Theme.outline(f.g, cx, ry, qs, qs, 1, Theme.INK);
                f.g.fill(cx, ry, cx + qs, ry + qs, Theme.RAISED);
                String more = "+" + (queued - slot);
                Text.small(f.g, more, cx + (qs - Text.smallWidth(more)) / 2, ry + (qs - 8) / 2, Theme.FAINT);
                break;
            }
            queueCell(f, queue.get(slot + 1).item.getCancelButton(prod, false), cx, ry, qs, owned, false, qs >= 18 ? slot + 2 : 0);
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

    private static void renderBuildingStacks(HudFrame f, Rect r, List<BuildingPlacement> selected, Map<String, List<BuildingPlacement>> types) {
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
