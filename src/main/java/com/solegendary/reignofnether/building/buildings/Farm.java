package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.Unit;
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

public class Farm extends Building {

    public final static String buildingName = "Farm";
    public final static String structureName = "farm";

    public Farm(LevelAccessor level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.HAY_BLOCK;

        this.foodCost = ResourceCosts.Farm.FOOD;
        this.woodCost = ResourceCosts.Farm.WOOD;
        this.oreCost = ResourceCosts.Farm.ORE;
        this.popSupply = ResourceCosts.Farm.SUPPLY;

        this.explodeChance = 0;
        this.minBlocksPercent = 0.5f;
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton() {
        return new AbilityButton(
                Farm.buildingName,
                Button.itemIconSize,
                new ResourceLocation("minecraft", "textures/block/hay_block_side.png"),
                null,
                () -> BuildingClientEvents.getBuildingToPlace() == Farm.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(Farm.class),
                List.of(
                        FormattedCharSequence.forward(Farm.buildingName, Style.EMPTY),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.Farm.WOOD + " + 5 per crop planted", MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A wheat field that be can tilled to collect food.", Style.EMPTY),
                        FormattedCharSequence.forward("Workers will automatically use wood to replant seeds while working.", Style.EMPTY)
                ),
                0,0,0
        );
    }
}
