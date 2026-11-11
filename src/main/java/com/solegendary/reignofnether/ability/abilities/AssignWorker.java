package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.FarmPlacement;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.LanguageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingClientEvents.getPlayerToBuildingRelationship;
import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

// assigns the nearest idle worker to each selected farm that has no worker (one worker per farm)
public class AssignWorker extends Ability {

    public AssignWorker() {
        super(
                UnitAction.ASSIGN_WORKER,
                0,
                0,
                0,
                false,
                false
        );
        this.defaultHotkey = Keybindings.abilitySlot1;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        return new AbilityButton(
                "Assign Worker",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_hoe.png"),
                hotkey,
                () -> false,
                () -> false,
                () -> HudClientEvents.hudSelectedPlacement instanceof FarmPlacement farm && farm.isBuilt && !farm.hasWorker(),
                () -> sendUnitCommand(UnitAction.ASSIGN_WORKER),
                null,
                List.of(
                        fcs(LanguageUtil.getTranslation("abilities.reignofnether.assign_worker"), true),
                        fcs(""),
                        fcs(LanguageUtil.getTranslation("abilities.reignofnether.assign_worker.tooltip1"))
                ),
                this,
                placement
        );
    }

    // the worker is only picked serverside so that idleness and distances are authoritative (clients are synced via
    // UnitSyncWorkerClientBoundPacket); clientside we just move the HUD on so the button can be pressed for the next farm
    @Override
    public void use(Level level, BuildingPlacement buildingUsing, BlockPos targetBp) {
        if (level.isClientSide())
            selectNextEmptyFarm(buildingUsing);
        else if (buildingUsing instanceof FarmPlacement farm)
            farm.assignNearestIdleWorker();
    }

    // next selected farm (in selection order, wrapping) without a worker, other than the one just used
    private static void selectNextEmptyFarm(BuildingPlacement current) {
        List<BuildingPlacement> selected = BuildingClientEvents.getSelectedBuildings();
        int start = selected.indexOf(current);
        for (int i = 1; i <= selected.size(); i++) {
            BuildingPlacement bpl = selected.get(Math.floorMod(start + i, selected.size()));
            if (bpl != current && bpl instanceof FarmPlacement farm && farm.isBuilt && !farm.hasWorker() &&
                getPlayerToBuildingRelationship(farm) == Relationship.OWNED) {
                HudClientEvents.hudSelectedPlacement = farm;
                return;
            }
        }
    }
}
