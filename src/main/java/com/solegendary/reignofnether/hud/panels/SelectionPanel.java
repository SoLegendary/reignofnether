package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.alliance.AlliancesClient;
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
import com.solegendary.reignofnether.hud.effecticons.ProfessionIcon;
import com.solegendary.reignofnether.hud.effecticons.ProfessionIcons;
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
 * The middle of the HUD bar: what is selected, in fixed columns. The portrait with health below it, stats, item
 * slots, the carried resources, then what depends on the selection: an army (type stacks with counts plus the
 * units of the active type), a production queue, or several buildings. The name sits in a tab above the bar.
 */
public final class SelectionPanel {

    private SelectionPanel() { }

    // sizes are the design's at half scale
    private static final int PORTRAIT_W = 85;
    private static final int STATS_W = 44;
    private static final int BUILDING_INFO_W = 75;
    private static final int SLOT = 32; // three rows fill the bar's height
    private static final int SLOT_GAP = 4;
    private static final int SLOTS_W = 2 * SLOT + SLOT_GAP;
    private static final int GATHER_W = 58;
    private static final int SEP = 13; // between columns: a gap, the divider and a gap
    private static final int MID_MIN = 160;
    private static final int MID_MAX = 385;
    private static final int TRAINING_W = 260;
    private static final int STACK_W = 58;
    private static final int STACK_H = 31;
    private static final int TILE = HudLayout.TILE_SMALL;
    private static final int GAP = 5;
    private static final int HP_H = 12;
    private static final int MANA_H = 10;
    private static final int XP_H = 4;
    private static final int STAT_GAP = 3;
    private static final int EFFECT = 25;
    private static final float NAME = Text.px(20); // medium: the body size

    // where the portrait was drawn, so holding the mouse on it can follow the unit
    private static Rect portraitRect = Rect.EMPTY;

    public static Rect portraitRect() {
        return portraitRect;
    }

    /** What is selected this frame, grouped once: the names cost several translation lookups each. */
    private record Selection(@Nullable BuildingPlacement building, @Nullable LivingEntity entity,
                             List<BuildingPlacement> buildings, boolean buildingStacks, List<LivingEntity> army,
                             Map<String, List<BuildingPlacement>> buildingTypes, Map<String, List<LivingEntity>> armyTypes) { }

    private static Selection selection;

    /** Call once a frame before {@link #width} and {@link #render}. */
    public static void prepare() {
        BuildingPlacement building = HudClientEvents.hudSelectedPlacement;
        LivingEntity entity = HudClientEvents.hudSelectedEntity;
        List<BuildingPlacement> buildings = BuildingClientEvents.getSelectedBuildings();
        boolean buildingStacks = building != null && buildings.size() >= 2
                && BuildingClientEvents.getPlayerToBuildingRelationship(building) == Relationship.OWNED;
        List<LivingEntity> army = building == null ? controllableSelection() : List.of();
        Map<String, List<BuildingPlacement>> buildingTypes = buildingStacks ? groupBy(buildings, SelectionPanel::buildingName) : Map.of();
        Map<String, List<LivingEntity>> armyTypes = building == null && army.size() >= 2
                ? groupBy(army, HudClientEvents::getModifiedEntityName) : Map.of();
        selection = new Selection(building, entity, buildings, buildingStacks, army, buildingTypes, armyTypes);
    }

    private static boolean showsSlots(Selection s) {
        return s.building() == null && ItemClientEvents.shouldRenderUnitInventory(s.entity());
    }

    private static boolean showsInfo(HudFrame f, Selection s) {
        if (f.layout.compact())
            return false;
        return s.building() != null ? showsRally(s.building()) || showsRange(s.building()) : s.entity() instanceof Unit;
    }

    /** Small screens need the room for the group, so there it only shows for one unit. */
    private static boolean showsGathering(HudFrame f, Selection s) {
        return s.building() == null && (s.entity() instanceof Unit || ResourceSources.isHuntableAnimal(s.entity()))
                && (!f.layout.compact() || s.army().size() < 2);
    }

    private static int infoWidth(Selection s) {
        return s.building() != null ? BUILDING_INFO_W : STATS_W;
    }

    private static int columnsWidth(HudFrame f, Selection s) {
        return PORTRAIT_W + (showsInfo(f, s) ? SEP + infoWidth(s) : 0) + (showsSlots(s) ? SEP + SLOTS_W : 0)
                + (showsGathering(f, s) ? SEP + GATHER_W : 0);
    }

