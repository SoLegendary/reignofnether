package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.config.ReignOfNetherClientConfigs;

import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.hud.ControlGroup;
import com.solegendary.reignofnether.hud.GlobalProductionQueue;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.items.ItemClientEvents;
import com.solegendary.reignofnether.items.RandomItemDropRule;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.IncomeTracker;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Top-left of the HUD: resource bar, then control groups and the global production queue on one row below it. */
public final class TopLeftPanel {

    private TopLeftPanel() { }

    private static final int CELL_H = HudLayout.TOP_CARD_H;
    private static final int CELL_MIN_W = 58;
    private static final int TILE = HudLayout.CELL;
    private static final int TILE_GAP = 5;
    private static final float VALUE = Text.px(20); // medium, like the spectator scoreboard
    private static final ResourceLocation WORKER_ICON =
            ResourceLocation.fromNamespaceAndPath(com.solegendary.reignofnether.ReignOfNether.MOD_ID, "textures/mobheads/villager.png");

    public static void render(HudFrame f) {
        int x = HudLayout.MARGIN;
        int y = HudLayout.MARGIN;

        String viewed = viewedPlayer();
        // sandbox players have the NONE faction too, but keep their bar for testing costs
        Resources resources = viewed != null && (PlayerClientEvents.getPlayerFaction(viewed) != Factions.NONE
                || SandboxClientEvents.isSandboxPlayer(viewed))
                ? ResourcesClientEvents.getResources(viewed) : null;
        String self = selfName();

        // an eye when looking at someone else's economy or spectating; the explanation is on hover
        if (viewed != null && !viewed.equals(self) && resources != null) {
            eyeBanner(f, x, y, viewed, I18n.get("hud.reignofnether.players_resources", viewed));
            y += 14;
        } else if (resources == null && !PlayerClientEvents.isRTSPlayer() && !TutorialClientEvents.isEnabled()) {
            eyeBanner(f, x, y, "", I18n.get("hud.reignofnether.you_are_spectator"));
            y += 14;
        }

        int rowY = y;
        if (resources != null) {
            Rect bar = renderResources(f, x, y, viewed, resources);
            rowY = bar.bottom() + HudLayout.GAP;
        }
        renderGroupsAndQueue(f, x, rowY, viewed);
    }

    private static void eyeBanner(HudFrame f, int x, int y, String name, String tooltip) {
        Icon.EYE.draw(f.g, x, y, 12, Theme.GOLD);
        if (!name.isEmpty())
            Text.draw(f.g, name, x + 15, y + 2, Theme.GOLD);
        Rect r = new Rect(x, y, 15 + (name.isEmpty() ? 0 : Text.width(name)), 12);
        f.register(new ButtonBuilder("viewing").build(), r, null, () -> Tooltip.of(tooltip));
    }

    /**
     * The player whose economy is shown: the owner of the selection when it's you or an ally (or you
     * are spectating), otherwise yourself. Selecting an enemy no longer hides your own resources.
     */
    @Nullable
    private static String viewedPlayer() {
        String self = selfName();
        String sel = null;
        if (!UnitClientEvents.getSelectedUnits().isEmpty() && UnitClientEvents.getSelectedUnits().get(0) instanceof Unit u)
            sel = u.getOwnerName();
        if (!BuildingClientEvents.getSelectedBuildings().isEmpty())
            sel = BuildingClientEvents.getSelectedBuildings().get(0).ownerName;

        boolean canSee = sel != null && (sel.equals(self) || AlliancesClient.isAllied(self, sel)
                || !PlayerClientEvents.isRTSPlayer()
                || (SandboxClientEvents.isSandboxPlayer() && PlayerClientEvents.isRTSPlayer(sel)));
        if (canSee)
            return sel;
        return PlayerClientEvents.isRTSPlayer() ? self : sel;
    }

    private static String selfName() {
        var player = Minecraft.getInstance().player;
        return player != null ? player.getName().getString() : "";
    }

    // ------------------------------------------------------------------ resources

    private record Cell(ResourceLocation icon, String value, String sub, int subColour, @Nullable String workers,
                        boolean alarm, Supplier<Tooltip> tooltip) { }

    static Rect renderResources(HudFrame f, int x, int y, String owner, Resources res) {
        return renderResources(f, x, y, owner, res, true, VALUE, null);
    }

    /** Each cell's width in an owner's compact (scoreboard) resource bar, to lay it out before drawing. */
    static int[] resourceWidths(String owner, Resources res, float valueScale) {
        return widths(cells(owner, res, true), valueScale);
    }

