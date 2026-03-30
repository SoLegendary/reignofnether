package com.solegendary.reignofnether.faction;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;

public class FactionRegistries {
    public static final FactionRegister VILLAGERS = new FactionRegister();
    public static final FactionRegister MONSTERS = new FactionRegister();
    public static final FactionRegister PIGLINS = new FactionRegister();
    public static final FactionRegister NONE = new FactionRegister();

    public static void register(Faction faction, Building building) {
        getRegister(faction).registerBuilding(building);
    }

    public static void register(Faction faction, Building building, Keybinding keybinding) {
        getRegister(faction).registerBuilding(building, keybinding);
    }

    public static FactionRegister getRegister(Faction faction) {
        return switch (faction) {
            case VILLAGERS -> VILLAGERS;
            case MONSTERS -> MONSTERS;
            case PIGLINS -> PIGLINS;
            case NONE, NEUTRAL -> NONE;
        };
    }

    public static void register() {
        // Monsters
        register(Faction.MONSTERS, Buildings.MAUSOLEUM, Keybindings.keyQ);
        register(Faction.MONSTERS, Buildings.SPRUCE_STOCKPILE, Keybindings.keyW);
        register(Faction.MONSTERS, Buildings.HAUNTED_HOUSE, Keybindings.keyE);
        register(Faction.MONSTERS, Buildings.PUMPKIN_FARM, Keybindings.keyR);
        register(Faction.MONSTERS, Buildings.DARK_WATCHTOWER, Keybindings.keyT);
        register(Faction.MONSTERS, Buildings.GRAVEYARD, Keybindings.keyY);
        register(Faction.MONSTERS, Buildings.DUNGEON, Keybindings.keyU);
        register(Faction.MONSTERS, Buildings.SPIDER_LAIR, Keybindings.keyI);
        register(Faction.MONSTERS, Buildings.SLIME_PIT, Keybindings.keyO);
        register(Faction.MONSTERS, Buildings.LABORATORY, Keybindings.keyP);
        register(Faction.MONSTERS, Buildings.STRONGHOLD, Keybindings.keyL);
        register(Faction.MONSTERS, Buildings.ALTAR_OF_DARKNESS, Keybindings.keyF);
        register(Faction.MONSTERS, Buildings.SPRUCE_BRIDGE, Keybindings.keyC);
        register(Faction.MONSTERS, Buildings.SCULK_CATALYST, Keybindings.keyV);
        register(Faction.MONSTERS, Buildings.BEACON);

        //Piglins
        register(Faction.PIGLINS, Buildings.CENTRAL_PORTAL, Keybindings.keyQ);
        register(Faction.PIGLINS, Buildings.PORTAL_BASIC, Keybindings.keyW);
        register(Faction.PIGLINS, Buildings.NETHERWART_FARM, Keybindings.keyE);
        register(Faction.PIGLINS, Buildings.BASTION, Keybindings.keyR);
        register(Faction.PIGLINS, Buildings.HOGLIN_STABLES, Keybindings.keyT);
        register(Faction.PIGLINS, Buildings.FLAME_SANCTUARY, Keybindings.keyY);
        register(Faction.PIGLINS, Buildings.WITHER_SHRINE, Keybindings.keyU);
        register(Faction.PIGLINS, Buildings.BASALT_SPRINGS, Keybindings.keyI);
        register(Faction.PIGLINS, Buildings.FORTRESS, Keybindings.keyO);
        register(Faction.PIGLINS, Buildings.INFERNAL_PORTAL, Keybindings.keyF);
        register(Faction.PIGLINS, Buildings.BLACKSTONE_BRIDGE, Keybindings.keyC);
        register(Faction.PIGLINS, Buildings.BEACON);

        //Villagers
        register(Faction.VILLAGERS, Buildings.TOWN_CENTRE, Keybindings.keyQ);
        register(Faction.VILLAGERS, Buildings.OAK_STOCKPILE, Keybindings.keyW);
        register(Faction.VILLAGERS, Buildings.VILLAGER_HOUSE, Keybindings.keyE);
        register(Faction.VILLAGERS, Buildings.WHEAT_FARM, Keybindings.keyR);
        register(Faction.VILLAGERS, Buildings.WATCHTOWER, Keybindings.keyT);
        register(Faction.VILLAGERS, Buildings.BARRACKS, Keybindings.keyY);
        register(Faction.VILLAGERS, Buildings.BLACKSMITH, Keybindings.keyU);
        register(Faction.VILLAGERS, Buildings.ARCANE_TOWER, Keybindings.keyI);
        register(Faction.VILLAGERS, Buildings.LIBRARY, Keybindings.keyO);
        register(Faction.VILLAGERS, Buildings.CASTLE, Keybindings.keyP);
        register(Faction.VILLAGERS, Buildings.SHRINE_OF_PROSPERITY, Keybindings.keyF);
        register(Faction.VILLAGERS, Buildings.IRON_GOLEM_BUILDING, Keybindings.keyL);
        register(Faction.VILLAGERS, Buildings.OAK_BRIDGE, Keybindings.keyC);
        register(Faction.VILLAGERS, Buildings.BEACON);

        //Neutral
        register(Faction.NONE, Buildings.CAPTURABLE_BEACON, Keybindings.keyQ);
        register(Faction.NONE, Buildings.HEALING_FOUNTAIN, Keybindings.keyW);
        register(Faction.NONE, Buildings.END_PORTAL, Keybindings.keyE);
        register(Faction.NONE, Buildings.NEUTRAL_TRANSPORT_PORTAL, Keybindings.keyR);
    }
}
