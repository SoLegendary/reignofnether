package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.researchItems.ResearchVindicatorAxes;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.units.villagers.IronGolemProdItem;
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

public class Blacksmith extends ProductionBuilding {

    public final static String buildingName = "Blacksmith";
    public final static String structureName = "blacksmith";

    public Blacksmith(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.SMITHING_TABLE;
        this.spawnRadiusOffset = 1;
        this.icon = new ResourceLocation("minecraft", "textures/block/smithing_table_front.png");

        this.foodCost = ResourceCosts.Blacksmith.FOOD;
        this.woodCost = ResourceCosts.Blacksmith.WOOD;
        this.oreCost = ResourceCosts.Blacksmith.ORE;
        this.popSupply = ResourceCosts.Blacksmith.SUPPLY;

        if (level.isClientSide())
            this.productionButtons = Arrays.asList(
                IronGolemProdItem.getStartButton(this, Keybindings.keyQ),
                ResearchVindicatorAxes.getStartButton(this, Keybindings.keyW)
            );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                Blacksmith.buildingName,
                Button.itemIconSize,
                new ResourceLocation("minecraft", "textures/block/smithing_table_front.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Blacksmith.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(Blacksmith.class),
                null,
                List.of(
                        FormattedCharSequence.forward(Blacksmith.buildingName, Style.EMPTY),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.Blacksmith.WOOD + "  \uE002  " + ResourceCosts.Blacksmith.ORE, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A smithy to forge military upgrades and iron golems", Style.EMPTY)
                ),
                0,0,0
        );
    }
}