    /** @param compact the scoreboard's narrower cells: the emerald and population sub-lines go in their tooltips */
    private static List<Cell> cells(String owner, Resources res, boolean compact) {
        List<Cell> cells = new ArrayList<>();
        RTSPlayer rtsPlayer = PlayerClientEvents.getPlayer(owner);
        int[] workers = countWorkers(owner);
        cells.add(resourceCell(owner, ResourceName.FOOD, ButtonCell.ICON_FOOD, res.food, workers[0]));
        cells.add(resourceCell(owner, ResourceName.WOOD, ButtonCell.ICON_WOOD, res.wood, workers[1]));
        cells.add(resourceCell(owner, ResourceName.ORE, ButtonCell.ICON_ORE, res.ore, workers[2]));
        if (ItemClientEvents.ENABLED) {
            int found = rtsPlayer != null ? rtsPlayer.itemsDropped : 0;
            String foundText = I18n.get("hud.reignofnether.items_found_short", found);
            cells.add(new Cell(ButtonCell.ICON_EMERALD, String.format(java.util.Locale.US, "%,d", res.emerald),
                    compact ? "" : foundText, Theme.MUTED, null, false, () -> {
                Tooltip t = Tooltip.of(I18n.get("resources.reignofnether.emerald")).tag(I18n.get("hud.reignofnether.tag.resource"), Theme.EMERALD);
                if (GameruleClient.randomItemDrops != RandomItemDropRule.DISABLED)
                    t.body(I18n.get("hud.reignofnether.items_hint")).body(I18n.get("hud.reignofnether.items_found_desc"));
                if (compact)
                    t.status(foundText, Theme.MUTED);
                return t;
            }));
        }
        cells.add(populationCell(owner, workers[3], compact));
        return cells;
    }

    private static int[] widths(List<Cell> cells, float valueScale) {
        int[] widths = new int[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            Cell c = cells.get(i);
            int textW = Math.max(Text.bigWidth(c.value, valueScale), Text.smallWidth(c.sub) + (c.workers != null ? Text.smallWidth(c.workers) + 14 : 0));
            widths[i] = Math.max(CELL_MIN_W, 7 + 15 + 6 + textW + 8);
        }
        return widths;
    }

    /**
     * @param framed     false inside another panel (the spectator scoreboard): the cells without their own card
     * @param valueScale size of the values: big on the player's own bar, medium on the scoreboard
     * @param colWidths  cell widths to use, so several bars line up in columns; null to fit this bar's values
     */
    static Rect renderResources(HudFrame f, int x, int y, String owner, Resources res, boolean framed,
                                float valueScale, @Nullable int[] colWidths) {
        List<Cell> cells = cells(owner, res, !framed);
        int[] widths = colWidths != null && colWidths.length == cells.size() ? colWidths : widths(cells, valueScale);
        int total = 0;
        for (int w : widths)
            total += w;
        Rect bar = new Rect(x, y, total, CELL_H);
        if (framed)
            Theme.panel(f.g, x, y, total, CELL_H, Theme.floating());
        f.zone(bar);

        int cx = x;
        for (int i = 0; i < cells.size(); i++) {
            Cell c = cells.get(i);
            int w = widths[i];
            Rect r = new Rect(cx, y, w, CELL_H);
            if (c.alarm)
                f.g.fill(cx, y, cx + w, y + CELL_H, Theme.ALARM_TINT);
            if (f.hovered(r))
                f.g.fill(cx, y, cx + w, y + CELL_H, Theme.HOVER);
            MyRenderer.renderIcon(f.g, c.icon, cx + 7, y + 8, 15);
            int tx = cx + 7 + 15 + 6;
            // the value's top, so a big or a medium value both end just above the sub-line; centred when it has none
            int vy = !c.sub.isEmpty() || c.workers != null ? y + 16 - Text.bigHeight(valueScale) : y + (CELL_H - Text.bigHeight(valueScale)) / 2;
            Text.big(f.g, c.value, tx, vy, c.alarm ? Theme.ERROR : Theme.TEXT, valueScale);
            Text.small(f.g, c.sub, tx, y + 19, c.alarm ? Theme.ERROR : c.subColour);
            if (c.workers != null) {
                int wx = tx + Text.smallWidth(c.sub) + 5;
                MyRenderer.renderIcon(f.g, workerIcon(owner), wx, y + 21, 6);
                Text.small(f.g, c.workers, wx + 8, y + 19, Theme.MUTED);
            }
            if (i < cells.size() - 1)
                Theme.divider(f.g, cx + w - 1, y + 1, 1, CELL_H - 2);
            if (f.hovered(r))
                c.tooltip.get().show(r.x(), r.y(), r.w(), r.h());
            cx += w;
        }
        return bar;
    }

