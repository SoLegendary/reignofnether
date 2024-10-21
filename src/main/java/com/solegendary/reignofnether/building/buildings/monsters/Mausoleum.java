package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.ZombieVillagerProd;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Mausoleum extends ProductionBuilding {

    public final static String buildingName = "Mausoleum";
    public final static String structureName = "mausoleum";
    public final static ResourceCost cost = ResourceCosts.MAUSOLEUM;
    public final static int nightRange = 80;

    public Mausoleum(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), true);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.DEEPSLATE_TILES;
        this.icon = new ResourceLocation("minecraft", "textures/block/deepslate_tiles.png");

        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;
        this.buildTimeModifier = 0.33f;
        this.canAcceptResources = true;

        this.startingBlockTypes.add(Blocks.STONE);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);
        this.startingBlockTypes.add(Blocks.STONE_BRICKS);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);

        if (level.isClientSide())
            this.productionButtons = List.of(
                ZombieVillagerProd.getStartButton(this, Keybindings.keyQ)
            );
    }

    public Faction getFaction() {return Faction.MONSTERS;}

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
            Mausoleum.buildingName,
            new ResourceLocation("minecraft", "textures/block/deepslate_tiles.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Mausoleum.class,
            () -> false,
            () -> true,
            () -> BuildingClientEvents.setBuildingToPlace(Mausoleum.class),
            null,
            List.of(
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.mausoleum"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPop(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.mausoleum.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.mausoleum.tooltip2", nightRange), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.mausoleum.tooltip3"), Style.EMPTY)
            ),
            null
        );
    }
}