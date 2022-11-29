package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.unit.UnitClientEvents.getPlayerToEntityRelationship;
import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;

// classic RTS control groups that can contain either buildings or units
// ctrl + number to create a group, number to select that group
// groups appear on the HUD as buttons which can be right clicked to be removed
// only owned units/buildings can be placed in a control group

public class ControlGroup {
    private final ArrayList<LivingEntity> entities = new ArrayList<>();
    private final ArrayList<Building> buildings = new ArrayList<>();
    private Keybinding keybinding = null;

    public ControlGroup() { }

    public int getKey() {
        return this.keybinding.key;
    }

    public void clearAll() {
        this.entities.clear();
        this.buildings.clear();
    }

    public void setSelected(Keybinding keybinding) {
        this.clearAll();
        this.keybinding = keybinding;

        ArrayList<LivingEntity> selUnits = UnitClientEvents.getSelectedUnits();
        Building selBuilding = BuildingClientEvents.getSelectedBuilding();

        if (selUnits.size() > 0 && getPlayerToEntityRelationship(selUnits.get(0)) == Relationship.OWNED) {

        }
        else if (selBuilding != null && BuildingClientEvents.getPlayerToBuildingRelationship(selBuilding) == Relationship.OWNED) {

        }
    }

    public void assignToSelected() {
        if (entities.size() > 0)
            UnitClientEvents.setSelectedUnits(entities);
        else if (buildings.size() > 0) // TODO: update when we can select multiple buildings
            BuildingClientEvents.setSelectedBuilding(buildings.get(0));
    }

    public Button getButton() {

        ResourceLocation icon = null;
        if (this.entities.size() > 0) {
            String unitName = HudClientEvents.getSimpleEntityName(this.entities.get(0));
            icon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/" + unitName + ".png");
        }
        else if (this.buildings.size() > 0) {
            icon = this.buildings.get(0).icon;
        }
        return new Button(
            "Control Group " + getKey(),
            Button.itemIconSize,
            icon,
            this.keybinding,
            () -> false,
            () -> false,
            () -> true,
            this::assignToSelected,
            List.of(FormattedCharSequence.forward("Control Group " + getKey() + " (Right click to remove)", Style.EMPTY))
        );
    }
}