    private static ResourceLocation workerIcon(String owner) {
        RTSPlayer player = PlayerClientEvents.getPlayer(owner);
        return player != null && player.faction != null ? player.faction.idleWorkerIcon : WORKER_ICON;
    }

    // every player's drop-offs reach every client, so the income shows for anyone whose bar is visible
    private static Cell resourceCell(String owner, ResourceName name, ResourceLocation icon, int value, int workers) {
        String lower = name.name().toLowerCase();
        int rate = IncomeTracker.perMinute(owner, name);
        // "+200/-330": gathered/spent per minute
        String sub = I18n.get("hud.reignofnether.gain_spend_short", rate, IncomeTracker.spentPerMinute(owner, name));
        return new Cell(icon, String.format(java.util.Locale.US, "%,d", value), sub, Theme.MUTED, String.valueOf(workers), false, () ->
                Tooltip.of(I18n.get("resources.reignofnether." + lower))
                        .tag(I18n.get("hud.reignofnether.tag.resource"), Theme.ORANGE)
                        .body(I18n.get("hud.reignofnether.resource_desc." + lower))
                        .status(I18n.get("hud.reignofnether.gain_per_minute", IncomeTracker.perMinute(owner, name)), Theme.OK)
                        .status(I18n.get("hud.reignofnether.spend_per_minute", IncomeTracker.spentPerMinute(owner, name)), Theme.ERROR)
                        .status(I18n.get("hud.reignofnether.workers_on", workers), Theme.MUTED));
    }

    private static Cell populationCell(String owner, int workers, boolean compact) {
        int pop = UnitClientEvents.getCurrentPopulation(owner);
        int supply = BuildingClientEvents.getTotalPopulationSupply(owner);
        int max = GameruleClient.maxPopulation;
        // blocked by housing only: 0/0 has nothing to house, and at the max population more housing wouldn't help
        boolean full = pop > 0 && pop >= supply && supply < max;
        String sub = full ? I18n.get("hud.reignofnether.housing_full") : I18n.get("hud.reignofnether.max_short", max);
        return new Cell(PlayerColors.getPlayerColorBedIcon(owner), pop + "/" + supply, compact ? "" : sub, Theme.MUTED,
                String.valueOf(workers), full, () -> {
            Tooltip t = Tooltip.of(I18n.get("resources.reignofnether.population"))
                    .tag(I18n.get("hud.reignofnether.tag.resource"), Theme.ORANGE)
                    .body(I18n.get(populationDescKey(owner)));
            if (compact)
                t.status(sub, full ? Theme.ERROR : Theme.MUTED);
            if (full)
                t.status(I18n.get("hud.reignofnether.housing_full_hint"), Theme.ERROR);
            return t;
        });
    }

    private static String populationDescKey(String owner) {
        Faction faction = PlayerClientEvents.getPlayerFaction(owner);
        String key = "hud.reignofnether.resource_desc.population";
        if (faction == Factions.VILLAGERS)
            return key + ".villagers";
        if (faction == Factions.MONSTERS)
            return key + ".monsters";
        if (faction == Factions.PIGLINS)
            return key + ".piglins";
        return key;
    }

    /**
     * One pass over the owner's workers: {food, wood, ore, total}. A worker counts towards a resource when
     * gathering or carrying it (unless idle); hunters count towards food.
     */
    static int[] countWorkers(String owner) {
        long tick = System.currentTimeMillis() / 50; // counted once a tick: every bar and scoreboard row asks each frame
        if (tick != workerCountTick) {
            workerCounts.clear();
            workerCountTick = tick;
        }
        return workerCounts.computeIfAbsent(owner, TopLeftPanel::scanWorkers);
    }

    private static final java.util.Map<String, int[]> workerCounts = new java.util.HashMap<>();
    private static long workerCountTick = -1;

    private static int[] scanWorkers(String owner) {
        int[] counts = new int[4];
        for (LivingEntity le : UnitClientEvents.getAllUnits()) {
            if (!(le instanceof Unit u) || !(le instanceof WorkerUnit wu) || !u.getOwnerName().equals(owner))
                continue;
            counts[3]++;
            boolean hunting = ResourceSources.isHuntableAnimal(u.getTargetGoal().getTarget());
            if (hunting)
                counts[0]++;
            if (UnitClientEvents.idleWorkerIds.contains(le.getId()))
                continue;
            boolean returning = u.getReturnResourcesGoal() != null;
            Resources carried = returning ? Resources.getTotalResourcesFromItems(u.getItems()) : null;
            ResourceName target = wu.getGatherResourceGoal().getTargetResourceName();
            if (!hunting && (target == ResourceName.FOOD || (returning && carried.food > 0)))
                counts[0]++;
            if (target == ResourceName.WOOD || (returning && carried.wood > 0))
                counts[1]++;
            if (target == ResourceName.ORE || (returning && carried.ore > 0))
                counts[2]++;
        }
        return counts;
    }

