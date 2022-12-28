package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServer;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.units.villagers.VindicatorUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchVindicatorAxes extends ProductionItem {

    public final static String itemName = "Diamond Axes";

    public ResearchVindicatorAxes(ProductionBuilding building) {
        super(building, ResourceCosts.ResearchVindicatorAxes.TICKS);
        this.onComplete = (Level level) -> {
            if (level.isClientSide())
                ResearchClient.addResearch(ResearchVindicatorAxes.itemName);
            else {
                ResearchServer.addResearch(this.building.ownerName, ResearchVindicatorAxes.itemName);
                for (LivingEntity unit : UnitServerEvents.getAllUnits())
                    if (unit instanceof VindicatorUnit vUnit)
                        vUnit.setupEquipmentAndUpgradesServer();
            }
        };
        this.foodCost = ResourceCosts.ResearchVindicatorAxes.FOOD;
        this.woodCost = ResourceCosts.ResearchVindicatorAxes.WOOD;
        this.oreCost = ResourceCosts.ResearchVindicatorAxes.ORE;
    }

    public String getItemName() {
        return ResearchVindicatorAxes.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {
        return new Button(
                ResearchVindicatorAxes.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/diamond_axe.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> false,
                () -> ProductionItem.itemIsBeingProduced(ResearchVindicatorAxes.itemName) ||
                        ResearchClient.hasResearch(ResearchVindicatorAxes.itemName),
                () -> true,
                () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
                null,
                List.of(
                        FormattedCharSequence.forward(ResearchVindicatorAxes.itemName, Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.ResearchVindicatorAxes.WOOD + "     \uE002  " + ResourceCosts.ResearchVindicatorAxes.ORE, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("\uE004  " + ResourceCosts.ResearchVindicatorAxes.TICKS/20 + "s", MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("Upgrades the axes of all vindicators to diamond (+2 damage)", Style.EMPTY)
                )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
                ResearchVindicatorAxes.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/diamond_axe.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> BuildingServerboundPacket.cancelProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName, first),
                null,
                null
        );
    }
}
