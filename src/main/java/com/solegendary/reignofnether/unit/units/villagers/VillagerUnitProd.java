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

public class VillagerUnitProd extends ProductionItem {

    public final static String itemName = "Villager";

    public VillagerUnitProd(ProductionBuilding building) {
        super(building, ResourceCosts.Villager.TICKS);
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.VILLAGER_UNIT.get(), building.ownerName);
        };
        this.foodCost = ResourceCosts.Villager.FOOD;
        this.woodCost = ResourceCosts.Villager.WOOD;
        this.oreCost = ResourceCosts.Villager.ORE;
        this.popCost = ResourceCosts.Villager.POPULATION;
    }

    public String getItemName() {
        return VillagerUnitProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding) {
        return new Button(
            "Villager",
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
            Keybindings.keyV,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.startProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName),
            null,
            List.of(
                FormattedCharSequence.forward("Villager", Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("\uE000  " + ResourceCosts.Villager.FOOD, MyRenderer.iconStyle),
                FormattedCharSequence.forward("\uE003  " + ResourceCosts.Villager.POPULATION + "     \uE004 " + ResourceCosts.Villager.TICKS/20 + "s", MyRenderer.iconStyle),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("A worker unit that can construct or repair buildings and gather resources.", Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            "Villager",
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
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