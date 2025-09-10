package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.sandbox.SandboxAction;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sandbox.SandboxAction;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class ZombieProd extends ProductionItem {

    public final static String itemName = "Zombie";
    public final static ResourceCost cost = ResourceCosts.ZOMBIE;

    public ZombieProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement building) -> {
            if (!level.isClientSide()) {
                if (ResearchServerEvents.playerHasResearch(building.ownerName, ProductionItems.RESEARCH_HUSKS))
                    building.produceUnit((ServerLevel) level, EntityRegistrar.HUSK_UNIT.get(), building.ownerName, true);
                else if (ResearchServerEvents.playerHasResearch(building.ownerName, ProductionItems.RESEARCH_DROWNED))
                    building.produceUnit((ServerLevel) level, EntityRegistrar.DROWNED_UNIT.get(), building.ownerName, true);
                else
                    building.produceUnit((ServerLevel) level, EntityRegistrar.ZOMBIE_UNIT.get(), building.ownerName, true);
            }
        };
    }

    public String getItemName() {
        return ZombieProd.itemName;
    }

    private static ResourceLocation getIcon() {
        if (ResearchClient.hasResearch(ProductionItems.RESEARCH_HUSKS))
            return new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/husk.png");
        else if (ResearchClient.hasResearch(ProductionItems.RESEARCH_DROWNED))
            return new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/drowned.png");
        else
            return new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/zombie.png");
    }

    private static String getCancelName() {
        if (ResearchClient.hasResearch(ProductionItems.RESEARCH_HUSKS))
            return "Husk";
        else if (ResearchClient.hasResearch(ProductionItems.RESEARCH_DROWNED))
            return "Drowned";
        else
            return "Zombie";
    }

    public AbilityButton getPlaceButton() {
        return new AbilityButton(
                ZombieProd.itemName,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/zombie.png"),
                null,
                () -> SandboxClientEvents.spawnUnitName.equals(itemName),
                () -> false,
                () -> true,
                () -> {
                    CursorClientEvents.setLeftClickSandboxAction(SandboxAction.SPAWN_UNIT);
                    SandboxClientEvents.spawnUnitName = itemName;
                },
                null,
                List.of(
                        FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.zombie"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.zombie.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.zombie.tooltip2"), Style.EMPTY)
                ),
                null
        );
    }

    public Button getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new Button(
                ZombieProd.itemName,
                14,
                getIcon(),
                hotkey,
                () -> false,
                () -> ResearchClient.hasResearch(ProductionItems.RESEARCH_HUSKS) || ResearchClient.hasResearch(ProductionItems.RESEARCH_DROWNED),
                () -> true,
                () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, ProductionItems.ZOMBIE),
                null,
                List.of(
                    FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.zombie"), Style.EMPTY.withBold(true)),
                    ResourceCosts.getFormattedCost(cost),
                    ResourceCosts.getFormattedPopAndTime(cost),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.zombie.tooltip1"), Style.EMPTY),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("units.monsters.reignofnether.zombie.tooltip2"), Style.EMPTY)
                )
        );
    }

    public Button getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new Button(
            getCancelName(),
            14,
            getIcon(),
            (Keybinding) null,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.cancelProduction(prodBuilding.originPos, ProductionItems.ZOMBIE, first),
            null,
            null
        );
    }
}
