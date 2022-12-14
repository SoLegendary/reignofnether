package com.solegendary.reignofnether.unit;

public class Ability {
    public final UnitAction action; // null for worker building production items (handled specially in BuildingClientEvents)
    public final int cooldownMax;
    public int cooldown = 0;
    public final float range; // if <= 0, is melee
    public final float radius; // if <= 0, is single target

    public Ability(UnitAction action, int cooldownMax, float range, float radius) {
        this.action = action;
        this.cooldownMax = cooldownMax;
        this.range = range;
        this.radius = radius;
    }

    public void tickCooldown() {
        if (this.cooldown > 0)
            this.cooldown -= 1;
    }

    public void setCooldown() {
        this.cooldown = cooldownMax;
    }
}