    private static boolean showsTraining(Selection s) {
        return s.building() instanceof ProductionPlacement && !s.buildingStacks()
                && (s.building().isBuilt || s.building().allowProdWhileBuilding)
                && (BuildingClientEvents.getPlayerToBuildingRelationship(s.building()) == Relationship.OWNED || !PlayerClientEvents.isRTSPlayer());
    }

    /** How wide the part right of the columns wants to be; 0 when it has nothing to show. */
    private static int midWidth(HudFrame f, Selection s) {
        int room = f.layout.selection().w() - columnsWidth(f, s) - SEP;
        int want = 0;
        if (s.buildingStacks())
            want = Math.max(MID_MIN, s.buildingTypes().size() * (STACK_W + GAP) - GAP);
        else if (showsTraining(s))
            want = TRAINING_W;
        else if (s.army().size() >= 2 && s.entity() != null) {
            int active = s.armyTypes().getOrDefault(HudClientEvents.getModifiedEntityName(s.entity()), List.of()).size();
            want = Math.max(MID_MIN, Math.max(s.armyTypes().size() * (STACK_W + GAP) - GAP, active * (TILE + 4) - 4));
        }
        return Math.max(0, Math.min(Math.min(want, MID_MAX), room));
    }

    /** With nothing selected the bar only holds the command card. */
    public static boolean isEmpty() {
        return selection.building() == null && selection.entity() == null;
    }

    /** Width of everything drawn, so the bar can end right after it on wide screens. */
    public static int width(HudFrame f) {
        int mid = midWidth(f, selection);
        return columnsWidth(f, selection) + (mid > 0 ? SEP + mid : 0);
    }

    /** One column after another, each with a divider before it; the portrait column is always there. */
    public static void render(HudFrame f) {
        portraitRect = Rect.EMPTY;
        Selection s = selection;
        Rect room = f.layout.selection();
        if (room.w() <= 0)
            return;
        if (isEmpty())
            return;
        int x = room.x();
        if (s.building() != null)
            renderBuildingPortrait(f, s.building(), x, room.y(), room.h());
        else
            renderUnitPortrait(f, s.entity(), x, room.y(), room.h());
        x += PORTRAIT_W;

        if (showsInfo(f, s)) {
            x = column(f, x, room);
            if (s.building() != null)
                renderBuildingInfo(f, s.building(), x, room.y(), BUILDING_INFO_W);
            else
                renderUnitInfo(f, s.entity(), x, room.y(), STATS_W, room.h());
            x += infoWidth(s);
        }
        if (showsSlots(s)) {
            x = column(f, x, room);
            renderSlots(f, s.entity(), x, room.y());
            x += SLOTS_W;
        }
        if (showsGathering(f, s)) {
            x = column(f, x, room);
            renderGathering(f, s.entity(), s.army().size() >= 2 ? s.army() : List.of(s.entity()), x, room.y(), room.h());
            x += GATHER_W;
        }
        int mid = midWidth(f, s);
        if (mid > 0) {
            x = column(f, x, room);
            Rect r = new Rect(x, room.y(), mid, room.h());
            if (s.buildingStacks())
                renderBuildingStacks(f, r, s.buildings(), s.buildingTypes());
            else if (s.building() != null)
                renderTraining(f, (ProductionPlacement) s.building(), BuildingClientEvents.getPlayerToBuildingRelationship(s.building()) == Relationship.OWNED, r.x(), r.y(), r.w(), r.h());
            else
                renderArmy(f, r, s.army(), s.armyTypes());
        }
        int barTop = f.layout.console().y();
        int tabH = s.building() != null
                ? renderNameTab(f, buildingName(s.building()), s.building().ownerName, 0, room.x(), barTop)
                : renderNameTab(f, displayName(s.entity()), owner(s.entity()), s.entity() instanceof HeroUnit hero ? hero.getHeroLevel() : 0, room.x(), barTop);
        if (s.building() == null && s.army().size() < 2 && s.entity() instanceof Unit u)
            renderEffects(f, u, room.x(), barTop - tabH);
    }

    /** Draws the divider after the column ending at x and returns where the next one starts. */
    private static int column(HudFrame f, int x, Rect room) {
        Theme.divider(f.g, x + SEP / 2, room.y(), 1, room.h());
        return x + SEP;
    }

