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

public class ObserverPlayerDisplay {

    private static final Minecraft MC = Minecraft.getInstance();

    public final RTSPlayer rtsPlayer;
    public final AbstractClientPlayer player;
    public final Resources resources;

    private int color;
    private int backgroundColor;

    public ObserverPlayerDisplay(RTSPlayer rtsPlayer) {
        this.rtsPlayer = rtsPlayer;
        this.resources = ResourcesClientEvents.getResources(rtsPlayer.name);
        var server = MC.getCurrentServer();
        if (server != null) {
            this.player = MC.level.players().stream().filter(p -> p.getName().getString().equals(this.rtsPlayer.name)).findFirst().orElse(null);
        } else if (MC.player.getName().getString().equals(this.rtsPlayer.name)) {
            this.player = MC.player;
            var loaded = this.player.isSkinLoaded();
        } else {
            this.player = null;
        }
    }

    private static final int PLAYER_FRAME_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 6; // frame containing player name + player icon + race icon
    private static final int PLAYER_VALUE_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 4; // name of the player
    private static final int RESOURCE_FRAME_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 3; // frame containing a resource value + icon
    private static final int RESOURCE_VALUE_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 2; // value of the resource
    private static final int SUPPLY_DETAIL_FRAME_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 4; // frame containing a resource value + icon
    public static final int DISPLAY_WIDTH = PLAYER_FRAME_WIDTH + RESOURCE_FRAME_WIDTH * 4 + SUPPLY_DETAIL_FRAME_WIDTH; // total width of a player display

    public static final ResourceLocation defaultIconLocation = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_back.png");

    public static final ResourceLocation factionVillagerIconLocation = new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/villager.png");
    public static final ResourceLocation factionMonsterIconLocation = new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png");
    public static final ResourceLocation factionPiglinIconLocation = new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png");

    private void renderPlayer(GuiGraphics guiGraphics, int x, int y) {
        // render colored background
        MyRenderer.renderFrameWithBg(guiGraphics,
                x,
                y,
                PLAYER_FRAME_WIDTH,
                Button.DEFAULT_ICON_FRAME_SIZE,
                this.backgroundColor
        );

        // render faction icon
        ResourceLocation factionIcon;
        switch (this.rtsPlayer.faction) {
            case VILLAGERS -> factionIcon = factionVillagerIconLocation;
            case MONSTERS -> factionIcon = factionMonsterIconLocation;
            case PIGLINS -> factionIcon = factionPiglinIconLocation;
            default -> factionIcon = null;
        }
        if(factionIcon != null) {
            MyRenderer.renderIcon(guiGraphics,
                    factionIcon,
                    x + 4,
                    y + 4,
                    Button.DEFAULT_ICON_SIZE
            );
        }

        // render player head
        if (this.player != null && this.player.isSkinLoaded()) {
            var iconLocation = player.getSkinTextureLocation();
            //RenderSystem.setShaderTexture(0, iconLocation);
            // draw base layer
            guiGraphics.blit(iconLocation,
                    x + 4 + Button.DEFAULT_ICON_FRAME_SIZE,
                    y + 4,
                    Button.DEFAULT_ICON_SIZE, Button.DEFAULT_ICON_SIZE,
                    8.0f, 8.0f, // where on texture to start drawing from
                    8, 8, // dimensions of blit texture
                    64, 64 // size of texture itself (if < dimensions, texture is repeated)
            );
            // draw hat
            guiGraphics.blit(iconLocation,
                    x + 4 + Button.DEFAULT_ICON_FRAME_SIZE,
                    y + 4,
                    Button.DEFAULT_ICON_SIZE, Button.DEFAULT_ICON_SIZE,
                    40.0f, 8.0f, // where on texture to start drawing from
                    8, 8, // dimensions of blit texture
                    64, 64 // size of texture itself (if < dimensions, texture is repeated)
            );
        } else {
            MyRenderer.renderIcon(guiGraphics,
                    defaultIconLocation,
                    x + 4 + Button.DEFAULT_ICON_FRAME_SIZE,
                    y + 4,
                    Button.DEFAULT_ICON_SIZE
            );
        }

        // render player name
        guiGraphics.drawString(
                MC.font,
                this.resources.ownerName,
                x + (Button.DEFAULT_ICON_FRAME_SIZE * 2),
                y + (Button.DEFAULT_ICON_SIZE / 2) + 1,
                0xFFFFFF
        );
    }

