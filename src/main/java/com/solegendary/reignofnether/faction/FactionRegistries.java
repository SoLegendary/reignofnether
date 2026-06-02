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
        register(Faction.MONSTERS, Buildings.MAUSOLEUM, Keybindings.abilitySlot1);
        register(Faction.MONSTERS, Buildings.SPRUCE_STOCKPILE, Keybindings.abilitySlot2);
        register(Faction.MONSTERS, Buildings.HAUNTED_HOUSE, Keybindings.abilitySlot3);
        register(Faction.MONSTERS, Buildings.PUMPKIN_FARM, Keybindings.abilitySlot4);
        register(Faction.MONSTERS, Buildings.DARK_WATCHTOWER, Keybindings.abilitySlot5);
        register(Faction.MONSTERS, Buildings.GRAVEYARD, Keybindings.abilitySlot6);
        register(Faction.MONSTERS, Buildings.DUNGEON, Keybindings.abilitySlot7);
        register(Faction.MONSTERS, Buildings.SPIDER_LAIR, Keybindings.abilitySlot8);
        register(Faction.MONSTERS, Buildings.SLIME_PIT, Keybindings.abilitySlot9);
        register(Faction.MONSTERS, Buildings.LABORATORY, Keybindings.abilitySlot10);
        register(Faction.MONSTERS, Buildings.STRONGHOLD, Keybindings.hotkey2);
        register(Faction.MONSTERS, Buildings.ALTAR_OF_DARKNESS, Keybindings.hotkey3);
        register(Faction.MONSTERS, Buildings.SPRUCE_BRIDGE, Keybindings.hotkey4);
        register(Faction.MONSTERS, Buildings.SCULK_CATALYST, Keybindings.hotkey1);
        register(Faction.MONSTERS, Buildings.BEACON);

        //Piglins
        register(Faction.PIGLINS, Buildings.CENTRAL_PORTAL, Keybindings.abilitySlot1);
        register(Faction.PIGLINS, Buildings.PORTAL_BASIC, Keybindings.abilitySlot2);
        register(Faction.PIGLINS, Buildings.NETHERWART_FARM, Keybindings.abilitySlot3);
        register(Faction.PIGLINS, Buildings.BASTION, Keybindings.abilitySlot4);
        register(Faction.PIGLINS, Buildings.HOGLIN_STABLES, Keybindings.abilitySlot5);
        register(Faction.PIGLINS, Buildings.FLAME_SANCTUARY, Keybindings.abilitySlot6);
        register(Faction.PIGLINS, Buildings.WITHER_SHRINE, Keybindings.abilitySlot7);
        register(Faction.PIGLINS, Buildings.BASALT_SPRINGS, Keybindings.abilitySlot8);
        register(Faction.PIGLINS, Buildings.FORTRESS, Keybindings.abilitySlot9);
        register(Faction.PIGLINS, Buildings.INFERNAL_PORTAL, Keybindings.hotkey3);
        register(Faction.PIGLINS, Buildings.BLACKSTONE_BRIDGE, Keybindings.hotkey4);
        register(Faction.PIGLINS, Buildings.BEACON);

        //Villagers
        register(Faction.VILLAGERS, Buildings.TOWN_CENTRE, Keybindings.abilitySlot1);
        register(Faction.VILLAGERS, Buildings.OAK_STOCKPILE, Keybindings.abilitySlot2);
        register(Faction.VILLAGERS, Buildings.VILLAGER_HOUSE, Keybindings.abilitySlot3);
        register(Faction.VILLAGERS, Buildings.WHEAT_FARM, Keybindings.abilitySlot4);
        register(Faction.VILLAGERS, Buildings.WATCHTOWER, Keybindings.abilitySlot5);
        register(Faction.VILLAGERS, Buildings.BARRACKS, Keybindings.abilitySlot6);
        register(Faction.VILLAGERS, Buildings.BLACKSMITH, Keybindings.abilitySlot7);
        register(Faction.VILLAGERS, Buildings.WITCH_HUT, Keybindings.abilitySlot8);
        register(Faction.VILLAGERS, Buildings.ARCANE_TOWER, Keybindings.abilitySlot9);
        register(Faction.VILLAGERS, Buildings.LIBRARY, Keybindings.abilitySlot10);
        register(Faction.VILLAGERS, Buildings.CASTLE, Keybindings.hotkey2);
        register(Faction.VILLAGERS, Buildings.SHRINE_OF_PROSPERITY, Keybindings.hotkey3);
        register(Faction.VILLAGERS, Buildings.IRON_GOLEM_BUILDING, Keybindings.hotkey8);
        register(Faction.VILLAGERS, Buildings.OAK_BRIDGE, Keybindings.hotkey4);
        register(Faction.VILLAGERS, Buildings.BEACON);

        //Neutral
        register(Faction.NONE, Buildings.CAPTURABLE_BEACON, Keybindings.abilitySlot1);
        register(Faction.NONE, Buildings.HEALING_FOUNTAIN, Keybindings.abilitySlot2);
        register(Faction.NONE, Buildings.END_PORTAL, Keybindings.abilitySlot3);
        register(Faction.NONE, Buildings.NEUTRAL_TRANSPORT_PORTAL, Keybindings.abilitySlot4);
    }
}
