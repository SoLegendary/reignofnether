package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProdDupeRule;
import com.solegendary.reignofnether.building.production.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchPortalForMilitary extends ProductionItem {

    public final static String itemName = "Military Portal";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_MILITARY_PORTAL;

    public ResearchPortalForMilitary() {
        super(cost, ProdDupeRule.DISALLOW_FOR_BUILDING);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (placement instanceof PortalPlacement portal) {
                if (!level.isClientSide()) {
                    portal.changePortalStructure(PortalPlacement.PortalType.MILITARY);
                    BuildingClientboundPacket.changePortal(placement.originPos, PortalPlacement.PortalType.MILITARY);
                }
            }
        };
    }

    public String getItemName() {
        return ResearchPortalForMilitary.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartPortalTransformButton(ResearchPortalForMilitary.itemName,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/red_glazed_terracotta.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> !prodBuilding.productionQueue.isEmpty() || (
                prodBuilding instanceof PortalPlacement portal && portal.getUpgradeLevel() > 0
            ),
            () -> true,
            List.of(FormattedCharSequence.forward(
                    I18n.get("research.reignofnether.military_portal"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.military_portal.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.military_portal.tooltip2"), Style.EMPTY)
            ),
            prodBuilding,
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(ResearchPortalForMilitary.itemName,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/red_glazed_terracotta.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            prodBuilding,
            this,
            first
        );
    }
}
