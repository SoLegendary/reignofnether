package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.CentralPortalPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.building.production.IUnitProductionItem;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
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
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class BabyHoglinProd extends ProductionItem implements IUnitProductionItem {

    public final static String itemName = "Baby Hoglin";
    public final static ResourceCost cost = ResourceCosts.BABY_HOGLIN;

    private final static ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/hoglin.png");

    public BabyHoglinProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level, EntityRegistrar.BABY_HOGLIN_UNIT.get(), placement.ownerName, true);
        };
    }

    public String getItemName() {
        return BabyHoglinProd.itemName;
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                itemName,
                TEXTURE_LOCATION,
                List.of(
                        Component.translatable("entity.reignofnether.baby_hoglin_unit").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        FormattedCharSequence.EMPTY,
                        Component.translatable("entity.reignofnether.baby_hoglin_unit.tooltip1").getVisualOrderText(),
                        Component.translatable("entity.reignofnether.baby_hoglin_unit.tooltip2").getVisualOrderText()
                )
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        StartProductionButton button = new StartProductionButton(
            BabyHoglinProd.itemName,
            TEXTURE_LOCATION,
            hotkey,
            () -> !FogOfWarClientEvents.isEnabled() ||
                    prodBuilding instanceof CentralPortalPlacement cpp && cpp.trainsStriders,
            () -> true,
            List.of(
                Component.translatable("entity.reignofnether.baby_hoglin_unit").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                fcs(""),
                Component.translatable("entity.reignofnether.baby_hoglin_unit.tooltip1").getVisualOrderText(),
                Component.translatable("entity.reignofnether.baby_hoglin_unit.tooltip2").getVisualOrderText(),
                fcs(""),
                Component.translatable("entity.reignofnether.baby_hoglin_unit.tooltip3").getVisualOrderText()
            ),
            this
        );
        button.onRightClick = () -> {
            if (prodBuilding instanceof CentralPortalPlacement cpp) {
                cpp.trainsStriders = !cpp.trainsStriders;
                cpp.updateButtons();
            }
        };
        return button;
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            BabyHoglinProd.itemName,
            TEXTURE_LOCATION,
            prodBuilding,
            this,
            first
        );
    }
}
