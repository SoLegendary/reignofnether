package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.BuildingProductionServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

// used for Custom buildings, eg. spawn eggs in chests
// we can't use the original ProductionItems items since we can't modify their costs
public class CustomProductionItem extends ProductionItem {

    public final String itemName;
    public final BiFunction<ProductionPlacement, Keybinding, StartProductionButton> getStartButton;
    public final BiFunction<ProductionPlacement, Boolean, StopProductionButton> getCancelButton;
    private List<FormattedCharSequence> newTooltip;
    private boolean useOriginalTitle;

    public CustomProductionItem(ResourceCost cost, String itemName, ProductionItem productionItem, List<FormattedCharSequence> newTooltip, boolean useOriginalTitle) {
        super(cost, productionItem.dupeRule, productionItem.onComplete);
        this.itemName = itemName;
        this.getStartButton = productionItem::getStartButton;
        this.getCancelButton = productionItem::getCancelButton;
        this.newTooltip = newTooltip;
        this.useOriginalTitle = useOriginalTitle;
    }

    public String getItemName() {
        return itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding keybinding) {
        StartProductionButton button = getStartButton.apply(prodBuilding, keybinding);
        button.onLeftClick = () -> BuildingProductionServerboundPacket.startProduction(this);
        if (!newTooltip.isEmpty() && !button.tooltipLines.isEmpty()) {
            if (useOriginalTitle) {
                newTooltip.set(0, button.tooltipLines.get(0));
            }
            button.tooltipLines = newTooltip;
        }
        return button;
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        StopProductionButton button = getCancelButton.apply(prodBuilding, first);
        button.onLeftClick = () -> BuildingProductionServerboundPacket.cancelProduction(prodBuilding.originPos, this, first);
        if (!newTooltip.isEmpty()) {
            if (useOriginalTitle && button.tooltipLines != null) {
                newTooltip.set(0, button.tooltipLines.get(0));
            }
            button.tooltipLines = List.of(newTooltip.get(0));
        }
        return button;
    }
}
