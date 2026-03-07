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
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sandbox.SandboxAction;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class SkeletonProd extends ProductionItem {

    public final static String itemName = "Skeleton";
    public final static ResourceCost cost = ResourceCosts.SKELETON;

    public SkeletonProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide()) {
                if (ResearchServerEvents.playerHasResearch(placement.ownerName, ProductionItems.RESEARCH_STRAYS))
                    placement.produceUnit((ServerLevel) level, EntityRegistrar.STRAY_UNIT.get(), placement.ownerName, true);
                else
                    placement.produceUnit((ServerLevel) level, EntityRegistrar.SKELETON_UNIT.get(), placement.ownerName, true);
            }
        };
    }

    public String getItemName() {
        return SkeletonProd.itemName;
    }

    private static ResourceLocation getIcon() {
        if (ResearchClient.hasResearch(ProductionItems.RESEARCH_STRAYS))
            return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/stray.png");
        else
            return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/skeleton.png");
    }

    private static String getCancelName() {
        if (ResearchClient.hasResearch(ProductionItems.RESEARCH_STRAYS))
            return "Stray";
        else
            return "Skeleton";
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                itemName,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/skeleton.png"),
                List.of(
                        FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.skeleton"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.skeleton.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.skeleton.tooltip2"), Style.EMPTY)
                )
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
            SkeletonProd.itemName,
            getIcon(),
            hotkey,
            () -> ResearchClient.hasResearch(ProductionItems.RESEARCH_STRAYS),
            () -> true,
            List.of(
                FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.skeleton"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.skeleton.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.skeleton.tooltip2"), Style.EMPTY)
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            getCancelName(),
            getIcon(),
            prodBuilding,
            this,
            first
        );
    }
}
