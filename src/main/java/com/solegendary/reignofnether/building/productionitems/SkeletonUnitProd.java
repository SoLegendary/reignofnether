package com.solegendary.reignofnether.building.productionitems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.PopulationCosts;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class SkeletonUnitProd extends ProductionItem {

    public final static String itemName = "Skeleton";

    public SkeletonUnitProd(ProductionBuilding building) {
        super(building, 100);
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.SKELETON_UNIT.get(), building.ownerName);
        };
        this.foodCost = 60;
        this.woodCost = 40;
        this.oreCost = 0;
        this.popCost = PopulationCosts.SKELETON;
    }

    public String getItemName() {
        return SkeletonUnitProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding) {
        return new Button(
            "Skeleton",
            14,
            "textures/mobheads/skeleton.png",
            Keybinding.keyW,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.startProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName),
            List.of(
                FormattedCharSequence.forward("Skeleton", Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("\uE000  60     \uE001  40", MyRenderer.iconStyle),
                FormattedCharSequence.forward("\uE003  1     \uE004  5s", MyRenderer.iconStyle),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("An undead soldier with a bow and arrows.", Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            "Skeleton",
            14,
            "textures/mobheads/skeleton.png",
            (KeyMapping) null,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.cancelProduction(BuildingUtils.getMinCorner(prodBuilding.getBlocks()), itemName, first),
            null
        );
    }
}
