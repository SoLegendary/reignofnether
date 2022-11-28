package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import com.solegendary.reignofnether.building.BuildingClientEvents;
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

public class VillagerTower extends Building {

    public final static String buildingName = "Villager Tower";
    public final static String structureName = "villager_tower";

    public VillagerTower(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.OAK_PLANKS;

        this.foodCost = ResourceCosts.VillagerTower.FOOD;
        this.woodCost = ResourceCosts.VillagerTower.WOOD;
        this.oreCost = ResourceCosts.VillagerTower.ORE;
        this.popSupply = ResourceCosts.VillagerTower.SUPPLY;
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton() {
        return new AbilityButton(
            VillagerTower.buildingName,
            Button.itemIconSize,
            new ResourceLocation("minecraft", "textures/block/oak_planks.png"),
            null,
            () -> BuildingClientEvents.getBuildingToPlace() == VillagerTower.class,
            () -> false,
            () -> true,
            () -> BuildingClientEvents.setBuildingToPlace(VillagerTower.class),
            List.of(
                    FormattedCharSequence.forward(VillagerTower.buildingName, Style.EMPTY),
                    FormattedCharSequence.forward("\uE001  " + ResourceCosts.VillagerTower.WOOD, MyRenderer.iconStyle),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward("A lookout tower that can see into the distance.", Style.EMPTY)
            ),
            0,0,0
        );
    }
}
