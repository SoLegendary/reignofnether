package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchResourceCapacity extends ProductionItem {

    public final static String itemName = "Worker Carry Bags";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_RESOURCE_CAPACITY;

    public ResearchResourceCapacity() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (level.isClientSide()) {
                ResearchClient.addResearch(placement.ownerName, ProductionItems.RESEARCH_RESOURCE_CAPACITY);
                for (LivingEntity unit : UnitClientEvents.getAllUnits())
                    if (unit instanceof WorkerUnit) {
                        ((Unit) unit).setupEquipmentAndUpgradesClient();
                    }
            } else {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_RESOURCE_CAPACITY);
                for (LivingEntity unit : UnitServerEvents.getAllUnits())
                    if (unit instanceof WorkerUnit) {
                        ((Unit) unit).setupEquipmentAndUpgradesServer();
                    }
            }
        };
    }

    public String getItemName() {
        return ResearchResourceCapacity.itemName;
    }

    public Button getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new Button(ResearchResourceCapacity.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/chest.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> false,
            () -> ProductionItems.RESEARCH_RESOURCE_CAPACITY.itemIsBeingProduced(prodBuilding.ownerName)
                || ResearchClient.hasResearch(ProductionItems.RESEARCH_RESOURCE_CAPACITY),
            () -> true,
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, ProductionItems.RESEARCH_RESOURCE_CAPACITY),
            null,
            List.of(FormattedCharSequence.forward(I18n.get("research.reignofnether.resource_capacity"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.resource_capacity.tooltip1"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward(I18n.get("research.reignofnether.resource_capacity.tooltip2"),
                    Style.EMPTY
                )
            )
        );
    }

    public Button getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new Button(ResearchResourceCapacity.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/chest.png"),
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            null,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.cancelProduction(prodBuilding.minCorner, ProductionItems.RESEARCH_RESOURCE_CAPACITY, first),
            null,
            null
        );
    }
}
