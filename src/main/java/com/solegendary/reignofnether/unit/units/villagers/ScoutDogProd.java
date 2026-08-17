package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.buildings.placements.TownCentrePlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class ScoutDogProd extends ProductionItem {

    public final static String itemName = "Scout Dog";
    public final static ResourceCost cost = ResourceCosts.SCOUT_DOG;

    private final static ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/scout_dog.png");

    public ScoutDogProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level, EntityRegistrar.SCOUT_DOG_UNIT.get(), placement.ownerName, true);
        };
    }

    public String getItemName() {
        return ScoutDogProd.itemName;
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                itemName,
                TEXTURE_LOCATION,
                List.of(
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.scout_dog_unit"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.scout_dog_unit.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.scout_dog_unit.tooltip2"), Style.EMPTY)
                )
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        StartProductionButton button = new StartProductionButton(
                ScoutDogProd.itemName,
                TEXTURE_LOCATION,
                hotkey,
                () -> !FogOfWarClientEvents.isEnabled() || prodBuilding instanceof TownCentrePlacement tcp && !tcp.trainsDogs,
                () -> true,
                List.of(
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.scout_dog_unit"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedPopAndTime(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.scout_dog_unit.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.scout_dog_unit.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.scout_dog_unit.tooltip3"), Style.EMPTY)
                ),
                this
        );
        button.onRightClick = () -> {
            if (prodBuilding instanceof TownCentrePlacement tcp) {
                tcp.trainsDogs = !tcp.trainsDogs;
                tcp.updateButtons();
            }
        };
        return button;
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            ScoutDogProd.itemName,
            TEXTURE_LOCATION,
            prodBuilding,
            this,
            first
        );
    }
}
