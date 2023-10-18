package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.units.piglins.PiglinGruntProd;
import com.solegendary.reignofnether.unit.units.villagers.VillagerProd;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class CitadelPortal extends ProductionBuilding implements NetherConvertingBuilding {

    public final static String buildingName = "Citadel Portal";
    public final static String structureName = "town_centre";
    public final static ResourceCost cost = ResourceCosts.CITADEL_PORTAL;

    private final double NETHER_CONVERT_RANGE_MAX = 45;
    private double netherConvertRange = 3;
    private int netherConvertTicksLeft = NETHER_CONVERT_TICKS_MAX;
    private int convertsAfterMaxRange = 0;

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

    public CitadelPortal(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), true);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.OBSIDIAN;
        this.icon = new ResourceLocation("minecraft", "textures/block/obsidian.png");

        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;
        this.buildTimeModifier = 0.8f;
        this.canAcceptResources = true;

        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);
        this.startingBlockTypes.add(Blocks.GRASS_BLOCK);
        this.startingBlockTypes.add(Blocks.POLISHED_ANDESITE_STAIRS);

        if (level.isClientSide())
            this.productionButtons = Arrays.asList(
                    PiglinGruntProd.getStartButton(this, Keybindings.keyQ)
            );
    }

    public Faction getFaction() {return Faction.PIGLINS;}

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                CitadelPortal.buildingName,
                new ResourceLocation("minecraft", "textures/block/obsidian.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == CitadelPortal.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(CitadelPortal.class),
                null,
                List.of(
                        FormattedCharSequence.forward(CitadelPortal.buildingName + " (Capitol)", Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedPop(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("The primary portal to transport piglin grunts from the nether.", Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("You may only have one capitol building at any time.", Style.EMPTY)
                ),
                null
        );
    }
}