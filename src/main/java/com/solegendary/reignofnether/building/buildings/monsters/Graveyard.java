package com.solegendary.reignofnether.building.buildings.monsters;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

import com.solegendary.reignofnether.ability.abilities.SetGraveyardReleaseOff;
import com.solegendary.reignofnether.ability.abilities.SetGraveyardReleaseOn;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.GraveyardPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

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
        this.maxHealth = 120d;

        this.abilities.add(new SetGraveyardReleaseOff(), Keybindings.abilitySlot9);
        this.abilities.add(new SetGraveyardReleaseOn(), Keybindings.abilitySlot9);

        this.productions.add(ProductionItems.ZOMBIE, Keybindings.abilitySlot1);
        this.productions.add(ProductionItems.HUSK, Keybindings.abilitySlot1);
        this.productions.add(ProductionItems.DROWNED, Keybindings.abilitySlot2);
        this.productions.add(ProductionItems.SKELETON, Keybindings.abilitySlot3);
        this.productions.add(ProductionItems.STRAY, Keybindings.abilitySlot3);
        this.productions.add(ProductionItems.BOGGED, Keybindings.abilitySlot4);
        this.productions.add(ProductionItems.RESEARCH_OVERFLOWING_GRAVEYARD, Keybindings.abilitySlot5);
    }

    @Override
    public String getUpgradedName(BuildingPlacement placement) {
        return Component.translatable("buildings.reignofnether.graveyard.upgraded").getString();
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new GraveyardPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
    }


    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(
            name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/mossy_stone_bricks.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.GRAVEYARD,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.MAUSOLEUM) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(
                Component.translatable("buildings.reignofnether.graveyard").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.graveyard.tooltip1").getVisualOrderText()
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
