package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchBeaconLevel1 extends ProductionItem {
    public final static ResourceCost cost = ResourceCosts.RESEARCH_BEACON_LEVEL1;

    public ResearchBeaconLevel1() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (placement instanceof BeaconPlacement beacon) {
                beacon.changeStructure(1);
                if (!level.isClientSide()) {
                    beacon.sendWarning("upgraded_warning");
                }
            }
        };
    }

    public String getItemName() {
        return itemName;
    }

    public Button getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new Button(
                itemName,
                14,
                new ResourceLocation("minecraft", "textures/block/iron_block.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> false,
                () -> itemIsBeingProducedAt(prodBuilding) ||
                        (prodBuilding instanceof BeaconPlacement beacon && beacon.getUpgradeLevel() != 0),
                () -> true,
                () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, this),
                null,
                List.of(
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.beacon_level1"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedTime(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.beacon_level1.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.beacon_level_win"), Style.EMPTY)
                )
        );
    }

    public Button getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new Button(
                itemName,
                14,
                new ResourceLocation("minecraft", "textures/block/iron_block.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                null,
                () -> false,
                () -> false,
                () -> true,
                () -> BuildingServerboundPacket.cancelProduction(prodBuilding.minCorner, this, first),
                null,
                null
        );
    }
}
