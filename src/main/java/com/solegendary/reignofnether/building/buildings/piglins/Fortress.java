package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.FortressPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Fortress extends ProductionBuilding {

    public final static String buildingName = "Fortress";
    public final static String structureName = "fortress";
    public final static ResourceCost cost = ResourceCosts.FORTRESS;

    public Fortress() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.CHISELED_NETHER_BRICKS;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/chiseled_nether_bricks.png");

        this.buildTimeModifier = 0.5f;

        this.canSetRallyPoint = false;

        this.startingBlockTypes.add(Blocks.NETHERRACK);
        this.startingBlockTypes.add(Blocks.NETHER_BRICKS);
        this.startingBlockTypes.add(Blocks.POLISHED_BASALT);
        this.startingBlockTypes.add(Blocks.NETHER_BRICK_STAIRS);

        this.productions.add(ProductionItems.RESEARCH_ADVANCED_PORTALS, Keybindings.keyQ);
        this.productions.add(ProductionItems.RESEARCH_BLOODLUST, Keybindings.keyW);
        this.productions.add(ProductionItems.RESEARCH_SOUL_FIREBALLS, Keybindings.keyE);
    }

    public Faction getFaction() {return Faction.PIGLINS;}

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new FortressPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
            name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/chiseled_nether_bricks.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.FORTRESS,
            () -> false,
            () -> (BuildingClientEvents.hasFinishedBuilding(Buildings.FLAME_SANCTUARY) &&
                    BuildingClientEvents.hasFinishedBuilding(Buildings.WITHER_SHRINE)) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(
                    FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.fortress"), Style.EMPTY.withBold(true)),
                    ResourceCosts.getFormattedCost(cost),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.fortress.tooltip1"), Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.fortress.tooltip2", FortressPlacement.MAX_OCCUPANTS), Style.EMPTY),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.fortress.tooltip3"), Style.EMPTY)
            ),
            this
        );
    }
}
