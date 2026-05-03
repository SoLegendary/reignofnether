package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class Watchtower extends Building implements GarrisonableBuildingAddon {
    public final static int MAX_OCCUPANTS = 3;

    public final static String buildingName = "Watchtower";
    public final static String structureName = "watchtower";
    public final static ResourceCost cost = ResourceCosts.WATCHTOWER;

    public Watchtower() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.STONE_BRICKS;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone_bricks.png");

        this.buildTimeModifier = 1.0f;

        this.startingBlockTypes.add(Blocks.STONE_BRICKS);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_SLAB);

        setActiveAddon(GarrisonableBuildingAddon.class, this, true);
    }

    public Faction getFaction() {return Faction.VILLAGERS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
            name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone_bricks.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.WATCHTOWER,
            TutorialClientEvents::isEnabled,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.TOWN_CENTRE) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(
                    FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.watchtower"), Style.EMPTY.withBold(true)),
                    ResourceCosts.getFormattedCost(cost),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.watchtower.tooltip1"), Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.watchtower.tooltip2"), Style.EMPTY),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.watchtower.tooltip3", MAX_OCCUPANTS), Style.EMPTY)
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
