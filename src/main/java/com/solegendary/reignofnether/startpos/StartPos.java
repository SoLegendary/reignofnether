package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.ButtonBuilder;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class StartPos {
    public BlockPos pos;
    public Faction faction = Faction.NONE; // if != NONE, is reserved by a player
    public String playerName = ""; // name of player who has reserved this spot
    public int colorId; // is the actual hex color if it's created via MapInfo
    public boolean isFromStartBlock = true;
    public boolean enabled = true;
    public boolean ready = false; // player on this spot is ready to start the game

    public static final String BUTTON_NAME = "Start Pos";

    public StartPos(BlockPos pos, int colorId) {
        this.pos = pos;
        this.colorId = colorId;
    }

    public StartPos(BlockPos pos, Faction faction, String playerName, int colorId) {
        this.pos = pos;
        this.faction = faction;
        this.playerName = playerName;
        this.colorId = colorId;
    }

    public void reset() {
        this.faction = Faction.NONE;
        this.playerName = "";
        this.ready = false;
    }

    public ResourceLocation getIcon() {
        if (!playerName.isBlank() && faction != Faction.NONE) {
            return switch (faction) {
                case VILLAGERS -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png");
                case MONSTERS -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png");
                case PIGLINS -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png");
                default -> null;
            };
        }
        if (colorId == MapColor.COLOR_MAGENTA.id     || colorId == 0xBD44B3) return getIcon("magenta");
        else if (colorId == MapColor.COLOR_LIGHT_BLUE.id  || colorId == 0x3AAFD9) return getIcon("light_blue");
        else if (colorId == MapColor.COLOR_ORANGE.id      || colorId == 0xF07613) return getIcon("orange");
        else if (colorId == MapColor.COLOR_YELLOW.id      || colorId == 0xF8C627) return getIcon("yellow");
        else if (colorId == MapColor.COLOR_LIGHT_GREEN.id || colorId == 0x70B919) return getIcon("lime");
        else if (colorId == MapColor.COLOR_PINK.id        || colorId == 0xED8DAC) return getIcon("pink");
        else if (colorId == MapColor.COLOR_GRAY.id        || colorId == 0x3E4447) return getIcon("gray");
        else if (colorId == MapColor.COLOR_LIGHT_GRAY.id  || colorId == 0x8E8E86) return getIcon("light_gray");
        else if (colorId == MapColor.COLOR_CYAN.id        || colorId == 0x158991) return getIcon("cyan");
        else if (colorId == MapColor.COLOR_PURPLE.id      || colorId == 0x792AAC) return getIcon("purple");
        else if (colorId == MapColor.COLOR_BLUE.id        || colorId == 0x35399D) return getIcon("blue");
        else if (colorId == MapColor.COLOR_BROWN.id       || colorId == 0x724728) return getIcon("brown");
        else if (colorId == MapColor.COLOR_GREEN.id       || colorId == 0x546D1B) return getIcon("green");
        else if (colorId == MapColor.COLOR_RED.id         || colorId == 0xA12722) return getIcon("red");
        else if (colorId == MapColor.COLOR_BLACK.id       || colorId == 0x141519) return getIcon("black");
        else return getIcon("white");
    }

    public int getHexColor() {
             if (colorId == MapColor.COLOR_MAGENTA.id     || colorId == 0xBD44B3) return 0xBD44B3;
        else if (colorId == MapColor.COLOR_LIGHT_BLUE.id  || colorId == 0x3AAFD9) return 0x3AAFD9;
        else if (colorId == MapColor.COLOR_ORANGE.id      || colorId == 0xF07613) return 0xF07613;
        else if (colorId == MapColor.COLOR_YELLOW.id      || colorId == 0xF8C627) return 0xF8C627;
        else if (colorId == MapColor.COLOR_LIGHT_GREEN.id || colorId == 0x70B919) return 0x70B919;
        else if (colorId == MapColor.COLOR_PINK.id        || colorId == 0xED8DAC) return 0xED8DAC;
        else if (colorId == MapColor.COLOR_GRAY.id        || colorId == 0x3E4447) return 0x3E4447;
        else if (colorId == MapColor.COLOR_LIGHT_GRAY.id  || colorId == 0x8E8E86) return 0x8E8E86;
        else if (colorId == MapColor.COLOR_CYAN.id        || colorId == 0x158991) return 0x158991;
        else if (colorId == MapColor.COLOR_PURPLE.id      || colorId == 0x792AAC) return 0x792AAC;
        else if (colorId == MapColor.COLOR_BLUE.id        || colorId == 0x35399D) return 0x35399D;
        else if (colorId == MapColor.COLOR_BROWN.id       || colorId == 0x724728) return 0x724728;
        else if (colorId == MapColor.COLOR_GREEN.id       || colorId == 0x546D1B) return 0x546D1B;
        else if (colorId == MapColor.COLOR_RED.id         || colorId == 0xA12722) return 0xA12722;
        else if (colorId == MapColor.COLOR_BLACK.id       || colorId == 0x141519) return 0x141519;
        return 0xFFFFFF;
    }

    private ResourceLocation getIcon(String colorName) {
        return ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/block/rts_start_block_" + colorName + ".png");
    }

    private ResourceLocation getCornerTickIcon() {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tick_corner.png");
    }

    public Button getButton(String localPlayerName) {
        ArrayList<FormattedCharSequence> fcsList = new ArrayList<>();

        if (enabled) {
            if (playerName.isBlank()) {
                fcsList.add(fcs(I18n.get("startpos.reignofnether.not_reserved"), true));
                fcsList.add(fcs(I18n.get("startpos.reignofnether.disable")));
            } else {
                fcsList.add(fcs(I18n.get("startpos.reignofnether.reserved", playerName, faction.name()), true));
            }
        } else {
            fcsList.add(fcs(I18n.get("startpos.reignofnether.disabled"), true));
            fcsList.add(fcs(I18n.get("startpos.reignofnether.enable")));
        }

        return new ButtonBuilder(BUTTON_NAME)
                .iconResource(faction != null && !playerName.isBlank() && ready ? getCornerTickIcon() : null)
                .bgIconResource(enabled ? getIcon() : null)
                .isSelected(() -> StartPosClientEvents.getPos() == this)
                .isEnabled(() -> playerName.isBlank() || localPlayerName.equals(playerName))
                .onLeftClick(() -> {
                    if (!enabled || (!playerName.isBlank() && !playerName.equals(localPlayerName)))
                        return;
                    if (StartPosClientEvents.getPos() == this)
                        StartPosServerboundPacket.unreservePos(pos);
                    else {
                        StartPosServerboundPacket.reservePos(pos, StartPosClientEvents.selectedFaction, localPlayerName);
                    }
                })
                .onRightClick(() -> {
                    if (!playerName.isBlank())
                        return;
                    if (enabled)
                        StartPosServerboundPacket.disablePos(pos);
                    else
                        StartPosServerboundPacket.enablePos(pos);
                })
                .tooltipLines(fcsList)
                .iconSize(MinimapClientEvents.isLargeMap() ? 6 : 4)
                .imageSize(MinimapClientEvents.isLargeMap() ? 12 : 10)
                .build();
    }
}
