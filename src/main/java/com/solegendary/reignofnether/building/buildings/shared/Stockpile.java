package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.monsters.Mausoleum;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.researchItems.ResearchLabLightningRod;
import com.solegendary.reignofnether.research.researchItems.ResearchResourceCapacity;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnitProd;
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

public class Stockpile extends ProductionBuilding {

    public final static String buildingName = "Stockpile";
    public final static String structureName = "stockpile";

    public Stockpile(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.CHEST;
        this.icon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/chest.png");

        this.foodCost = ResourceCosts.Stockpile.FOOD;
        this.woodCost = ResourceCosts.Stockpile.WOOD;
        this.oreCost = ResourceCosts.Stockpile.ORE;
        this.popSupply = ResourceCosts.Stockpile.SUPPLY;
        this.canAcceptResources = true;
        this.canSetRallyPoint = false;

        this.startingBlockTypes.add(Blocks.OAK_LOG);

        if (level.isClientSide()) {
            this.productionButtons = Arrays.asList(
                ResearchResourceCapacity.getStartButton(this, Keybindings.keyQ)
            );
        }
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                Stockpile.buildingName,
                Button.itemIconSize,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/chest.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Stockpile.class,
                () -> !BuildingClientEvents.hasFinishedBuilding(TownCentre.buildingName) &&
                      !BuildingClientEvents.hasFinishedBuilding(Mausoleum.buildingName),
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(Stockpile.class),
                null,
                List.of(
                        FormattedCharSequence.forward(Stockpile.buildingName, Style.EMPTY),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.Stockpile.WOOD, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("Storage for units and players to drop off resources", Style.EMPTY)
                ),
                null
        );
    }
}
