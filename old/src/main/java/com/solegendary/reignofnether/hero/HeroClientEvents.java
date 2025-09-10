package com.solegendary.reignofnether.hero;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.unit.HeroUnitSave;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;

public class HeroClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    public static ArrayList<HeroUnitSave> fallenHeroes = new ArrayList<>();

    public static void addFallenHero(HeroUnitSave heroUnitSave) {
        if (MC.player != null && heroUnitSave.ownerName.equals(MC.player.getName().getString())) {
            fallenHeroes.removeIf(fHero -> fHero.ownerName.equals(heroUnitSave.ownerName) && fHero.name.equals(heroUnitSave.name));
            fallenHeroes.add(heroUnitSave);
            for (BuildingPlacement buildingPlacement : BuildingClientEvents.getBuildings())
                if (buildingPlacement instanceof ProductionPlacement)
                    buildingPlacement.updateButtons();
        }
    }
}