    /** Level, name and owner on the bar's top edge, from the portrait's left. The tab grows with the text, so long names are never cut off. */
    private static int renderNameTab(HudFrame f, String name, @Nullable String owner, int level, int x, int barTop) {
        String who = owner == null || owner.isBlank() ? "" : isMe(owner) ? I18n.get("hud.reignofnether.you") : owner;
        String lv = level > 0 ? I18n.get("hud.reignofnether.level_short", level) : "";
        int lvW = lv.isEmpty() ? 0 : Text.bigWidth(lv, NAME) + 5;
        int nameW = lvW + Text.bigWidth(name, NAME);
        int whoW = who.isEmpty() ? 0 : Text.smallWidth(who);
        int room = f.layout.width() - x - 4;
        int inline = nameW + (whoW > 0 ? 5 + whoW : 0);
        boolean oneLine = inline + 10 <= room;
        int nameH = Text.bigHeight(NAME);
        int h = nameH + 6 + (oneLine || whoW == 0 ? 0 : 10);
        int w = Math.min(room, 10 + (oneLine ? inline : Math.max(nameW, whoW)));
        int y = barTop - h;
        Theme.panel(f.g, x, y, w, h, Theme.floating());
        f.zone(new Rect(x, y, w, h));
        Text.big(f.g, lv, x + 5, y + 3, Theme.MUTED, NAME);
        Text.big(f.g, name, x + 5 + lvW, y + 3, Theme.TEXT, NAME);
        if (whoW > 0)
            Text.small(f.g, who, oneLine ? x + 10 + nameW : x + 5, oneLine ? y + nameH - 6 : y + nameH + 4, Theme.GOLD);
        return h;
    }

