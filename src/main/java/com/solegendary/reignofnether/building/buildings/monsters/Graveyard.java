package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.ability.abilities.SetGraveyardReleaseOff;
import com.solegendary.reignofnether.ability.abilities.SetGraveyardReleaseOn;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.placements.GraveyardPlacement;
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

public class Graveyard extends ProductionBuilding {

    public final static String buildingName = "Graveyard";
    public final static String structureName = "graveyard";
    public final static String upgradedStructureName = "overflowing_graveyard";
    public final static ResourceCost cost = ResourceCosts.GRAVEYARD;

    public final static int OVERFLOW_AMOUNT = 10;
    public final static int OVERFLOW_AMOUNT_UPGRADED = 20;

    public Graveyard() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.MOSSY_STONE_BRICKS;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/mossy_stone_bricks.png");

        this.startingBlockTypes.add(Blocks.DEEPSLATE_BRICKS);

        this.explodeChance = 0.2f;

        this.abilities.add(new SetGraveyardReleaseOff(), Keybindings.keyO);
        this.abilities.add(new SetGraveyardReleaseOn(), Keybindings.keyO);

        this.productions.add(ProductionItems.ZOMBIE, Keybindings.keyQ);
        this.productions.add(ProductionItems.HUSK, Keybindings.keyQ);
        this.productions.add(ProductionItems.DROWNED, Keybindings.keyW);
        this.productions.add(ProductionItems.SKELETON, Keybindings.keyE);
        this.productions.add(ProductionItems.STRAY, Keybindings.keyE);
        this.productions.add(ProductionItems.BOGGED, Keybindings.keyR);
        this.productions.add(ProductionItems.RESEARCH_OVERFLOWING_GRAVEYARD, Keybindings.keyT);
    }

    @Override
    public String getUpgradedName(BuildingPlacement placement) {
        return I18n.get("buildings.monsters.reignofnether.graveyard.upgraded");
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new GraveyardPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
    }

    public Faction getFaction() {return Faction.MONSTERS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
            name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/mossy_stone_bricks.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.GRAVEYARD,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.MAUSOLEUM) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.graveyard"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.graveyard.tooltip1"), Style.EMPTY)
            ),
            this
        );
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        for (BuildingBlock block : placement.getBlocks())
            if (block.getBlockState().getBlock() == Blocks.DEEPSLATE_TILES ||
                block.getBlockState().getBlock() == Blocks.CHISELED_DEEPSLATE) {
                return 1;
            }
        return 0;
    }

    @Override
    public String getUpgradedStructureName(int upgradeLevel) {
        return upgradeLevel > 0 ? upgradedStructureName : structureName;
    }
}
