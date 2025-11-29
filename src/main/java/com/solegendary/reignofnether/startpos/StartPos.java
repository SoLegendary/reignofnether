package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;

public class StartPos {
    public BlockPos pos;
    public Faction faction = Faction.NONE; // if != NONE, is reserved by a player
    public String playerName = ""; // name of player who has reserved this spot
    public int colorId;

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
    }

    public ResourceLocation getIcon() {
        if (colorId == MapColor.COLOR_MAGENTA.id) return getIcon("magenta");
        else if (colorId == MapColor.COLOR_LIGHT_BLUE.id) return getIcon("light_blue");
        else if (colorId == MapColor.COLOR_ORANGE.id) return getIcon("orange");
        else if (colorId == MapColor.COLOR_YELLOW.id) return getIcon("yellow");
        else if (colorId == MapColor.COLOR_LIGHT_GREEN.id) return getIcon("lime");
        else if (colorId == MapColor.COLOR_PINK.id) return getIcon("pink");
        else if (colorId == MapColor.COLOR_GRAY.id) return getIcon("gray");
        else if (colorId == MapColor.COLOR_LIGHT_GRAY.id) return getIcon("light_gray");
        else if (colorId == MapColor.COLOR_CYAN.id) return getIcon("cyan");
        else if (colorId == MapColor.COLOR_PURPLE.id) return getIcon("purple");
        else if (colorId == MapColor.COLOR_BLUE.id) return getIcon("blue");
        else if (colorId == MapColor.COLOR_BROWN.id) return getIcon("brown");
        else if (colorId == MapColor.COLOR_GREEN.id) return getIcon("green");
        else if (colorId == MapColor.COLOR_RED.id) return getIcon("red");
        else if (colorId == MapColor.COLOR_BLACK.id) return getIcon("black");
        else return getIcon("white");
    }

    private ResourceLocation getIcon(String colorName) {
        return ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/block/rts_start_block_" + colorName + ".png");
    }
}
