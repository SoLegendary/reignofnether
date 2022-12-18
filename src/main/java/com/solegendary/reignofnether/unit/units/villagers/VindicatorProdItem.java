package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.researchItems.ResearchVindicatorAxes;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class VindicatorProdItem extends ProductionItem {

    public final static String itemName = "Vindicator";

    public VindicatorProdItem(ProductionBuilding building) {
        super(building, ResourceCosts.Vindicator.TICKS);
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.VINDICATOR_UNIT.get(), building.ownerName, true);
        };
        this.foodCost = ResourceCosts.Vindicator.FOOD;
        this.woodCost = ResourceCosts.Vindicator.WOOD;
        this.oreCost = ResourceCosts.Vindicator.ORE;
        this.popCost = ResourceCosts.Vindicator.POPULATION;
    }

    public String getItemName() {
        return VindicatorProdItem.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {

        List<FormattedCharSequence> tooltipLines = new ArrayList<>(List.of(
                FormattedCharSequence.forward(VindicatorProdItem.itemName, Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("\uE000  " + ResourceCosts.Vindicator.FOOD, MyRenderer.iconStyle),
                FormattedCharSequence.forward("\uE003  " + ResourceCosts.Vindicator.POPULATION + "     \uE004  " + ResourceCosts.Vindicator.TICKS / 20 + "s", MyRenderer.iconStyle),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("A villager armed with an axe for melee combat.", Style.EMPTY)
        ));
        if (ResearchClient.hasResearch(ResearchVindicatorAxes.itemName)) {
            tooltipLines.add(FormattedCharSequence.forward("", Style.EMPTY));
            tooltipLines.add(FormattedCharSequence.forward("Upgraded with diamond axes that deal +2 damage", Style.EMPTY.withBold(true)));
        }
        return new Button(
            VindicatorProdItem.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/vindicator.png"),
            hotkey,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
            null,
            tooltipLines
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            VindicatorProdItem.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/vindicator.png"),
            (Keybinding) null,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.cancelProduction(prodBuilding.originPos, itemName, first),
            null,
            null
        );
    }
}