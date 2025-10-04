package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.Faction;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;

// Dashboard for Observer players to track RTS players' resources and population
public class ObserverPlayerDisplay extends AbstractPlayerDisplay {

    private static final Minecraft MC = Minecraft.getInstance();
    public final Resources resources;

    public ObserverPlayerDisplay(RTSPlayer rtsPlayer) {
        super(rtsPlayer);
        this.resources = ResourcesClientEvents.getResources(rtsPlayer.name);
    }

    private static final int RESOURCE_FRAME_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 3; // frame containing a resource value + icon
    private static final int SUPPLY_DETAIL_FRAME_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 4; // frame containing a resource value + icon
    public static final int DISPLAY_WIDTH = PLAYER_FRAME_WIDTH + RESOURCE_FRAME_WIDTH * 4 + SUPPLY_DETAIL_FRAME_WIDTH; // total width of a player display

    private final static int frameBgColour = 0xA0000000;

    private void renderResource(GuiGraphics guiGraphics, int x, int y, ResourceName resource) {
        ResourceLocation icon;
        String value;
        int color = 0xFFFFFF;
        switch (resource) {
            case FOOD -> {
                icon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wheat.png");
                value = String.valueOf(resources.food);
                color = 0xE8BC5F;
            }
            case WOOD -> {
                icon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wood.png");
                value = String.valueOf(resources.wood);
                color = 0xA3753B;
            }
            case ORE -> {
                icon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/iron_ore.png");
                value = String.valueOf(resources.ore);
                color = 0xFFF4ED;
            }
            default -> {
                icon = PlayerColors.getPlayerColorBedIcon(this.rtsPlayer.name);
                var used = UnitClientEvents.getCurrentPopulation(this.rtsPlayer.name);
                var produced = BuildingClientEvents.getTotalPopulationSupply(this.rtsPlayer.name);
                value = used + "/" + produced;
                color = used > produced
                        ? 0xFF0000
                        : 0xFFFFFF;
            }
        }

        MyRenderer.renderFrameWithBg(guiGraphics,
                x, y,
                RESOURCE_FRAME_WIDTH,
                Button.DEFAULT_ICON_FRAME_SIZE,
                frameBgColour
        );
        MyRenderer.renderIcon(guiGraphics,
                icon,
                x + 4,
                y + 4,
                Button.DEFAULT_ICON_SIZE
        );
        guiGraphics.drawString(
                MC.font,
                value,
                x + (Button.DEFAULT_ICON_FRAME_SIZE),
                y + (Button.DEFAULT_ICON_SIZE / 2) + 1,
                color
        );
        if (!isPlayerLoggedIn()) {
            guiGraphics.fill(
                    x, y,
                    x + RESOURCE_FRAME_WIDTH,
                    y + Button.DEFAULT_ICON_FRAME_SIZE,
                    0x99000000
            );
        }
    }

    private void renderSupplyDetail(GuiGraphics guiGraphics, int x, int y) {
        int civilianSupply = 0;
        int militarySupply = 0;

        for (LivingEntity entities : UnitClientEvents.getAllUnits()) {
            if (!(entities instanceof Unit unit)) {
                continue;
            }

            if (!this.rtsPlayer.name.equals(unit.getOwnerName())) {
                continue;
            }

            if (unit instanceof WorkerUnit) {
                civilianSupply += unit.getCost().population;
            } else {
                militarySupply += unit.getCost().population;
            }
        }

        MyRenderer.renderFrameWithBg(guiGraphics,
                x, y,
                SUPPLY_DETAIL_FRAME_WIDTH,
                Button.DEFAULT_ICON_FRAME_SIZE,
                frameBgColour
        );
        MyRenderer.renderIcon(guiGraphics,
                //new ResourceLocation(ReignOfNether.MOD_ID, "textures/cursors/customcursor_shovel.png"),
                //new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/pickaxe.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/shovel.png"),
                x + 4,
                y + 4,
                Button.DEFAULT_ICON_SIZE
        );
        guiGraphics.drawString(
                MC.font,
                "" + civilianSupply,
                x + (Button.DEFAULT_ICON_FRAME_SIZE),
                y + (Button.DEFAULT_ICON_SIZE / 2) + 1,
                0xFFFFFF
        );
        MyRenderer.renderIcon(guiGraphics,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/sword_and_bow.png"),
                x + SUPPLY_DETAIL_FRAME_WIDTH / 2 + 4,
                y + 4,
                Button.DEFAULT_ICON_SIZE
        );
        guiGraphics.drawString(
                MC.font,
                "" + militarySupply,
                x + SUPPLY_DETAIL_FRAME_WIDTH / 2 + (Button.DEFAULT_ICON_FRAME_SIZE),
                y + (Button.DEFAULT_ICON_SIZE / 2) + 1,
                0xFFFFFF
        );
        if (!isPlayerLoggedIn()) {
            guiGraphics.fill(
                    x, y,
                    x + SUPPLY_DETAIL_FRAME_WIDTH,
                    y + Button.DEFAULT_ICON_FRAME_SIZE,
                    0x99000000
            );
        }
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        super.render(guiGraphics, x, y);
        this.renderResource(guiGraphics, x += PLAYER_FRAME_WIDTH, y, ResourceName.FOOD);
        this.renderResource(guiGraphics, x += RESOURCE_FRAME_WIDTH, y, ResourceName.WOOD);
        this.renderResource(guiGraphics, x += RESOURCE_FRAME_WIDTH, y, ResourceName.ORE);
        this.renderResource(guiGraphics, x += RESOURCE_FRAME_WIDTH, y, ResourceName.NONE); // supply
        this.renderSupplyDetail(guiGraphics, x += RESOURCE_FRAME_WIDTH, y);
    }
}
