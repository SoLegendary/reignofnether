package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.ability.abilities.ForgeChestplate;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class Blacksmith extends ProductionBuilding {

    public final static String buildingName = "Blacksmith";
    public final static String structureName = "blacksmith";
    public final static String upgradedStructureNameLeather = "blacksmith_leather";
    public final static String upgradedStructureNameIron = "blacksmith_iron";
    public final static ResourceCost cost = ResourceCosts.BLACKSMITH;

    public Blacksmith() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.SMITHING_TABLE;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/smithing_table_front.png");

        this.buildTimeModifier = 0.85f;

        this.startingBlockTypes.add(Blocks.OAK_PLANKS);
        this.startingBlockTypes.add(Blocks.COBBLESTONE);

        this.abilities.add(new ForgeChestplate(), Keybindings.keyY);

        this.productions.add(ProductionItems.IRON_GOLEM, Keybindings.keyQ);
        this.productions.add(ProductionItems.RESEARCH_GOLEM_SMITHING, Keybindings.keyW);
        this.productions.add(ProductionItems.RESEARCH_MILITIA_BOWS, Keybindings.keyE);
        this.productions.add(ProductionItems.RESEARCH_BLACKSMITH_LEATHER_ARMOR, Keybindings.keyR);
        this.productions.add(ProductionItems.RESEARCH_BLACKSMITH_IRON_ARMOR, Keybindings.keyT);
    }

    public Faction getFaction() {return Faction.VILLAGERS;}

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        // Tier detection uses marker blocks (we inject these when upgrading, and on load).
        // - Tier 2: iron marker
        // - Tier 1: copper marker
        for (BuildingBlock block : placement.getBlocks()) {
            BlockState bs = block.getBlockState();
            if (bs.getBlock() == Blocks.IRON_BLOCK) {
                return 2;
            }
        }
        for (BuildingBlock block : placement.getBlocks()) {
            BlockState bs = block.getBlockState();
            if (bs.getBlock() == Blocks.COPPER_BLOCK) {
                return 1;
            }
        }
        return 0;
    }

    public static void applyTierMarker(BuildingPlacement placement, int tier) {
        BlockState marker = (tier >= 2 ? Blocks.IRON_BLOCK : Blocks.COPPER_BLOCK).defaultBlockState();

        // Prefer swapping a common floor block to keep the structure intact.
        for (BuildingBlock block : placement.getBlocks()) {
            if (block.getBlockState().getBlock() == Blocks.OAK_PLANKS) {
                block.setBlockState(marker);
                return;
            }
        }
        for (BuildingBlock block : placement.getBlocks()) {
            if (block.getBlockState().getBlock() == Blocks.COBBLESTONE) {
                block.setBlockState(marker);
                return;
            }
        }
        for (BuildingBlock block : placement.getBlocks()) {
            if (!block.getBlockState().isAir()) {
                block.setBlockState(marker);
                return;
            }
        }
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/smithing_table_front.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.BLACKSMITH,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.ATTACK_ENEMY_BASE),
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.BARRACKS) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.blacksmith"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.blacksmith.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.blacksmith.tooltip2"), Style.EMPTY)
                ),
                this
        );
    }
}
