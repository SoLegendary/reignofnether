package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.text.WordUtils;

public class CustomBuilding extends Building {

    public BlockPos structurePos;
    public Vec3i structureSize;

    public CustomBuilding(String structureName, BlockPos structurePos, Vec3i structureSize, Block portraitBlock) {
        super(structureName, ResourceCost.Building(0,0,0,0), false);
        this.name = WordUtils.capitalize(structureName
                .replace("minecraft:", "")
                .replace("reignofnether:", "")
                .replace("_", " "));
        this.structurePos = structurePos;
        this.structureSize = structureSize;
        this.portraitBlock = portraitBlock;
        this.icon = new ResourceLocation("minecraft", "textures/block/command_block.png");
    }

    public Faction getFaction() {return Faction.NONE;}

    @Override
    public AbilityButton getBuildButton(Keybinding hotkey) {
        return null;
    }
}
