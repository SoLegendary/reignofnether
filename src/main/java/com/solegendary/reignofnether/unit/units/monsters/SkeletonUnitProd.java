package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
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

public class SkeletonUnitProd extends ProductionItem {

    public final static String itemName = "Skeleton";

    public SkeletonUnitProd(ProductionBuilding building) {
        super(building, ResourceCosts.Skeleton.TICKS);
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.SKELETON_UNIT.get(), building.ownerName);
        };
        this.foodCost = ResourceCosts.Skeleton.FOOD;
        this.woodCost = ResourceCosts.Skeleton.WOOD;
        this.oreCost = ResourceCosts.Skeleton.ORE;
        this.popCost = ResourceCosts.Skeleton.POPULATION;
    }

    public String getItemName() {
        return SkeletonUnitProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding) {
        return new Button(
            "Skeleton",
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/skeleton.png"),
            Keybindings.keyW,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.startProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName),
            null,
            List.of(
                FormattedCharSequence.forward("Skeleton", Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("\uE000  " + ResourceCosts.Skeleton.FOOD + "     \uE001  " + ResourceCosts.Skeleton.WOOD, MyRenderer.iconStyle),
                FormattedCharSequence.forward("\uE003  " + ResourceCosts.Skeleton.POPULATION + "     \uE004 " + ResourceCosts.Skeleton.TICKS/20 + "s", MyRenderer.iconStyle),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("An undead soldier with a bow and arrows.", Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            "Skeleton",
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/skeleton.png"),
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
