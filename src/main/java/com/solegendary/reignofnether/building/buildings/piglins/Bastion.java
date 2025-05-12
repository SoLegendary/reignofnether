package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.BastionPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.Faction;
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

public class Bastion extends ProductionBuilding {

    public final static String buildingName = "Bastion";
    public final static String structureName = "bastion";
    public final static ResourceCost cost = ResourceCosts.BASTION;

    public Bastion() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.CHISELED_POLISHED_BLACKSTONE;
        this.icon = new ResourceLocation("minecraft", "textures/block/chiseled_polished_blackstone.png");

        this.canSetRallyPoint = false;

        this.buildTimeModifier = 0.72f;

        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE);
        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE_BRICKS);
        this.startingBlockTypes.add(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);

        this.explodeChance = 0.2f;

        this.productions.add(ProductionItems.RESEARCH_BRUTE_SHIELDS, Keybindings.keyQ);
        this.productions.add(ProductionItems.RESEARCH_HEAVY_TRIDENTS, Keybindings.keyW);
    }

    public Faction getFaction() {return Faction.PIGLINS;}

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new BastionPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
    }

    public AbilityButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new AbilityButton(
                name,
                new ResourceLocation("minecraft", "textures/block/chiseled_polished_blackstone.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.BASTION,
                () -> false,
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.PORTAL_BASIC) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                () -> BuildingClientEvents.setBuildingToPlace(Buildings.BASTION),
                null,
                List.of(
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.bastion"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.bastion.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.bastion.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.bastion.tooltip3", BastionPlacement.MAX_OCCUPANTS), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.bastion.tooltip4"), Style.EMPTY)
                ),
                null,
                (BuildingPlacement) null
        );
    }
}
