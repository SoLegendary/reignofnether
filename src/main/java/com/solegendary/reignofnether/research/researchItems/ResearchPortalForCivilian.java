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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ResearchPortalForCivilian extends ProductionItem {

    public final static String itemName = "Civilian Portal";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_CIVILIAN_PORTAL;

    public ResearchPortalForCivilian() {
        super(cost, ProdDupeRule.DISALLOW_FOR_BUILDING);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (placement instanceof PortalPlacement portal) {
                if (!level.isClientSide()) {
                    portal.changePortalStructure(PortalPlacement.PortalType.CIVILIAN);
                    BuildingClientboundPacket.changePortal(placement.originPos, PortalPlacement.PortalType.CIVILIAN);
                }
            }
        };
    }

    public String getItemName() {
        return ResearchPortalForCivilian.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartPortalTransformButton(ResearchPortalForCivilian.itemName,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/cyan_glazed_terracotta.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> prodBuilding.productionQueue.size() > 0 || (
                prodBuilding instanceof PortalPlacement portal && portal.getUpgradeLevel() > 0
            ),
            () -> true,
            List.of(fcs(I18n.get("research.reignofnether.civilian_portal"), true),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                fcs(""),
                fcs(I18n.get("research.reignofnether.civilian_portal.tooltip1")),
                fcs(I18n.get("research.reignofnether.civilian_portal.tooltip2"))
            ),
            prodBuilding,
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(ResearchPortalForCivilian.itemName,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/cyan_glazed_terracotta.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            prodBuilding,
            this,
            first
        );
    }
}
