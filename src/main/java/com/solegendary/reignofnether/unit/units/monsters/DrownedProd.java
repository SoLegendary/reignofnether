package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sandbox.SandboxAction;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.LanguageUtil;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class DrownedProd extends ProductionItem {

    public final static String itemName = "Drowned";
    public final static ResourceCost cost = ResourceCosts.DROWNED;

    public DrownedProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level, EntityRegistrar.DROWNED_UNIT.get(), placement.ownerName, true);
        };
    }

    public String getItemName() {
        return DrownedProd.itemName;
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                DrownedProd.itemName,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/drowned.png"),
                List.of(
                        FormattedCharSequence.forward(LanguageUtil.getTranslation("units.monsters.reignofnether.drowned"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(LanguageUtil.getTranslation("units.monsters.reignofnether.drowned.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(LanguageUtil.getTranslation("units.monsters.reignofnether.drowned.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(LanguageUtil.getTranslation("units.monsters.reignofnether.drowned.tooltip3"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(LanguageUtil.getTranslation("units.monsters.reignofnether.drowned.tooltip4"), Style.EMPTY)
                )
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
            DrownedProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/drowned.png"),
            hotkey,
            () -> false,
            () -> ResearchClient.hasResearch(ProductionItems.RESEARCH_DROWNED),
            List.of(
                FormattedCharSequence.forward(LanguageUtil.getTranslation("units.monsters.reignofnether.drowned"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(LanguageUtil.getTranslation("units.monsters.reignofnether.drowned.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(LanguageUtil.getTranslation("units.monsters.reignofnether.drowned.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(LanguageUtil.getTranslation("units.monsters.reignofnether.drowned.tooltip3"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(LanguageUtil.getTranslation("units.monsters.reignofnether.drowned.tooltip4"), Style.EMPTY)
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            DrownedProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/drowned.png"),
            prodBuilding,
            this,
            first
        );
    }
}
