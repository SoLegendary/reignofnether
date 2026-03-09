package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.ability.abilities.CallLightning;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.NightSourceAddon;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Laboratory extends ProductionBuilding implements RangeIndicatorAddon {

    public final static String buildingName = "Laboratory";
    public final static String structureName = "laboratory";
    public final static String upgradedStructureName = "laboratory_lightning";
    public final static ResourceCost cost = ResourceCosts.LABORATORY;

    public Laboratory() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.BREWING_STAND;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/brewing_stand.png");

        this.buildTimeModifier = 0.85f;

        this.canSetRallyPoint = false;

        this.startingBlockTypes.add(Blocks.SPRUCE_PLANKS);
        this.startingBlockTypes.add(Blocks.BLACKSTONE);

        this.abilities.add(new CallLightning(), Keybindings.keyL);

        this.productions.add(ProductionItems.RESEARCH_HUSKS, Keybindings.keyQ);
        this.productions.add(ProductionItems.RESEARCH_DROWNED, Keybindings.keyW);
        this.productions.add(ProductionItems.RESEARCH_STRAYS, Keybindings.keyE);
        this.productions.add(ProductionItems.RESEARCH_BOGGED, Keybindings.keyR);
        this.productions.add(ProductionItems.RESEARCH_SPIDER_JOCKEYS, Keybindings.keyT);
        this.productions.add(ProductionItems.RESEARCH_POISON_SPIDERS, Keybindings.keyY);
        this.productions.add(ProductionItems.RESEARCH_SPIDER_WEBS, Keybindings.keyU);
        this.productions.add(ProductionItems.RESEARCH_SLIME_CONVERSION, Keybindings.keyI);
        this.productions.add(ProductionItems.RESEARCH_LAB_LIGHTNING_ROD, Keybindings.keyO);
        this.productions.add(ProductionItems.RESEARCH_SILVERFISH, Keybindings.keyP);
        this.productions.add(ProductionItems.RESEARCH_SCULK_AMPLIFIERS, Keybindings.keyJ);

        setActiveAddon(RangeIndicatorAddon.class, this, true);
    }

    public Faction getFaction() {return Faction.MONSTERS;}

    // return the lightning rod is built based on existing placed blocks
    // returns null if it is not build or is damaged
    // also will return null if outside of render range, but shouldn't matter since it'd be out of ability range anyway
    public BlockPos getLightningRodPos(BuildingPlacement placement) {
        for (BuildingBlock block : placement.getBlocks()) {
            if (placement.getLevel().getBlockState(block.getBlockPos()).getBlock() == Blocks.LIGHTNING_ROD &&
                placement.getLevel().getBlockState(block.getBlockPos().below()).getBlock() == Blocks.WAXED_COPPER_BLOCK)
                return block.getBlockPos();
        }
        return null;
    }

    // check that the lightning rod is built based on existing placed blocks
    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        for (BuildingBlock block : placement.getBlocks())
            if (block.getBlockState().getBlock() == Blocks.LIGHTNING_ROD)
                return 1;
        return 0;
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
            name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/brewing_stand.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.LABORATORY,
            () -> false,
            () -> (BuildingClientEvents.hasFinishedBuilding(Buildings.MAUSOLEUM) &&
                    BuildingClientEvents.hasFinishedBuilding(Buildings.GRAVEYARD)) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.laboratory"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.laboratory.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.laboratory.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.laboratory.tooltip3"), Style.EMPTY)
            ),
                this
        );
    }

    @Override
    public String getUpgradedStructureName(int upgradeLevel) {
        return upgradeLevel > 0 ? upgradedStructureName : structureName;
    }

    @Override
    public void tick(Level tickLevel, BuildingPlacement buildingPlacement) {
        super.tick(tickLevel, buildingPlacement);
        if (tickLevel.isClientSide && buildingPlacement.getTickAgeAfterBuilt() > 0 && buildingPlacement.getTickAgeAfterBuilt() % 100 == 0)
            updateHighlightBps(buildingPlacement);
    }

    @Override
    public int getRange(BuildingPlacement placement) {
        return (getUpgradeLevel(placement) > 0 && placement.isBuilt) ? CallLightning.RANGE : 0;
    }

    @Override
    public void updateHighlightBps(BuildingPlacement placement) {
        if (!placement.level.isClientSide())
            return;
        placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE).clear();
        placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE).addAll(MiscUtil.getRangeIndicatorCircleBlocks(placement.centrePos,
                getRange(placement) - BlockClientEvents.VISIBLE_BORDER_ADJ, placement.level, hasActiveAddon(NightSourceAddon.class)));
    }

    @Override
    public boolean showOnlyWhenSelected(BuildingPlacement placement) {
        return true;
    }
}
