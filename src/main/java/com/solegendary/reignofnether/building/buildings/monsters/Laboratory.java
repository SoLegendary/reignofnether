package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
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

public class Laboratory extends ProductionBuilding {

    public final static String buildingName = "Laboratory";
    public final static String structureName = "laboratory";

    public Laboratory(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.SMITHING_TABLE;
        this.spawnRadiusOffset = 1;
        this.icon = new ResourceLocation("minecraft", "textures/block/brewing_stand.png");

        this.foodCost = ResourceCosts.Laboratory.FOOD;
        this.woodCost = ResourceCosts.Laboratory.WOOD;
        this.oreCost = ResourceCosts.Laboratory.ORE;
        this.popSupply = ResourceCosts.Laboratory.SUPPLY;

        /*
        if (level.isClientSide())
            this.productionButtons = Arrays.asList(
                    ZombieUnitProd.getStartButton(this),
                    SkeletonUnitProd.getStartButton(this),
                    CreeperUnitProd.getStartButton(this)
            );
         */
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton() {
        return new AbilityButton(
                Laboratory.buildingName,
                Button.itemIconSize,
                new ResourceLocation("minecraft", "textures/block/brewing_stand.png"),
                null,
                () -> BuildingClientEvents.getBuildingToPlace() == Laboratory.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(Laboratory.class),
                null,
                List.of(
                        FormattedCharSequence.forward(Laboratory.buildingName, Style.EMPTY),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.Laboratory.WOOD + "  \uE002  " + ResourceCosts.Laboratory.ORE, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A sinister lab that can research new technologies and", Style.EMPTY),
                        FormattedCharSequence.forward("produce creepers. Can be upgraded to have a lightning rod.", Style.EMPTY)
                ),
                0,0,0
        );
    }
}
