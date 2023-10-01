package com.solegendary.reignofnether.building.buildings.netherlings;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.researchItems.ResearchBruteShields;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.units.piglins.PiglinBruteProd;
import com.solegendary.reignofnether.unit.units.piglins.PiglinGruntProd;
import com.solegendary.reignofnether.unit.units.piglins.PiglinHeadhunterProd;
import com.solegendary.reignofnether.unit.units.villagers.PillagerProd;
import com.solegendary.reignofnether.unit.units.villagers.VindicatorProd;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Portal extends ProductionBuilding implements NetherConvertingBuilding {

    public final static String buildingName = "Portal";
    public final static String structureName = "portal";
    public final static ResourceCost cost = ResourceCosts.PORTAL;

    private final double NETHER_CONVERT_RANGE_MAX = 20;
    private double netherConvertRange = 3;
    private int netherConvertTicksLeft = NETHER_CONVERT_TICKS_MAX;
    private int convertsAfterMaxRange = 0;

    public Portal(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), false);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.OBSIDIAN;
        this.icon = new ResourceLocation("minecraft", "textures/block/obsidian.png");

        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;
        this.buildTimeModifier = 0.8f;

        this.startingBlockTypes.add(Blocks.NETHER_BRICKS);

        if (level.isClientSide())
            this.productionButtons = Arrays.asList(
                    PiglinGruntProd.getStartButton(this, Keybindings.keyQ),
                    PiglinBruteProd.getStartButton(this, Keybindings.keyW),
                    PiglinHeadhunterProd.getStartButton(this, Keybindings.keyE),
                    ResearchBruteShields.getStartButton(this, Keybindings.keyR)
            );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                Portal.buildingName,
                new ResourceLocation("minecraft", "textures/block/obsidian.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Portal.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(Portal.class),
                null,
                List.of(
                        FormattedCharSequence.forward(Portal.buildingName, Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedPop(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("An obsidian portal leading to the nether.", Style.EMPTY)
                ),
                null
        );
    }

    public double getMaxRange() { return NETHER_CONVERT_RANGE_MAX; }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        netherConvertTicksLeft -= 1;
        if (netherConvertTicksLeft <= 0 && convertsAfterMaxRange < MAX_CONVERTS_AFTER_MAX_RANGE) {
            netherConvertTick(this, netherConvertRange, NETHER_CONVERT_RANGE_MAX);
            if (netherConvertRange < NETHER_CONVERT_RANGE_MAX)
                netherConvertRange += 0.1f;
            else
                convertsAfterMaxRange += 1;
            netherConvertTicksLeft = NETHER_CONVERT_TICKS_MAX;
        }
    }
}




















