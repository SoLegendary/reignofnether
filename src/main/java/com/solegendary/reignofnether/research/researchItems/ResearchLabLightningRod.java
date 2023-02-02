package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.building.buildings.monsters.Laboratory;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchLabLightningRod extends ProductionItem {

    public final static String itemName = "Lightning Rod";

    public ResearchLabLightningRod(ProductionBuilding building) {
        super(building, ResourceCosts.ResearchLabLightningRod.TICKS);
        this.onComplete = (Level level) -> {
            if (this.building instanceof Laboratory lab)
                lab.changeStructure("laboratory_lightning");
        };
        this.foodCost = ResourceCosts.ResearchLabLightningRod.FOOD;
        this.woodCost = ResourceCosts.ResearchLabLightningRod.WOOD;
        this.oreCost = ResourceCosts.ResearchLabLightningRod.ORE;
    }

    public String getItemName() {
        return ResearchLabLightningRod.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {
        return new Button(
                ResearchLabLightningRod.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/lightbulb_off.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> false,
                () -> ProductionItem.itemIsBeingProduced(ResearchLabLightningRod.itemName) ||
                        (prodBuilding instanceof Laboratory lab && lab.isUpgraded()),
                () -> true,
                () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
                null,
                List.of(
                        FormattedCharSequence.forward(ResearchLabLightningRod.itemName, Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.ResearchLabLightningRod.WOOD + "     \uE002  " + ResourceCosts.ResearchLabLightningRod.ORE, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("\uE004  " + ResourceCosts.ResearchLabLightningRod.TICKS/20 + "s", MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("Adds a lightning rod onto this lab. Lightning can ", Style.EMPTY),
                        FormattedCharSequence.forward("be called to charge creepers and damage enemies.", Style.EMPTY)
                )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
                ResearchLabLightningRod.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/lightbulb_off.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                null,
                () -> false,
                () -> false,
                () -> true,
                () -> BuildingServerboundPacket.cancelProduction(prodBuilding.minCorner, itemName, first),
                null,
                null
        );
    }
}
