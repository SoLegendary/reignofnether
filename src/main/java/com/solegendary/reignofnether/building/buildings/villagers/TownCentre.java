package com.solegendary.reignofnether.building.buildings.villagers;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.BackToWorkBuilding;
import com.solegendary.reignofnether.ability.abilities.CallToArmsBuilding;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.addon.NightSourceAddon;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.building.buildings.placements.TownCentrePlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.MiscUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

public class TownCentre extends ProductionBuilding implements RangeIndicatorAddon {

    public final static String buildingName = "Town Centre";
    public final static String structureName = "town_centre";
    public final static ResourceCost cost = ResourceCosts.TOWN_CENTRE;

    // distance you can move away from a town centre before being turned back into a villager
    public static final int MILITIA_RANGE = 60;

    public TownCentre() {
        super(structureName, cost, true);
        this.name = buildingName;
        this.portraitBlock = Blocks.POLISHED_GRANITE;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/polished_granite.png");

        this.maxHealth = 380d;
        this.buildTimeModifier = 0.328f; // 60s total build time with 3 villagers
        this.canAcceptResources = true;

        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);
        this.startingBlockTypes.add(Blocks.GRASS_BLOCK);
        this.startingBlockTypes.add(Blocks.POLISHED_ANDESITE_STAIRS);

        Ability callToArms = new CallToArmsBuilding();
        this.abilities.add(callToArms, Keybindings.hotkey1);
        BackToWorkBuilding backToWork = new BackToWorkBuilding();
        this.abilities.add(backToWork, Keybindings.build);

        this.productions.add(ProductionItems.VILLAGER, Keybindings.abilitySlot1);
        this.productions.add(ProductionItems.SCOUT_DOG, Keybindings.abilitySlot2);
        this.productions.add(ProductionItems.SCOUT_CAT, Keybindings.abilitySlot2);

        setActiveAddon(RangeIndicatorAddon.class, this, true);
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new TownCentrePlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation));
    }


    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(
               name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/polished_granite.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.TOWN_CENTRE,
                () -> false,
                () -> true,
                List.of(
                        Component.translatable("buildings.reignofnether.town_centre").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedPop(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        Component.translatable("buildings.reignofnether.town_centre.tooltip1").getVisualOrderText()
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
        return (placement.isBuilt) ? MILITIA_RANGE : 0;
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
