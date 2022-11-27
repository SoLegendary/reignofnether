package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.productionitems.CreeperUnitProd;
import com.solegendary.reignofnether.building.productionitems.SkeletonUnitProd;
import com.solegendary.reignofnether.building.productionitems.ZombieUnitProd;
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
import java.util.Arrays;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Graveyard extends ProductionBuilding {

    public final static String buildingName = "Graveyard";
    public final static String structureName = "graveyard";

    public Graveyard(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.MOSSY_STONE_BRICKS;
        this.spawnRadiusOffset = 1;

        this.foodCost = ResourceCosts.Graveyard.FOOD;
        this.woodCost = ResourceCosts.Graveyard.WOOD;
        this.oreCost = ResourceCosts.Graveyard.ORE;
        this.popSupply = ResourceCosts.Graveyard.SUPPLY;

        this.explodeChance = 0;

        if (level.isClientSide())
            this.productionButtons = Arrays.asList(
                ZombieUnitProd.getStartButton(this),
                SkeletonUnitProd.getStartButton(this),
                CreeperUnitProd.getStartButton(this)
            );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton() {
        return new AbilityButton(
                Graveyard.buildingName,
                Button.itemIconSize,
                new ResourceLocation("minecraft", "textures/block/mossy_stone_bricks.png"),
                null,
                () -> BuildingClientEvents.getBuildingToPlace() == Graveyard.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(Graveyard.class),
                List.of(
                        FormattedCharSequence.forward(Graveyard.buildingName, Style.EMPTY),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.Graveyard.WOOD, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A spooky field that can create monster units", Style.EMPTY)
                ),
                0,0,0
        );
    }
}