    private final static int frameBgColour = 0xA0000000;

    private void renderResource(GuiGraphics guiGraphics, int x, int y, ResourceName resource) {
        ResourceLocation icon;
        String value;
        int color = 0xFFFFFF;
        switch (resource) {
            case FOOD -> {
                icon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/wheat.png");
                value = String.valueOf(resources.food);
                color = 0xE8BC5F;
            }
            case WOOD -> {
                icon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/wood.png");
                value = String.valueOf(resources.wood);
                color = 0xA3753B;
            }
            case ORE -> {
                icon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/iron_ore.png");
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
                x,
                y,
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
                x,
                y,
                SUPPLY_DETAIL_FRAME_WIDTH,
                Button.DEFAULT_ICON_FRAME_SIZE,
                frameBgColour
        );

        MyRenderer.renderIcon(guiGraphics,
                //new ResourceLocation(ReignOfNether.MOD_ID, "textures/cursors/customcursor_shovel.png"),
                //new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/pickaxe.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/shovel.png"),
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
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/sword_and_bow.png"),
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
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        //var baseX = x;
        int playerColorHex = PlayerColors.getPlayerColorHex(this.rtsPlayer.name);
        this.color = 0xFF000000 | playerColorHex;
        this.backgroundColor = 0xA0000000 | playerColorHex;
        this.renderPlayer(guiGraphics, x, y); // icon, name
        this.renderResource(guiGraphics, x += PLAYER_FRAME_WIDTH, y, ResourceName.FOOD);
        this.renderResource(guiGraphics, x += RESOURCE_FRAME_WIDTH, y, ResourceName.WOOD);
        this.renderResource(guiGraphics, x += RESOURCE_FRAME_WIDTH, y, ResourceName.ORE);
        this.renderResource(guiGraphics, x += RESOURCE_FRAME_WIDTH, y, ResourceName.NONE); // supply
        this.renderSupplyDetail(guiGraphics, x += RESOURCE_FRAME_WIDTH, y);

        //x = baseX;
        //guiGraphics.fill(x - 2, y + 2, x + 2, y + Button.DEFAULT_ICON_FRAME_SIZE - 2, this.color);
        //guiGraphics.fill((x += PLAYER_FRAME_WIDTH) - 2, y + 2, x + 2, y + Button.DEFAULT_ICON_FRAME_SIZE - 2, this.color);
        //guiGraphics.fill((x += RESOURCE_FRAME_WIDTH) - 2, y + 2, x + 2, y + Button.DEFAULT_ICON_FRAME_SIZE - 2, this.color);
        //guiGraphics.fill((x += RESOURCE_FRAME_WIDTH) - 2, y + 2, x + 2, y + Button.DEFAULT_ICON_FRAME_SIZE - 2, this.color);
        //guiGraphics.fill((x += RESOURCE_FRAME_WIDTH) - 2, y + 2, x + 2, y + Button.DEFAULT_ICON_FRAME_SIZE - 2, this.color);
        //guiGraphics.fill((x += RESOURCE_FRAME_WIDTH) - 2, y + 2, x + 2, y + Button.DEFAULT_ICON_FRAME_SIZE - 2, this.color);
        //guiGraphics.fill((x += SUPPLY_DETAIL_FRAME_WIDTH) - 2, y + 2, x + 2, y + Button.DEFAULT_ICON_FRAME_SIZE - 2, this.color);
        //guiGraphics.fill(baseX, y + Button.DEFAULT_ICON_FRAME_SIZE - 2, baseX + DISPLAY_WIDTH, y + Button.DEFAULT_ICON_FRAME_SIZE, this.color);
    }
}
