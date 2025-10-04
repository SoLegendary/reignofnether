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

public abstract class AbstractPlayerDisplay {

    private static final Minecraft MC = Minecraft.getInstance();

    public final RTSPlayer rtsPlayer;
    public final AbstractClientPlayer player;

    private int color;
    private int backgroundColor;

    public AbstractPlayerDisplay(RTSPlayer rtsPlayer) {
        this.rtsPlayer = rtsPlayer;
        var server = MC.getCurrentServer();
        if (server != null) {
            this.player = MC.level.players().stream().filter(p -> p.getName().getString().equals(this.rtsPlayer.name)).findFirst().orElse(null);
        } else if (MC.player.getName().getString().equals(this.rtsPlayer.name)) {
            this.player = MC.player;
        } else {
            this.player = null;
        }
    }

    public boolean isPlayerLoggedIn() {
        return MC.getConnection() != null && MC.getConnection().getPlayerInfo(rtsPlayer.name) != null;
    }

    public static final int PLAYER_FRAME_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 6; // frame containing player name + player icon + race icon

    public static final ResourceLocation defaultIconLocation = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_back.png");

    public static final ResourceLocation factionVillagerIconLocation = new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/villager.png");
    public static final ResourceLocation factionMonsterIconLocation = new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png");
    public static final ResourceLocation factionPiglinIconLocation = new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png");

    private void renderPlayer(GuiGraphics guiGraphics, int x, int y) {
        // render colored background
        MyRenderer.renderFrameWithBg(guiGraphics,
                x, y,
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
                    x + Button.DEFAULT_ICON_FRAME_SIZE,
                    y + 4,
                    Button.DEFAULT_ICON_SIZE, Button.DEFAULT_ICON_SIZE,
                    8.0f, 8.0f, // where on texture to start drawing from
                    8, 8, // dimensions of blit texture
                    64, 64 // size of texture itself (if < dimensions, texture is repeated)
            );
            // draw hat
            guiGraphics.blit(iconLocation,
                    x + Button.DEFAULT_ICON_FRAME_SIZE,
                    y + 4,
                    Button.DEFAULT_ICON_SIZE, Button.DEFAULT_ICON_SIZE,
                    40.0f, 8.0f, // where on texture to start drawing from
                    8, 8, // dimensions of blit texture
                    64, 64 // size of texture itself (if < dimensions, texture is repeated)
            );
        } else {
            MyRenderer.renderIcon(guiGraphics,
                    defaultIconLocation,
                    x + Button.DEFAULT_ICON_FRAME_SIZE,
                    y + 4,
                    Button.DEFAULT_ICON_SIZE
            );
        }

        // render player name
        guiGraphics.drawString(
                MC.font,
                this.rtsPlayer.name,
                x + (Button.DEFAULT_ICON_FRAME_SIZE * 2),
                y + (Button.DEFAULT_ICON_SIZE / 2) + 1,
                0xFFFFFF
        );
        if (!isPlayerLoggedIn()) {
            guiGraphics.fill(
                    x, y,
                    x + PLAYER_FRAME_WIDTH,
                    y + Button.DEFAULT_ICON_FRAME_SIZE,
                    0x99000000
            );
        }
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        int playerColorHex = PlayerColors.getPlayerColorHex(this.rtsPlayer.name);
        this.color = 0xFF000000 | playerColorHex;
        this.backgroundColor = 0xA0000000 | playerColorHex;
        this.renderPlayer(guiGraphics, x, y);
    }
}
