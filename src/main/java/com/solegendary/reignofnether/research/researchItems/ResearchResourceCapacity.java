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
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchResourceCapacity extends ProductionItem {

    public final static String itemName = "Worker Carry Bags";

    public ResearchResourceCapacity(ProductionBuilding building) {
        super(building, ResourceCosts.ResearchResourceCapacity.TICKS);
        this.onComplete = (Level level) -> {
            if (level.isClientSide()) {
                ResearchClient.addResearch(ResearchResourceCapacity.itemName);
                for (LivingEntity unit : UnitClientEvents.getAllUnits())
                    if (unit instanceof WorkerUnit)
                        ((Unit) unit).setupEquipmentAndUpgradesClient();
            }
            else {
                ResearchServer.addResearch(this.building.ownerName, ResearchResourceCapacity.itemName);
                for (LivingEntity unit : UnitServerEvents.getAllUnits())
                    if (unit instanceof WorkerUnit)
                        ((Unit) unit).setupEquipmentAndUpgradesServer();
            }
        };
        this.foodCost = ResourceCosts.ResearchResourceCapacity.FOOD;
        this.woodCost = ResourceCosts.ResearchResourceCapacity.WOOD;
        this.oreCost = ResourceCosts.ResearchResourceCapacity.ORE;
    }

    public String getItemName() {
        return ResearchResourceCapacity.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {
        return new Button(
                ResearchResourceCapacity.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/chest.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> false,
                () -> ProductionItem.itemIsBeingProduced(ResearchResourceCapacity.itemName) ||
                        ResearchClient.hasResearch(ResearchResourceCapacity.itemName),
                () -> true,
                () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
                null,
                List.of(
                        FormattedCharSequence.forward(ResearchResourceCapacity.itemName, Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("\uE000  " + ResourceCosts.ResearchResourceCapacity.FOOD + "     \uE001  " + ResourceCosts.ResearchResourceCapacity.WOOD, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("\uE004  " + ResourceCosts.ResearchResourceCapacity.TICKS/20 + "s", MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("Raises the resource capacity of workers from 100 to 150", Style.EMPTY)
                )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
                ResearchResourceCapacity.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/chest.png"),
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