    // ------------------------------------------------------------------ control groups, then the queue below

    private static void renderGroupsAndQueue(HudFrame f, int x, int y, @Nullable String owner) {
        int cx = x;
        for (ControlGroup group : HudClientEvents.controlGroups) {
            group.clean();
            if (group.isEmpty())
                continue;
            renderGroup(f, group, cx, y);
            cx += TILE + TILE_GAP;
        }

        if (owner == null || !canSeeQueue(owner))
            return;
        List<GlobalProductionQueue.Group> queue = GlobalProductionQueue.forPlayer(owner);
        if (queue.isEmpty())
            return;

        // the queue continues the groups' row, after a divider and a vertical label, like the design; when it
        // doesn't fit before the middle of the screen, or the player wants it apart, it takes its own row(s) under
        // the groups instead
        int limit = f.layout.width() / 2;
        int step = TILE + TILE_GAP;
        int labelW = 7 + TILE_GAP;
        int divider = cx > x ? 1 + TILE_GAP : 0;
        if (cx > x && (ReignOfNetherClientConfigs.QUEUE_OWN_ROW.get() || cx + divider + labelW + queue.size() * step - TILE_GAP > limit)) {
            cx = x;
            y += step;
            divider = 0;
        }
        if (divider > 0) {
            Theme.divider(f.g, cx, y, 1, TILE);
            cx += divider;
        }
        String label = I18n.get("hud.reignofnether.queue");
        f.g.pose().pushPose();
        f.g.pose().translate(cx, y + (TILE + Text.smallWidth(label)) / 2f, 0);
        f.g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90));
        Text.smallShadowed(f.g, label, 0, -1, Theme.GOLD);
        f.g.pose().popPose();
        int qx = cx + labelW;
        int perRow = Math.max(1, (limit - qx + TILE_GAP) / step);
        boolean own = owner.equals(selfName());
        for (int i = 0; i < queue.size(); i++)
            renderQueueTile(f, queue.get(i), qx + (i % perRow) * step, y + (i / perRow) * step, own);
    }

    private static boolean canSeeQueue(String owner) {
        return owner.equals(selfName()) || !PlayerClientEvents.isRTSPlayer() || SandboxClientEvents.isSandboxPlayer()
                || AlliancesClient.isAllied(selfName(), owner);
    }

    private static void renderGroup(HudFrame f, ControlGroup group, int x, int y) {
        Button b = group.getButton();
        Rect r = new Rect(x, y, TILE, TILE);
        boolean selected = isGroupSelected(group);
        Theme.cell(f.g, x, y, TILE, TILE, selected ? Theme.CellState.ACTIVE : Theme.CellState.READY, f.hovered(r));
        if (group.getIcon() != null)
            MyRenderer.renderIcon(f.g, group.getIcon(), x + 7, y + 6, 14);

        List<LivingEntity> members = groupEntities(group);
        int count = members.isEmpty() ? group.buildingBps.size() : members.size();
        float hp = averageHealth(group, members);
        String label = group.getKeybinding() != null ? group.getKeybinding().getCurrentLabel() : "";
        Theme.keyCap(f.g, label, x + 1, y + 1, selected ? Theme.GOLD : Theme.MUTED);
        String n = String.valueOf(count);
        Text.smallShadowed(f.g, n, x + TILE - Text.smallWidth(n) - 2, y + TILE - 13, Theme.TEXT);
        Theme.bar(f.g, x + 3, y + TILE - 4, TILE - 6, 2, hp, Theme.hpColor(hp));

        f.register(b, r, group.getKeybinding(), () -> Tooltip.of(I18n.get("hud.control_group.reignofnether.title", label))
                .body(composition(members))
                .status(I18n.get("hud.control_group.reignofnether.keys", label), Theme.LABEL)
                .status(I18n.get("hud.control_group.reignofnether.mouse"), Theme.LABEL));
    }

    private static boolean isGroupSelected(ControlGroup group) {
        var selected = UnitClientEvents.getSelectedUnits();
        if (!group.entityIds.isEmpty()) {
            if (selected.size() != group.entityIds.size())
                return false;
            Set<Integer> ids = new HashSet<>(group.entityIds);
            return selected.stream().allMatch(e -> ids.contains(e.getId()));
        }
        var buildings = BuildingClientEvents.getSelectedBuildings();
        return !group.buildingBps.isEmpty() && buildings.size() == group.buildingBps.size()
                && buildings.stream().allMatch(b -> group.buildingBps.contains(b.originPos));
    }

    private static List<LivingEntity> groupEntities(ControlGroup group) {
        List<LivingEntity> list = new ArrayList<>();
        Set<Integer> ids = new HashSet<>(group.entityIds);
        for (LivingEntity e : UnitClientEvents.getAllUnits())
            if (ids.contains(e.getId()))
                list.add(e);
        return list;
    }

    private static float averageHealth(ControlGroup group, List<LivingEntity> members) {
        float sum = 0;
        int n = 0;
        for (LivingEntity e : members) {
            sum += e.getHealth() / Math.max(1, e.getMaxHealth());
            n++;
        }
        if (n == 0)
            for (BuildingPlacement b : BuildingClientEvents.getBuildings())
                if (group.buildingBps.contains(b.originPos)) {
                    sum += b.getHealth() / (float) Math.max(1, b.getMaxHealth());
                    n++;
                }
        return n == 0 ? 1f : sum / n;
    }

    private static String composition(List<LivingEntity> members) {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (LivingEntity e : members)
            counts.merge(SelectionPanel.displayName(e), 1, Integer::sum);
        StringBuilder sb = new StringBuilder();
        counts.forEach((name, n) -> sb.append(sb.isEmpty() ? "" : ", ").append(n).append(" ").append(name));
        return sb.toString();
    }

    private static void renderQueueTile(HudFrame f, GlobalProductionQueue.Group group, int x, int y, boolean own) {
        Rect r = new Rect(x, y, TILE, TILE);
        Theme.cell(f.g, x, y, TILE, TILE, Theme.CellState.READY, f.hovered(r));

        Button cancel = group.item.getCancelButton(group.soonestPlacement, group.inProgress);
        if (cancel.iconResource != null)
            MyRenderer.renderIcon(f.g, cancel.iconResource, x + 7, y + 3, 14);
        if (group.count > 1) {
            String n = "×" + group.count;
            Text.smallShadowed(f.g, n, x + TILE - Text.smallWidth(n) - 1, y - 1, Theme.TEXT);
        }
        boolean blocked = group.isPopBlocked();
        String time = blocked ? I18n.get("hud.reignofnether.pop_short") : group.inProgress ? TimeUtils.formatSeconds(group.secondsLeft()) : "";
        Text.smallShadowed(f.g, time, x + (TILE - Text.smallWidth(time)) / 2, y + TILE - 12, blocked ? Theme.ERROR : Theme.BODY);
        Theme.bar(f.g, x + 3, y + TILE - 3, TILE - 6, 2, group.progress(), blocked ? Theme.ERROR : Theme.ORANGE);

        Runnable cancelOne = cancel.onLeftClick;
        Button b = new ButtonBuilder(cancel.name)
                .onLeftClick(() -> selectNextProducer(group))
                .onRightClick(own ? cancelOne : null)
                .build();
        f.register(b, r, null, () -> {
            Tooltip t = Tooltip.of(cancel.name + (group.count > 1 ? " ×" + group.count : ""))
                    .tag(I18n.get("hud.reignofnether.tag.in_production"), Theme.ORANGE);
            if (blocked)
                t.status(I18n.get("hud.reignofnether.paused_population"), Theme.ERROR);
            else if (group.inProgress)
                t.status(I18n.get("hud.reignofnether.next_ready_in", TimeUtils.formatSeconds(group.secondsLeft())), Theme.MUTED);
            t.status(I18n.get(own ? "hud.reignofnether.queue_hint" : "hud.reignofnether.queue_hint_view"), Theme.LABEL);
            return t;
        });
    }

    private static void selectNextProducer(GlobalProductionQueue.Group group) {
        if (group.placements.isEmpty())
            return;
        ProductionPlacement next = group.placements.get(0);
        if (HudClientEvents.hudSelectedPlacement instanceof ProductionPlacement current && group.placements.contains(current))
            next = group.placements.get((group.placements.indexOf(current) + 1) % group.placements.size());
        BuildingClientEvents.clearSelectedBuildings();
        UnitClientEvents.clearSelectedUnits();
        BuildingClientEvents.addSelectedBuilding(next);
        OrthoviewClientEvents.centreCameraOnPos(next.centrePos);
    }
}
