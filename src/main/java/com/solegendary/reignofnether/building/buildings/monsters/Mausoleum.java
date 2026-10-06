package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.addon.NightSourceAddon;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.MiscUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class Mausoleum extends ProductionBuilding implements NightSourceAddon, RangeIndicatorAddon {

    public final static String buildingName = "Mausoleum";
    public final static String structureName = "mausoleum";
    public final static ResourceCost cost = ResourceCosts.MAUSOLEUM;
    public final static int nightRange = 60;
    public final static int nightRangeReduced = 40;

    public Mausoleum() {
        super(structureName, cost, true);
        this.name = buildingName;
        this.portraitBlock = Blocks.DEEPSLATE_TILES;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/deepslate_tiles.png");
        this.foundationYLayers = 4;

        this.buildTimeModifier = 0.274f; // 60s total build time with 3 villagers
        this.canAcceptResources = true;
        this.maxHealth = 460d;

        this.startingBlockTypes.add(Blocks.STONE);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);
        this.startingBlockTypes.add(Blocks.STONE_BRICKS);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);

        this.productions.add(ProductionItems.ZOMBIE_VILLAGER, Keybindings.abilitySlot1);
        this.productions.add(ProductionItems.BAT, Keybindings.abilitySlot2);

        setActiveAddon(RangeIndicatorAddon.class, this, true);
        setActiveAddon(NightSourceAddon.class, this, true);
    }

    

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/deepslate_tiles.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.MAUSOLEUM,
            () -> false,
            () -> true,
            List.of(Component.translatable("buildings.reignofnether.mausoleum").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPop(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.mausoleum.tooltip1").getVisualOrderText(),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.mausoleum.tooltip2", nightRange).getVisualOrderText(),
                Component.translatable("buildings.reignofnether.mausoleum.tooltip3", nightRangeReduced).getVisualOrderText()
            ),
            this
        );
    }

    @Override
    public void tick(Level tickLevel, BuildingPlacement buildingPlacement) {
        super.tick(tickLevel, buildingPlacement);
        if (tickLevel.isClientSide && buildingPlacement.getTickAgeAfterBuilt() > 0 && buildingPlacement.getTickAgeAfterBuilt() % 100 == 0)
            updateHighlightBps(buildingPlacement);
    }

    @Override
    public int getRange(BuildingPlacement placement) {
        List<BuildingPlacement> bpls;
        if (placement.level.isClientSide())
            bpls = BuildingClientEvents.getBuildings();
        else
            bpls = BuildingServerEvents.getBuildings();

        int nRange = nightRangeReduced;
        long oldestMausoleumAge = 0;
        for (BuildingPlacement bpl : bpls)
            if (bpl.getBuilding().isTypeOf(this) && bpl.ownerName.equals(placement.ownerName))
                if (bpl.tickAge > oldestMausoleumAge)
                    oldestMausoleumAge = bpl.tickAge;
        if (placement.tickAge >= oldestMausoleumAge)
            nRange = nightRange;
        return placement.isBuilt ? nRange : 0;
    }

    @Override
    public int getNightRange(BuildingPlacement placement) {
        return getRange(placement);
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
        return false;
    }

    @Override
    public int getDefaultNightRange() {
        return nightRangeReduced;
    }
}