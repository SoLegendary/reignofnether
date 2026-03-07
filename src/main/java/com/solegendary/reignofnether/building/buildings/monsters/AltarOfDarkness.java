package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class AltarOfDarkness extends ProductionBuilding {

    public final static String buildingName = "Altar of Darkness";
    public final static String structureName = "altar_of_darkness";
    public final static ResourceCost cost = ResourceCosts.ALTAR_OF_DARKNESS;

    public AltarOfDarkness() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.DARK_PRISMARINE;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/dark_prismarine.png");

        this.startingBlockTypes.add(Blocks.DARK_PRISMARINE);
        this.startingBlockTypes.add(Blocks.TINTED_GLASS);

        this.productions.add(ProductionItems.NECROMANCER, Keybindings.keyQ);
        this.productions.add(ProductionItems.NECROMANCER_REVIVE, Keybindings.keyQ);
        this.productions.add(ProductionItems.WRETCHED_WRAITH, Keybindings.keyW);
        this.productions.add(ProductionItems.WRETCHED_WRAITH_REVIVE, Keybindings.keyW);
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        ProductionPlacement pp = (ProductionPlacement) super.createBuildingPlacement(level, pos, rotation, ownerName);
        pp.allowProdWhileBuilding = true;
        return pp;
    }

    public Faction getFaction() {return Faction.MONSTERS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/dark_prismarine.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.ALTAR_OF_DARKNESS,
                () -> !SandboxClientEvents.isSandboxPlayer() && GameruleClient.allowedHeroes <= 0,
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.MAUSOLEUM) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        fcs(I18n.get("buildings.monsters.reignofnether.altar_of_darkness"), true),
                        ResourceCosts.getFormattedCost(cost),
                        fcs(""),
                        fcs(I18n.get("buildings.monsters.reignofnether.altar_of_darkness.tooltip1")),
                        fcs(I18n.get("buildings.monsters.reignofnether.altar_of_darkness.tooltip2"))
                ),
                this
        );
    }
}