    /** Effects float in a row above the name tab, so gaining or losing one never resizes it. */
    private static void renderEffects(HudFrame f, Unit u, int left, int barTop) {
        int ix = left;
        int iy = barTop - EFFECT - 3;
        // a villager still learning has no profession icon yet: its progress gets a tile all the same
        if (u instanceof VillagerUnit v && !v.hasUnitProfession() && professionShown(v) != VillagerUnitProfession.NONE) {
            Button learning = ProfessionIcons.getProfessionIcon(professionShown(v), false);
            effectTile(f, (LivingEntity) u, learning, ix, iy);
            f.register(new ButtonBuilder("profession").build(), new Rect(ix, iy, EFFECT, EFFECT), null, () -> professionTooltip(v));
            ix += EFFECT + 3;
        }
        for (Button icon : passiveIcons(u)) {
            effectTile(f, (LivingEntity) u, icon, ix, iy);
            ix += EFFECT + 3;
        }
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

    // ------------------------------------------------------------------ one unit

    /** Portrait with the health below it, then mana for heroes; experience runs along the portrait's bottom edge. */
    private static void renderUnitPortrait(HudFrame f, LivingEntity e, int x, int y, int h) {
        HeroUnit hero = e instanceof HeroUnit he ? he : null;
        int portraitH = h - HP_H - 3 - (hero != null ? MANA_H + 2 : 0);
        drawEntityPortrait(f, e, x, y, PORTRAIT_W, portraitH);
        int by = y + portraitH + 3;
        healthBar(f, x, by, PORTRAIT_W, HP_H, e.getHealth() + e.getAbsorptionAmount(),
                e.getMaxHealth() + (e.getAbsorptionAmount() > 0 ? MiscUtil.getMaxAbsorptionAmount(e) : 0));
        if (hero == null)
            return;
        renderExperience(f, hero, x, y + portraitH - XP_H, PORTRAIT_W);
        float manaFrac = hero.getMana() / Math.max(1f, hero.getMaxMana());
        Theme.bar(f.g, x, by + HP_H + 2, PORTRAIT_W, MANA_H, manaFrac, Theme.MANA);
        barText(f, (int) hero.getMana() + " / " + (int) hero.getMaxMana(), x, by + HP_H + 2, PORTRAIT_W, MANA_H, manaFrac);
    }

    private static void renderUnitInfo(HudFrame f, LivingEntity e, int x, int y, int w, int h) {
        if (e instanceof Unit unit)
            renderStatRows(f, unit, x, y, w, h);
    }

    private static boolean isMe(String owner) {
        var me = net.minecraft.client.Minecraft.getInstance().player;
        return me != null && owner.equals(me.getName().getString());
    }

    /** The selected hero's unspent skill points, ending at the right edge given. Returns the width used. */
    public static int renderSkillPoints(HudFrame f, int right, int y) {
        LivingEntity e = HudClientEvents.hudSelectedEntity;
        if (HudClientEvents.hudSelectedPlacement != null || !(e instanceof HeroUnit hero) || hero.getSkillPoints() <= 0
                || UnitClientEvents.getPlayerToEntityRelationship(e) != Relationship.OWNED)
            return 0;
        int points = hero.getSkillPoints();
        String s = "+" + points;
        float scale = Text.px(15);
        int w = Text.bigWidth(s, scale) + 8;
        badge(f, s, right - w, y);
        f.register(new ButtonBuilder("skill points").build(), new Rect(right - w, y, w, Text.bigHeight(scale) + 3), null,
                () -> Tooltip.of(I18n.get("hud.reignofnether.skill_points", points)));
        return w;
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
     * A villager's way to a profession, then to veteran, 0 to 1: the profession it is learning (the one with the
     * most progress) until it has one, then its experience towards veteran.
     */
    private static float professionProgress(VillagerUnit v) {
        VillagerUnitProfession p = professionShown(v);
        if (p == VillagerUnitProfession.NONE)
            return 0;
        if (v.isVeteran())
            return 1;
        int need = v.getUnitProfession() == VillagerUnitProfession.NONE ? VillagerUnit.getExpReq(p) / 2 : VillagerUnit.getExpReq(p);
        return Math.min(v.getExp(p), need) / (float) need;
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

    private static void renderExperience(HudFrame f, HeroUnit hero, int x, int y, int w) {
        boolean max = hero.getHeroLevel() >= HeroUnit.MAX_LEVEL;
        float frac = max ? 1f : hero.getExpOnCurrentLevel() / (float) Math.max(1, hero.getExpToNextlevel());
        Theme.bar(f.g, x, y, w, XP_H, frac, Theme.XP);
        f.register(new ButtonBuilder("xp").build(), new Rect(x, y, w, XP_H), null, () -> max
                ? Tooltip.of(I18n.get("hud.hero.reignofnether.max_level"))
                : Tooltip.of(I18n.get("hud.hero.reignofnether.experience", hero.getExpOnCurrentLevel(), hero.getExpToNextlevel()))
                        .body(I18n.get("hud.hero.reignofnether.experience_warning")));
    }

    /** One row per stat, icon and value; the name and unit are in the tooltip, so nothing gets cut off. */
    private static void renderStatRows(HudFrame f, Unit unit, int x, int y, int w, int h) {
        List<Stat> stats = PortraitRendererUnit.computeStats(unit);
        int rowH = (h - (stats.size() - 1) * STAT_GAP) / Math.max(1, stats.size());
        for (int i = 0; i < stats.size(); i++) {
            Stat s = stats.get(i);
            int ry = y + i * (h - rowH) / Math.max(1, stats.size() - 1); // the last row ends at the bottom
            Rect row = new Rect(x, ry, w, rowH);
            Theme.card(f.g, x, ry, w, rowH);
            if (f.hovered(row))
                f.g.fill(x, ry, x + w, ry + rowH, Theme.HOVER);
            MyRenderer.renderIcon(f.g, s.icon(), x + 3, ry + (rowH - 10) / 2, 10);
            Text.draw(f.g, Text.fit(s.value(), w - 18), x + 16, ry + (rowH - 8) / 2 + 1, s.colour());
            String name = I18n.get("hud.reignofnether.stat." + s.type().name().toLowerCase());
            String statUnit = statUnit(s);
            f.register(new ButtonBuilder("stat").build(), row, null, () -> {
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

    // slots column: only for units with an inventory, then one column for what the units carry

    private static final String[] RESOURCE_NAMES = { "food", "wood", "ore", "emerald" };
    private static final int[] RESOURCE_COLOURS = { Theme.FOOD, Theme.WOOD, Theme.ORE, Theme.EMERALD };

    private static void renderSlots(HudFrame f, LivingEntity e, int x, int y) {
        if (e instanceof UnitInventory inv)
            renderInventory(f, inv, x, y);
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

    private static void effectTile(HudFrame f, LivingEntity e, Button icon, int x, int y) {
        icon.refresh();
        int size = EFFECT;
        Theme.outline(f.g, x, y, size, size, 1, Theme.INK);
        f.g.fill(x, y, x + size, y + size, Theme.RAISED);
        String frame = icon.frameResource != null ? icon.frameResource.getPath() : "";
        int ring = frame.contains("diamond") ? Theme.BUFF : frame.contains("red") ? Theme.ERROR : Theme.FAINT;
        Theme.ring(f.g, x, y, size, size, 1, ring);
        ButtonCell.drawIcon(f.g, icon, x + 3, y + 3, size - 6);
        // a villager's speciality does the opposite of a timed effect: its ring fills up as it levels
        if (e instanceof VillagerUnit v && icon instanceof ProfessionIcon)
            clockRing(f.g, x, y, size, professionProgress(v), Theme.GOLD);
        // timed effects: the ring goes dark clockwise from the top as the effect runs out
        if (icon instanceof MobEffectIcon mei && mei.duration >= MobEffectIcon.MIN_DURATION_SHOWN) {
            // whichever moved further: the client's copy of the effect, or real time since the icon appeared
            var live = e.getEffect(mei.effect);
            if (live == null || !live.isInfiniteDuration()) {
                float byLive = live != null ? 1f - live.getDuration() / (float) Math.max(mei.duration, live.getDuration()) : 0f;
                float byTime = (System.currentTimeMillis() - mei.createdMs) / (mei.duration * 50f);
                clockRing(f.g, x, y, size, Math.max(byLive, byTime), Theme.INK); // Theme.ring draws on the tile's outer edge
            }
        }
        f.register(icon, new Rect(x, y, size, size), null, null);
    }

    /** Paints the first `elapsed` fraction of a 1-wide square ring of side s, clockwise from its top middle. */
    private static void clockRing(GuiGraphics g, int x, int y, int s, float elapsed, int colour) {
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
            g.fill(x + px, y + py, x + px + 1, y + py + 1, colour);
        }
    }

    private static void renderInventory(HudFrame f, UnitInventory inv, int x, int y) {
        List<Button> slots = ItemClientEvents.inventoryButtons(inv);
        for (int i = 0; i < slots.size(); i++) {
            Button b = slots.get(i);
            int sx = x + (i % 2) * (SLOT + SLOT_GAP);
            int sy = y + (i / 2) * (SLOT + SLOT_GAP);
            Keybinding key = i < ItemClientEvents.hotkeys.size() ? ItemClientEvents.hotkeys.get(i) : null;
            if (b instanceof AbstractUnitItemButton item && item.unitItem instanceof EmptyUnitItem) {
                ButtonCell.renderEmpty(f.g, sx, sy, SLOT, null);
                f.register(b, new Rect(sx, sy, SLOT, SLOT), null, null);
            } else {
                f.cell(b, sx, sy, SLOT, b.hotkey != null ? key : null);
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

    /** A vertical bar of the limit the units share: each resource fills its share from the bottom, with its numbers inside. */
    private static void renderGathering(HudFrame f, LivingEntity e, List<LivingEntity> units, int x, int by, int bh) {
        boolean animal = !(e instanceof Unit);
        int[] values = new int[4];
        int max = 0;
        for (LivingEntity unit : units) {
            Resources res = carried(unit);
            if (res != null) {
                values[0] += res.food;
                values[1] += res.wood;
                values[2] += res.ore;
                values[3] += res.emerald;
            }
            if (unit instanceof Unit u && unit instanceof Mob mob && mob.canPickUpLoot())
                max += u.getMaxResources();
        }
        int total = values[0] + values[1] + values[2] + values[3];
        if (max == 0)
            max = e instanceof Unit u ? Math.max(1, u.getMaxResources()) : Math.max(1, total);
        boolean full = !animal && total >= max;
        String hint = full ? I18n.get("hud.reignofnether.carry_full") : "";
        int bottom = by + bh;
        f.g.fill(x, by, x + GATHER_W, bottom, Theme.INK);
        int held = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0)
                continue;
            held += values[i];
            int top = by + bh - bh * Math.min(held, max) / max;
            if (top < bottom)
                renderResourceSegment(f, i, values[i], 100 * values[i] / max, hint, new Rect(x, top, GATHER_W, bottom - top));
            bottom = top;
        }
    }

    /** Lit like the other bars; its icon and amount only show when they fit, the tooltip always has them. */
    private static void renderResourceSegment(HudFrame f, int resource, int value, int percent, String hint, Rect r) {
        f.g.fill(r.x(), r.y(), r.right(), r.bottom(), RESOURCE_COLOURS[resource]);
        f.g.fill(r.x(), r.y(), r.right(), r.y() + 1, 0x59FFFFFF);
        f.g.fill(r.x(), r.y(), r.x() + 1, r.bottom(), 0x33FFFFFF);
        f.g.fill(r.right() - 1, r.y(), r.right(), r.bottom(), 0x4D000000);
        f.g.fill(r.x(), r.bottom() - 1, r.right(), r.bottom(), 0x40000000);
        if (f.hovered(r))
            f.g.fill(r.x(), r.y(), r.right(), r.bottom(), Theme.HOVER);
        if (r.h() >= 10) {
            String text = String.valueOf(value);
            int ix = r.x() + (r.w() - 10 - Text.smallWidth(text)) / 2;
            MyRenderer.renderIcon(f.g, RESOURCE_ICONS[resource], ix, r.y() + (r.h() - 8) / 2, 8);
            Text.small(f.g, text, ix + 10, r.y() + Math.floorDiv(r.h() - 9, 2), Theme.ON_ORANGE);
        }
        String name = I18n.get("resources.reignofnether." + RESOURCE_NAMES[resource]);
        String status = value + " · " + percent + "%" + (hint.isEmpty() ? "" : " · " + hint);
        f.register(new ButtonBuilder(name).build(), r, null, () -> Tooltip.of(name).status(status, Theme.LABEL));
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
        // small screens drop the gathering column for groups: what the whole selection carries goes here instead
        Resources held = new Resources("", 0, 0, 0, 0);
        if (f.layout.compact())
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
        int hidden = army.size();
        for (Map.Entry<String, List<LivingEntity>> type : types.entrySet()) {
            int sx = r.x() + (i % perRow) * (STACK_W + GAP);
            int stackY = sy + (i / perRow) * (STACK_H + 5);
            if (i == maxStacks - 1 && types.size() > maxStacks) {
                renderMoreStack(f, hidden, sx, stackY);
                break;
            }
            hidden -= type.getValue().size();
            renderStack(f, type.getValue(), type.getKey().equals(active), sx, stackY);
            i++;
        }
        int usedRows = (Math.min(types.size(), maxStacks) + perRow - 1) / perRow;
        int ty = sy + usedRows * (STACK_H + 5) + 1;
        List<LivingEntity> activeUnits = types.getOrDefault(active, List.of());
        // the portrait already shows the hero
        if (r.bottom() - ty >= TILE && !(HudClientEvents.hudSelectedEntity instanceof HeroUnit))
            renderUnitTiles(f, activeUnits, r.x(), ty, r.w(), r.bottom() - ty);
    }

    /** Takes the last stack's place when there are more types than fit. */
    private static void renderMoreStack(HudFrame f, int hidden, int x, int y) {
        Rect r = new Rect(x, y, STACK_W, STACK_H);
        Theme.cell(f.g, x, y, STACK_W, STACK_H, Theme.CellState.READY, f.hovered(r));
        Text.bigCentered(f.g, "+" + hidden, r.centerX(), y + (STACK_H - Text.bigHeight(Text.px(20))) / 2, Theme.TEXT, Text.px(20));
        f.register(new ButtonBuilder("more units").build(), r, null, () -> Tooltip.of(I18n.get("hud.reignofnether.more_units", hidden)));
    }

    private static void renderStack(HudFrame f, List<LivingEntity> units, boolean active, int x, int y) {
        LivingEntity first = units.get(0);
        Rect r = new Rect(x, y, STACK_W, STACK_H);
        Theme.cell(f.g, x, y, STACK_W, STACK_H, active ? Theme.CellState.ACTIVE : Theme.CellState.READY, f.hovered(r));
        MyRenderer.renderIcon(f.g, headIcon(first), x + 4, y + 4, 17);
        String count = "×" + units.size();
        Text.draw(f.g, count, x + STACK_W - 4 - Text.width(count), y + 5, active ? Theme.GOLD : Theme.TEXT);
        float hp = 0;
        for (LivingEntity e : units)
            hp += e.getHealth() / Math.max(1, e.getMaxHealth());
        hp /= units.size();
        Theme.bar(f.g, x + 24, y + 15, STACK_W - 28, 3, hp, Theme.hpColor(hp));
        if (first instanceof HeroUnit hero)
            renderHeroStatus(f, hero, x + 24, y + 19, STACK_W - 28);

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

    /** What matters about a hero while the army has the command card: its mana, and a mark per spell. */
    private static void renderHeroStatus(HudFrame f, HeroUnit hero, int x, int y, int w) {
        Theme.bar(f.g, x, y, w, 2, hero.getMana() / Math.max(1f, hero.getMaxMana()), Theme.MANA);
        int px = x;
        for (Button b : hero.getAbilityButtons()) {
            if (!(b instanceof AbilityButton ab && ab.ability instanceof HeroAbility ha))
                continue;
            boolean learnt = ha.getRank(hero) > 0;
            int colour = !learnt ? Theme.FAINT : ab.getCooldownTicks() > 0 ? Theme.DIM : hero.getMana() < ha.manaCost ? Theme.ERROR : Theme.GOLD;
            f.g.fill(px, y + 4, px + 4, y + 6, colour);
            px += 6;
        }
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

    private static void renderBuildingPortrait(HudFrame f, BuildingPlacement b, int x, int y, int h) {
        int portraitH = h - HP_H - 3 - (b.isBuilt ? 0 : MANA_H + 2);
        portraitFrame(f, x, y, PORTRAIT_W, portraitH, b.ownerName);
        Clip.push(f.g, x, y, PORTRAIT_W, portraitH);
        PortraitRendererBuilding.drawBlock(f.g, b.getBuilding().portraitBlock, x + PORTRAIT_W / 2f, y + portraitH / 2f - 6, 11f);
        Clip.pop(f.g);
        portraitRect = new Rect(x, y, PORTRAIT_W, portraitH);
        f.zone(portraitRect);
        int by = y + portraitH + 3;
        healthBar(f, x, by, PORTRAIT_W, HP_H, b.getHealth(), b.getMaxHealth());
        if (b.isBuilt)
            return;
        float built = (float) b.getBlocksPlacedPercent();
        Theme.bar(f.g, x, by + HP_H + 2, PORTRAIT_W, MANA_H, built, Theme.ORANGE);
        barText(f, I18n.get("hud.reignofnether.construction_progress", (int) (built * 100)), x, by + HP_H + 2, PORTRAIT_W, MANA_H, built);
    }

    private static boolean showsRally(BuildingPlacement b) {
        return b instanceof ProductionPlacement && b.isBuilt && BuildingClientEvents.getPlayerToBuildingRelationship(b) == Relationship.OWNED;
    }

    private static boolean showsRange(BuildingPlacement b) {
        return b instanceof SculkCatalystPlacement && b.isBuilt && b.getBuilding().hasActiveAddon(NightSourceAddon.class);
    }

    private static void renderBuildingInfo(HudFrame f, BuildingPlacement b, int x, int y, int w) {
        if (showsRange(b)) {
            Text.small(f.g, Text.fitSmall(I18n.get("hud.buildings.reignofnether.sculk_catalyst.range",
                    b.getBuilding().getActiveAddon(NightSourceAddon.class).getNightRange(b), SculkCatalyst.MAX_NIGHT_RANGE), w), x, y, Theme.ORANGE);
            y += 10;
        }
        if (b instanceof ProductionPlacement prod && showsRally(b)) {
            boolean set = !prod.getRallyPoints().isEmpty() || prod.getRallyPointEntity() != null;
            Text.small(f.g, I18n.get(set ? "hud.reignofnether.rally_set" : "hud.reignofnether.no_rally"), x, y, set ? Theme.OK : Theme.DIM);
        }
    }

    private static void renderTraining(HudFrame f, ProductionPlacement prod, boolean owned, int x, int y, int w, int h) {
        int px = x;
        int pw = w;
        List<ActiveProduction> queue = prod.productionQueue;
        Text.small(f.g, I18n.get("hud.reignofnether.training"), px, y, Theme.LABEL);
        Text.smallRight(f.g, I18n.get("hud.reignofnether.queue_count", queue.size()), px + pw, y, Theme.FAINT);
        if (queue.isEmpty()) {
            Text.draw(f.g, Text.fit(I18n.get("hud.reignofnether.nothing_in_production"), pw), px, y + 13, Theme.LABEL);
            return;
        }

        // top: the unit in production, its name, time left and a bar across the whole card
        ActiveProduction current = queue.get(0);
        int cs = 24;
        int cy = y + 10;
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
        int avail = y + h - gy;
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
