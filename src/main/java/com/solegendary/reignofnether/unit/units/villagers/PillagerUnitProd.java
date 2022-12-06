package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class PillagerUnitProd extends ProductionItem {

    public final static String itemName = "Pillager";

    public PillagerUnitProd(ProductionBuilding building) {
        super(building, ResourceCosts.Pillager.TICKS);
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.PILLAGER_UNIT.get(), building.ownerName);
        };
        this.foodCost = ResourceCosts.Pillager.FOOD;
        this.woodCost = ResourceCosts.Pillager.WOOD;
        this.oreCost = ResourceCosts.Pillager.ORE;
        this.popCost = ResourceCosts.Pillager.POPULATION;
    }

    public String getItemName() {
        return PillagerUnitProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding) {
        return new Button(
            "Pillager",
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/pillager.png"),
            Keybindings.keyW,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.startProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName),
            null,
            List.of(
                FormattedCharSequence.forward("Pillager", Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("\uE000  " + ResourceCosts.Pillager.FOOD + "     \uE001  " + ResourceCosts.Pillager.WOOD, MyRenderer.iconStyle),
                FormattedCharSequence.forward("\uE003  " + ResourceCosts.Pillager.POPULATION + "     \uE004 " + ResourceCosts.Pillager.TICKS/20 + "s", MyRenderer.iconStyle),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("A villager armed with a crossbow for ranged combat.", Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            "Pillager",
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/pillager.png"),
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
