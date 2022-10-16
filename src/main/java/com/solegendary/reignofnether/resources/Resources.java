package com.solegendary.reignofnether.resources;

// RTS resources used for buildings, units, etc.
// usually tied to a ServerPlayer's NBT data so it is retained between relogs and server restarts
// but with a clientside copy too for the HUD
public class Resources {

    public String ownerName;

    // present amounts of each resource
    public int food;
    public int wood;
    public int ore;

    // balances of each resource to add (or remove if < 0)
    // shown as a +-X amount beside the resource on the HUD that is ticked in over time
    public int foodToAdd = 0;
    public int woodToAdd = 0;
    public int oreToAdd = 0;

    public Resources(String ownerName, int food, int wood, int ore) {
        this.ownerName = ownerName;
        this.food = food;
        this.wood = wood;
        this.ore = ore;
    }

    // usually used clientside
    public void changeOverTime(int food, int wood, int ore) {
        this.foodToAdd += food;
        this.woodToAdd += wood;
        this.oreToAdd += ore;
    }

    // usually used serverside
    public void changeInstantly(int food, int wood, int ore) {
        this.food += food;
        this.wood += wood;
        this.ore += ore;
    }

    // drain ToAdd fields into totals so that we get the appearance of change over time on the HUD
    public void tick() {
        this.food += getDrainPerTick(this.foodToAdd);
        this.foodToAdd -= getDrainPerTick(this.foodToAdd);
        this.wood += getDrainPerTick(this.woodToAdd);
        this.woodToAdd -= getDrainPerTick(this.woodToAdd);
        this.ore += getDrainPerTick(this.oreToAdd);
        this.oreToAdd -= getDrainPerTick(this.oreToAdd);
    }

    private int getDrainPerTick(int totalToAdd) {
        int absVal = Math.abs(totalToAdd);
        int retVal = 0;

        if (absVal > 10)
            retVal = Math.round((float) absVal / 10);
        else if (absVal > 0)
            retVal = 1;

        return (int) Math.signum(totalToAdd) * retVal;
    }
}
