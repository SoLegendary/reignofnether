package com.solegendary.reignofnether.hud.playerdisplay;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.hud.panels.ButtonCell;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** An observer row: a player's resources and population, for spectators. */
public class ObserverPlayerDisplay extends AbstractPlayerDisplay {

    private static final ResourceLocation WORKER_ICON = rl("textures/icons/items/shovel.png");
    private static final ResourceLocation ARMY_ICON = rl("textures/icons/items/sword_and_bow.png");
    private static final int CELL_W = 56;
    private static final int CELL_H = 17;
    public static final int CELL_STEP = CELL_W + 5;

    public ObserverPlayerDisplay(RTSPlayer rtsPlayer) {
        super(rtsPlayer);
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, path);
    }

    public void render(HudFrame f, int x, int y, int w) {
        String status;
        if (!isPlayerLoggedIn())
            status = I18n.get("hud.reignofnether.diplomacy.offline");
        else if (!AlliancesClient.getAllAllies(playerName).isEmpty())
            status = I18n.get("hud.reignofnether.observer.allies", String.join(", ", AlliancesClient.getAllAllies(playerName)));
        else
            status = "";
        int cx = renderIdentity(f, x, y, w, status, Theme.OK);
        int cy = y + (ROW_H - CELL_H) / 2;
        if (hiddenByFog()) {
            Text.draw(f.g, I18n.get("hud.reignofnether.observer.fog_hidden"), cx, y + ROW_H / 2 - 4, Theme.LABEL);
            fadeIfOffline(f, x, y, w);
            return;
        }

        Resources resources = ResourcesClientEvents.getResources(playerName);
        if (resources != null) {
            cx = cell(f, cx, cy, ButtonCell.ICON_FOOD, String.valueOf(resources.food), Theme.TEXT, "resources.reignofnether.food");
            cx = cell(f, cx, cy, ButtonCell.ICON_WOOD, String.valueOf(resources.wood), Theme.TEXT, "resources.reignofnether.wood");
            cx = cell(f, cx, cy, ButtonCell.ICON_ORE, String.valueOf(resources.ore), Theme.TEXT, "resources.reignofnether.ore");
        }
        int used = UnitClientEvents.getCurrentPopulation(playerName);
        int supply = BuildingClientEvents.getTotalPopulationSupply(playerName);
        cx = cell(f, cx, cy, PlayerColors.getPlayerColorBedIcon(playerName), used + "/" + supply,
                used > supply ? Theme.ERROR : Theme.TEXT, "resources.reignofnether.population");

        int workers = 0;
        int army = 0;
        for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
            if (entity instanceof Unit unit && playerName.equals(unit.getOwnerName())) {
                if (unit instanceof WorkerUnit)
                    workers += unit.getCost().population;
                else
                    army += unit.getCost().population;
            }
        }
        cx = cell(f, cx, cy, WORKER_ICON, String.valueOf(workers), Theme.TEXT, "hud.reignofnether.observer.worker_pop");
        cell(f, cx, cy, ARMY_ICON, String.valueOf(army), Theme.TEXT, "hud.reignofnether.observer.army_pop");
        fadeIfOffline(f, x, y, w);
    }

    /** Compact stat cell. Returns the x of the next cell. */
    private static int cell(HudFrame f, int x, int y, ResourceLocation icon, String value, int colour, String tooltipKey) {
        Theme.outline(f.g, x, y, CELL_W, CELL_H, 1, Theme.INK);
        f.g.fill(x, y, x + CELL_W, y + CELL_H, Theme.EMPTY);
        MyRenderer.renderIcon(f.g, icon, x + 4, y + 4, 9);
        Text.bigRight(f.g, value, x + CELL_W - 4, y + 5, colour, 1f);
        Rect r = new Rect(x, y, CELL_W, CELL_H);
        if (f.hovered(r))
            Tooltip.of(I18n.get(tooltipKey)).show(r.x(), r.y(), r.w(), r.h());
        return x + CELL_STEP;
    }
}
