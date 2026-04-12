package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
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
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Bastion extends ProductionBuilding implements GarrisonableBuildingAddon {
    public final static int MAX_OCCUPANTS = 4;

    public final static String buildingName = "Bastion";
    public final static String structureName = "bastion";
    public final static ResourceCost cost = ResourceCosts.BASTION;

    public Bastion() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.CHISELED_POLISHED_BLACKSTONE;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/chiseled_polished_blackstone.png");

        this.canSetRallyPoint = false;

        this.buildTimeModifier = 0.72f;

        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE);
        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE_BRICKS);
        this.startingBlockTypes.add(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);

        this.explodeChance = 0.2f;

        this.productions.add(ProductionItems.RESEARCH_BRUTE_SHIELDS, Keybindings.keyQ);
        this.productions.add(ProductionItems.RESEARCH_HEAVY_TRIDENTS, Keybindings.keyW);
        this.productions.add(ProductionItems.RESEARCH_CLEAVING_FLAILS, Keybindings.keyE);

        setActiveAddon(GarrisonableBuildingAddon.class, this, true);
    }

    public Faction getFaction() {return Faction.PIGLINS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/chiseled_polished_blackstone.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.BASTION,
                () -> false,
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.PORTAL_BASIC) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.bastion"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.bastion.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.bastion.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.bastion.tooltip3", MAX_OCCUPANTS), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.bastion.tooltip4"), Style.EMPTY)
                ),
                this
        );
    }

    // don't use this for abilities as it may not be balanced
    public int getAttackRange() { return 24; }
    // bonus for units attacking garrisoned units
    public int getExternalAttackRangeBonus() { return 10; }

    public boolean canDestroyBlock(BlockPos relativeBp) {
        return relativeBp.getY() != 10 &&
                relativeBp.getY() != 11;
    }

    @Override
    public BlockPos getEntryPosition(BuildingPlacement placement) {
        return placement.originPos.offset(GarrisonableBuildingAddon.rotatePos(new BlockPos(2,11,2), placement.rotation));
    }

    @Override
    public BlockPos getExitPosition(BuildingPlacement placement) {
        return placement.originPos.offset(GarrisonableBuildingAddon.rotatePos(new BlockPos(2,1,2), placement.rotation));
    }

    @Override
    public int getCapacity() { return MAX_OCCUPANTS; }
}
