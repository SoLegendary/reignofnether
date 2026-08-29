package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class DarkWatchtower extends Building implements GarrisonableBuildingAddon {
    public final static int MAX_OCCUPANTS = 3;

    public final static String buildingName = "Dark Watchtower";
    public final static String structureName = "dark_watchtower";
    public final static ResourceCost cost = ResourceCosts.DARK_WATCHTOWER;

    public DarkWatchtower() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.DEEPSLATE_BRICKS;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/deepslate_bricks.png");

        this.buildTimeModifier = 1.0f;

        this.startingBlockTypes.add(Blocks.DEEPSLATE_BRICKS);
        this.startingBlockTypes.add(Blocks.DEEPSLATE_BRICK_SLAB);
        this.startingBlockTypes.add(Blocks.CRACKED_DEEPSLATE_BRICKS);

        setActiveAddon(GarrisonableBuildingAddon.class, this, true);

        this.maxHealth = 240d;
    }


    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(
            name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/deepslate_bricks.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.DARK_WATCHTOWER,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.MAUSOLEUM) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(
                    Component.translatable("buildings.reignofnether.dark_watchtower").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                    ResourceCosts.getFormattedCost(cost),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    Component.translatable("buildings.reignofnether.dark_watchtower.tooltip1").getVisualOrderText(),
                    Component.translatable("buildings.reignofnether.dark_watchtower.tooltip2").getVisualOrderText(),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    Component.translatable("buildings.reignofnether.dark_watchtower.tooltip3", MAX_OCCUPANTS).getVisualOrderText()
            ),
            this
        );
    }

    // don't use this for abilities as it may not be balanced
    public int getAttackRange() { return 24; }

    // bonus for units attacking garrisoned units
    public int getExternalAttackRangeBonus() { return 10; }
    @Override
    public BlockPos getEntryPosition(BuildingPlacement placement) {
        return placement.originPos.offset(BuildingUtils.rotatePos(new BlockPos(2,11,2), placement.rotation));
    }

    @Override
    public BlockPos getExitPosition(BuildingPlacement placement) {
        return placement.originPos.offset(BuildingUtils.rotatePos(new BlockPos(2,1,2), placement.rotation));
    }

    @Override
    public int getCapacity() { return MAX_OCCUPANTS; }

    public boolean canDestroyBlock(BlockPos relativeBp, BuildingPlacement placement) {
        return relativeBp.getY() != 10 &&
                relativeBp.getY() != 11;
    }
}
