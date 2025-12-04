package com.solegendary.reignofnether.hud;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.*;

import static com.solegendary.reignofnether.hud.HudClientEvents.hudSelectedPlacement;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class GlobalProductionQueueRenderer {

    private static final Minecraft MC = Minecraft.getInstance();

    private static class ProductionQueueGroup {
        final ProductionItem item;
        final List<ProductionPlacement> placements = new ArrayList<>();
        ActiveProduction activeProd;
        ProductionPlacement placement;
        int count;
        boolean includesFront;

        ProductionQueueGroup(ProductionItem item) { this.item = item; }

        void add(ActiveProduction p, int idx, ProductionPlacement plac) {
            count++;
            if (!placements.contains(plac)) placements.add(plac);
            if (idx == 0) includesFront = true;
            if (activeProd == null || p.ticksLeft < activeProd.ticksLeft) { activeProd = p; placement = plac; }
        }

        float getProgress() {
            if (activeProd == null || placement == null) return 0f;
            float total = activeProd.item.getCost(true, placement.ownerName).ticks;
            return total <= 0 ? 0f : activeProd.ticksLeft / total;
        }
    }

    private static void aggregateQueue(ProductionPlacement placement, Map<ProductionItem, ProductionQueueGroup> grouped, List<ProductionQueueGroup> ordered) {
        for (int i = 0; i < placement.productionQueue.size(); i++) {
            ActiveProduction production = placement.productionQueue.get(i);
            ProductionQueueGroup group = grouped.computeIfAbsent(production.item, k -> {
                ProductionQueueGroup g = new ProductionQueueGroup(k);
                ordered.add(g);
                return g;
            });
            group.add(production, i, placement);
        }
    }

    private static List<ProductionQueueGroup> groupPlayerProductionQueues(String playerName) {
        if (playerName == null) return Collections.emptyList();
        LinkedHashMap<ProductionItem, ProductionQueueGroup> grouped = new LinkedHashMap<>();
        ArrayList<ProductionQueueGroup> ordered = new ArrayList<>();
        for (BuildingPlacement b : BuildingClientEvents.getBuildings()) {
            if (b instanceof ProductionPlacement p && playerName.equals(p.ownerName))
                aggregateQueue(p, grouped, ordered);
        }
        return ordered;
    }

    private static void renderQueueGroupCount(GuiGraphics guiGraphics, int x, int y, int iconFrameSize, int count) {
        if (count <= 1) return;
        String countText = String.valueOf(count);
        int textWidth = MC.font.width(countText);
        guiGraphics.drawString(MC.font, countText, x + iconFrameSize - textWidth - 3, y + iconFrameSize - 10, 0xFFFFFF);
    }

    private static Button createQueueButton(ProductionQueueGroup group) {
        Button btn = group.item.getCancelButton(group.placement, group.includesFront);
        btn.onLeftClick = () -> {
            if (group.placements.isEmpty()) return;
            ProductionPlacement next = group.placements.get(0);
            if (hudSelectedPlacement instanceof ProductionPlacement && group.placements.contains(hudSelectedPlacement)) {
                next = group.placements.get((group.placements.indexOf(hudSelectedPlacement) + 1) % group.placements.size());
            }
            BuildingClientEvents.clearSelectedBuildings();
            UnitClientEvents.clearSelectedUnits();
            BuildingClientEvents.addSelectedBuilding(next);
            OrthoviewClientEvents.centreCameraOnPos(next.centrePos);
        };
        float frac = Mth.clamp(group.getProgress(), 0f, 1f);
        btn.greyPercent = 1 - (group.includesFront ? frac : 0.99f);

        List<FormattedCharSequence> tooltip = new ArrayList<>();
        tooltip.add(fcs(btn.name));
        int progressPercent = Math.round((1.0f - frac) * 100f);
        if (progressPercent > 0)
            tooltip.add(fcs(progressPercent + "%"));
        btn.tooltipLines = tooltip;
        return btn;
    }

    public static Pair<List<RectZone>, List<Button>> renderQueue(GuiGraphics guiGraphics, String playerName, int baseX, int baseY, int mouseX, int mouseY) {
        List<ProductionQueueGroup> groupedQueue = groupPlayerProductionQueues(playerName);

        List<RectZone> hudZones = new ArrayList<>();
        List<Button> renderedButtons = new ArrayList<>();

        if (groupedQueue.isEmpty())
            return new Pair<>(hudZones, renderedButtons);

        int iconFrameSize = Button.DEFAULT_ICON_FRAME_SIZE;
        int iconsPerRow = 5;
        int rows = (int) Math.ceil((double) groupedQueue.size() / iconsPerRow);

        hudZones.add(MyRenderer.renderFrameWithBg(guiGraphics, baseX, baseY,
                iconFrameSize * Math.min(groupedQueue.size(), iconsPerRow) + 10,
                iconFrameSize * rows + 10, 0xA0000000));

        int startX = baseX + 5, startY = baseY + 5;
        for (int i = 0; i < groupedQueue.size(); i++) {
            ProductionQueueGroup group = groupedQueue.get(i);
            int iconX = startX + (i % iconsPerRow) * iconFrameSize;
            int iconY = startY + (i / iconsPerRow) * iconFrameSize;

            Button btn = createQueueButton(group);
            btn.render(guiGraphics, iconX, iconY, mouseX, mouseY);
            renderedButtons.add(btn);
            renderQueueGroupCount(guiGraphics, iconX, iconY, iconFrameSize, group.count);

            if (btn.isMouseOver(mouseX, mouseY)) btn.renderTooltip(guiGraphics, mouseX, mouseY);
        }
        return new Pair<>(hudZones, renderedButtons);
    }
}
