package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.units.villagers.VillagerProdItem;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.resources.ResourceCosts;
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

public class TownCentre extends ProductionBuilding {

    public final static String buildingName = "Town Centre";
    public final static String structureName = "town_centre";

    public TownCentre(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation));
        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.POLISHED_GRANITE;
        this.icon = new ResourceLocation("minecraft", "textures/block/polished_granite.png");

        this.foodCost = ResourceCosts.TownCentre.FOOD;
        this.woodCost = ResourceCosts.TownCentre.WOOD;
        this.oreCost = ResourceCosts.TownCentre.ORE;
        this.popSupply = ResourceCosts.TownCentre.SUPPLY;
        this.buildTimeModifier = 0.8f;
        this.canAcceptResources = true;

        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);
        this.startingBlockTypes.add(Blocks.GRASS_BLOCK);
        this.startingBlockTypes.add(Blocks.POLISHED_ANDESITE_STAIRS);

        if (level.isClientSide())
            this.productionButtons = List.of(
                VillagerProdItem.getStartButton(this, Keybindings.keyQ)
            );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                TownCentre.buildingName,
                Button.itemIconSize,
                new ResourceLocation("minecraft", "textures/block/polished_granite.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == TownCentre.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(TownCentre.class),
                null,
                List.of(
                        FormattedCharSequence.forward(TownCentre.buildingName, Style.EMPTY),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.TownCentre.WOOD + "  \uE002  " + ResourceCosts.TownCentre.ORE, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A gazebo at the centre of your village that produces villagers.", Style.EMPTY),
                        FormattedCharSequence.forward("Is required to build most other buildings.", Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("Supports " + ResourceCosts.TownCentre.SUPPLY + " population.", Style.EMPTY)
                ),
                null
        );
    }
}
