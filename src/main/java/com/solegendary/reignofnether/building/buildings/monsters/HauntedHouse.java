package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.units.monsters.ZombieVillagerUnitProd;
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

public class HauntedHouse extends ProductionBuilding {

    public final static String buildingName = "Haunted House";
    public final static String structureName = "haunted_house";

    public HauntedHouse(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.DARK_OAK_LOG;
        this.spawnRadiusOffset = 1;
        this.icon = new ResourceLocation("minecraft", "textures/block/dark_oak_log.png");

        this.foodCost = ResourceCosts.HauntedHouse.FOOD;
        this.woodCost = ResourceCosts.HauntedHouse.WOOD;
        this.oreCost = ResourceCosts.HauntedHouse.ORE;
        this.popSupply = ResourceCosts.HauntedHouse.SUPPLY;

        if (level.isClientSide())
            this.productionButtons = List.of(
                    ZombieVillagerUnitProd.getStartButton(this, Keybindings.keyQ)
            );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                HauntedHouse.buildingName,
                Button.itemIconSize,
                new ResourceLocation("minecraft", "textures/block/dark_oak_log.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == HauntedHouse.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(HauntedHouse.class),
                null,
                List.of(
                        FormattedCharSequence.forward(HauntedHouse.buildingName, Style.EMPTY),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.HauntedHouse.WOOD, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A spooky house that can produce zombie villagers.", Style.EMPTY),
                        FormattedCharSequence.forward("Supports " + ResourceCosts.HauntedHouse.SUPPLY + " population.", Style.EMPTY)
                ),
                0,0,0
        );
    }
}
