package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.shared.AbstractMarket;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.items.StockedShopItem;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class MonsterMarket extends AbstractMarket {

    public static final String buildingName = "Conversion Crucible";
    public static final String structureName = "market_monsters1";
    public static final String upgradedStructureName = "market_monsters2";
    public static final ResourceCost cost = ResourceCosts.MONSTER_MARKET;

    public MonsterMarket() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.POLISHED_DEEPSLATE;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/polished_deepslate.png");

        this.startingBlockTypes.add(Blocks.POLISHED_DEEPSLATE);
        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE_BRICKS);
        this.startingBlockTypes.add(Blocks.DEEPSLATE_TILES);
        this.startingBlockTypes.add(Blocks.SMOOTH_BASALT);
        this.startingBlockTypes.add(Blocks.POLISHED_BASALT);
        this.startingBlockTypes.add(Blocks.DEEPSLATE_TILE_SLAB);
        this.startingBlockTypes.add(Blocks.POLISHED_DEEPSLATE_SLAB);
        this.startingBlockTypes.add(Blocks.DARK_PRISMARINE_SLAB);

        this.productions.add(ProductionItems.RESEARCH_MARKET_UPGRADE_MONSTERS, Keybindings.abilitySlot4);
    }

    @Override
    public String getUpgradedStructureName(int upgradeLevel) {
        return upgradeLevel > 0 ? upgradedStructureName : structureName;
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        for (BuildingBlock block : placement.getBlocks())
            if (block.getBlockState().getBlock() == Blocks.BREWING_STAND) {
                return 1;
            }
        return 0;
    }

    @Override
    protected ArrayList<StockedShopItem> getStartingItemsAndStock() {
        return new ArrayList<>(List.of(
                new StockedShopItem(UnitItems.HEALTH_POTION, 1, 60 * 20),
                new StockedShopItem(UnitItems.MANA_POTION, 1, 60 * 20)
        ));
    }

    public Faction getFaction() { return Faction.MONSTERS; }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/polished_deepslate.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.EXPLAIN_BUILDINGS),
                () -> true,
                List.of(
                        fcs(I18n.get("buildings.reignofnether.monster_market"), true),
                        ResourceCosts.getFormattedCost(cost),
                        fcs(""),
                        fcs(I18n.get("buildings.reignofnether.monster_market.tooltip1")),
                        fcs(""),
                        fcs(I18n.get("buildings.reignofnether.monster_market.tooltip2"))
                ),
                this
        );
    }
}