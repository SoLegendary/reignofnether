package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class IronGolemProdItem extends ProductionItem {

    public final static String itemName = "Iron Golem";

    public IronGolemProdItem(ProductionBuilding building) {
        super(building, ResourceCosts.IronGolem.TICKS);
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.IRON_GOLEM_UNIT.get(), building.ownerName, false);
        };
        this.foodCost = ResourceCosts.IronGolem.FOOD;
        this.woodCost = ResourceCosts.IronGolem.WOOD;
        this.oreCost = ResourceCosts.IronGolem.ORE;
        this.popCost = ResourceCosts.IronGolem.POPULATION;
    }

    public String getItemName() {
        return IronGolemProdItem.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {
        return new Button(
            IronGolemProdItem.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/iron_golem.png"),
                hotkey,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
            null,
            List.of(
                FormattedCharSequence.forward(IronGolemProdItem.itemName, Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("\uE002  " + ResourceCosts.IronGolem.ORE, MyRenderer.iconStyle),
                FormattedCharSequence.forward("\uE003  " + ResourceCosts.IronGolem.POPULATION + "     \uE004  " + ResourceCosts.IronGolem.TICKS/20 + "s", MyRenderer.iconStyle),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("A hulking golem of metal with a powerful melee attack and high armour.", Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            IronGolemProdItem.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/iron_golem.png"),
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