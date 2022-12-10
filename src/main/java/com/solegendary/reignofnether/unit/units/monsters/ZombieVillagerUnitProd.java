package com.solegendary.reignofnether.unit.units.monsters;

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

public class ZombieVillagerUnitProd extends ProductionItem {

    public final static String itemName = "Zombie Villager";

    public ZombieVillagerUnitProd(ProductionBuilding building) {
        super(building, ResourceCosts.ZombieVillager.TICKS);
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get(), building.ownerName, true);
        };
        this.foodCost = ResourceCosts.ZombieVillager.FOOD;
        this.woodCost = ResourceCosts.ZombieVillager.WOOD;
        this.oreCost = ResourceCosts.ZombieVillager.ORE;
        this.popCost = ResourceCosts.ZombieVillager.POPULATION;
    }

    public String getItemName() {
        return ZombieVillagerUnitProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {
        return new Button(
            "Zombie Villager",
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/zombie_villager.png"),
            hotkey,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
            null,
            List.of(
                FormattedCharSequence.forward("Zombie Villager", Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("\uE000  " + ResourceCosts.ZombieVillager.FOOD, MyRenderer.iconStyle),
                FormattedCharSequence.forward("\uE003  " + ResourceCosts.ZombieVillager.POPULATION + "     \uE004 " + ResourceCosts.ZombieVillager.TICKS/20 + "s", MyRenderer.iconStyle),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("An undead worker unit that can construct or repair buildings and gather resources.", Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            "Zombie Villager",
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/zombie_villager.png"),
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