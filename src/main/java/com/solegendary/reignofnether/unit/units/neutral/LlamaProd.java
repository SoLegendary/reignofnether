package com.solegendary.reignofnether.unit.units.neutral;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.UnitProductionItem;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class LlamaProd extends ProductionItem implements UnitProductionItem {

    public final static String itemName = "Llama";
    public final static ResourceCost cost = ResourceCosts.LLAMA;

    public LlamaProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide()) {
                placement.produceUnit((ServerLevel) level, EntityRegistrar.LLAMA_UNIT.get(), placement.ownerName, true);
            }
        };
    }

    public String getItemName() {
        return LlamaProd.itemName;
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
            itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/llama.png"),
            List.of(
                    Component.translatable("entity.reignofnether.llama_unit").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText()
            )
        );
    }
}
