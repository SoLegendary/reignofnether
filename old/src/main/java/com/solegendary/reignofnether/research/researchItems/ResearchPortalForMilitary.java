package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientboundPacket;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.Button;
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
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (placement instanceof PortalPlacement portal) {
                if (!level.isClientSide()) {
                    portal.changeStructure(PortalPlacement.PortalType.MILITARY);
                    BuildingClientboundPacket.changePortal(placement.originPos, PortalPlacement.PortalType.MILITARY);
                }
            }
        };
    }

    public String getItemName() {
        return ResearchPortalForMilitary.itemName;
    }

    public Button getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new Button(ResearchPortalForMilitary.itemName,
            14,
            new ResourceLocation("minecraft", "textures/block/red_glazed_terracotta.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> false,
            () -> prodBuilding.productionQueue.size() > 0 || (
                prodBuilding instanceof PortalPlacement portal && portal.getUpgradeLevel() > 0
            ),
            () -> true,
            () -> {
                if (prodBuilding.productionQueue.isEmpty())
                    BuildingServerboundPacket.startProduction(prodBuilding.originPos, ProductionItems.RESEARCH_PORTAL_FOR_MILITARY);
            },
            null,
            List.of(FormattedCharSequence.forward(
                    I18n.get("research.reignofnether.military_portal"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.military_portal.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.military_portal.tooltip2"), Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new Button(ResearchPortalForMilitary.itemName,
            14,
            new ResourceLocation("minecraft", "textures/block/red_glazed_terracotta.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            null,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.cancelProduction(prodBuilding.minCorner, ProductionItems.RESEARCH_PORTAL_FOR_MILITARY, first),
            null,
            null
        );
    }
}
