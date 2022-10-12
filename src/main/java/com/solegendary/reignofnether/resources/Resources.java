package com.solegendary.reignofnether.resources;

// RTS resources used for buildings, units, etc.
// usually tied to a ServerPlayer's NBT data so it is retained between relogs and server restarts
// but with a clientside copy too for the HUD
public class Resources {

    String ownerName;
    // present amounts of each resource
    int wood;
    int food;
    int ore;

    // balances of each resource to add (or remove if < 0)
    // shown as a +-X amount beside the resource on the HUD that is ticked in over time
    int woodToAdd = 0;
    int foodToAdd = 0;
    int oreToAdd = 0;

    public Resources(String ownerName, int wood, int food, int ore) {
        this.ownerName = ownerName;
        this.wood = 0;
        this.food = 0;
        this.ore = 0;
    }

    // usually used clientside
    public void changeOverTime(int wood, int food, int ore) {
        this.woodToAdd += wood;
        this.foodToAdd += food;
        this.oreToAdd += ore;
    }

    // usually used serverside
    public void changeInstantly(int wood, int food, int ore) {
        this.wood += wood;
        this.food += food;
        this.ore += ore;
    }

    // drain ToAdd fields into totals so that we get the appearance of change over time on the HUD
    public void tick() {

    }
}
