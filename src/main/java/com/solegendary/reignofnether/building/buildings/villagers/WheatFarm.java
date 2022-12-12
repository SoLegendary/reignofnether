package com.solegendary.reignofnether.building.buildings.villagers;

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

public class WheatFarm extends Building {

    public final static String buildingName = "Wheat Farm";
    public final static String structureName = "wheat_farm";

    public WheatFarm(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.HAY_BLOCK;
        this.icon = new ResourceLocation("minecraft", "textures/block/hay_block_side.png");

        this.foodCost = ResourceCosts.WheatFarm.FOOD;
        this.woodCost = ResourceCosts.WheatFarm.WOOD;
        this.oreCost = ResourceCosts.WheatFarm.ORE;
        this.popSupply = ResourceCosts.WheatFarm.SUPPLY;

        this.explodeChance = 0;
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                WheatFarm.buildingName,
                Button.itemIconSize,
                new ResourceLocation("minecraft", "textures/block/hay_block_side.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == WheatFarm.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(WheatFarm.class),
                null,
                List.of(
                        FormattedCharSequence.forward(WheatFarm.buildingName, Style.EMPTY),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.WheatFarm.WOOD + "  +  5  per  crop  planted", MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A wheat field that be can tilled to collect food.", Style.EMPTY),
                        FormattedCharSequence.forward("Workers will automatically use wood to replant seeds while working.", Style.EMPTY)
                ),
                null,
                0,0,0
        );
    }
}
