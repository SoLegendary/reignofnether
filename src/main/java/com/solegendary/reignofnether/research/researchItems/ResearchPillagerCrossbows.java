package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServer;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchPillagerCrossbows extends ProductionItem {

    public final static String itemName = "Multishot Crossbows";

    public ResearchPillagerCrossbows(ProductionBuilding building) {
        super(building, ResourceCosts.ResearchPillagerCrossbows.TICKS);
        this.onComplete = (Level level) -> {
            if (level.isClientSide())
                ResearchClient.addResearch(ResearchPillagerCrossbows.itemName);
            else {
                ResearchServer.addResearch(this.building.ownerName, ResearchPillagerCrossbows.itemName);
                for (LivingEntity unit : UnitServerEvents.getAllUnits())
                    if (unit instanceof PillagerUnit pUnit)
                        pUnit.setupEquipmentAndUpgradesServer();
            }
        };
        this.foodCost = ResourceCosts.ResearchPillagerCrossbows.FOOD;
        this.woodCost = ResourceCosts.ResearchPillagerCrossbows.WOOD;
        this.oreCost = ResourceCosts.ResearchPillagerCrossbows.ORE;
    }

    public String getItemName() {
        return ResearchPillagerCrossbows.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {
        return new Button(
                ResearchPillagerCrossbows.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/crossbow.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> false,
                () -> ProductionItem.itemIsBeingProduced(ResearchPillagerCrossbows.itemName) ||
                        ResearchClient.hasResearch(ResearchPillagerCrossbows.itemName),
                () -> true,
                () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
                null,
                List.of(
                        FormattedCharSequence.forward(ResearchPillagerCrossbows.itemName, Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.ResearchPillagerCrossbows.WOOD + "     \uE002  " + ResourceCosts.ResearchPillagerCrossbows.ORE, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("\uE004  " + ResourceCosts.ResearchPillagerCrossbows.TICKS/20 + "s", MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("Enchants the crossbows of all pillagers with multishot", Style.EMPTY)
                )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
                ResearchPillagerCrossbows.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/crossbow.png"),
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
