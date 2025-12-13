package com.solegendary.reignofnether.faction;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

public class FactionRegister {
    List<Pair<Building, Keybinding>> registeredBuildings = new ArrayList<>();
    public <T extends Building> T registerBuilding(T building) {
        return registerBuilding(building, null);
    }

    public <T extends Building> T registerBuilding(T building, Keybinding keybinding) {
        boolean keybindExist = false;
        if (keybinding != null) {
            for (Pair<Building, Keybinding> registeredBuilding : registeredBuildings) {
                if (registeredBuilding.getB() == keybinding) {
                    keybindExist = true;
                }
            }
        }
        registeredBuildings.add(new Pair<>(building, keybindExist ? null : keybinding));
        return building;
    }

    public List<BuildingPlaceButton> getBuildingButtons() {
        List<BuildingPlaceButton> list = new ArrayList<>();
        for (Pair<Building, Keybinding> pair : registeredBuildings) {
            BuildingPlaceButton buildButton = pair.getA().getBuildButton(pair.getB());
            list.add(buildButton);
        }
        return list;
    }
}
