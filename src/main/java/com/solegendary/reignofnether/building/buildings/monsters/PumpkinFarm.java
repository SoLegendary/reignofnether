package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class PumpkinFarm extends Building {

    public final static String buildingName = "Pumpkin Farm";
    public final static String structureName = "pumpkin_farm";

    public PumpkinFarm(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.PUMPKIN;
        this.icon = new ResourceLocation("minecraft", "textures/block/pumpkin_side.png");

        this.foodCost = ResourceCosts.PumpkinFarm.FOOD;
        this.woodCost = ResourceCosts.PumpkinFarm.WOOD;
        this.oreCost = ResourceCosts.PumpkinFarm.ORE;
        this.popSupply = ResourceCosts.PumpkinFarm.SUPPLY;

        this.explodeChance = 0;
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                PumpkinFarm.buildingName,
                Button.itemIconSize,
                new ResourceLocation("minecraft", "textures/block/pumpkin_side.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == PumpkinFarm.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(PumpkinFarm.class),
                null,
                List.of(
                        FormattedCharSequence.forward(PumpkinFarm.buildingName, Style.EMPTY),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.PumpkinFarm.WOOD + "  +  5  per  crop  planted", MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A pumpkin field that be can harvested to collect food.", Style.EMPTY),
                        FormattedCharSequence.forward("Pumpkins grow slowly but do not require replanting.", Style.EMPTY)
                ),
                0,0,0
        );
    }
}
