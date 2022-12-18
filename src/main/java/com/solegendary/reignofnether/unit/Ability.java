package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

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

    public void use(Level level, Unit unitUsing, BlockPos targetedBp) { }

    public void use(Level level, Building buildingUsing, BlockPos targetedBp) { }

    public BlockPos getRangeLimitedBlockPos(BlockPos originPos, BlockPos targetPos) {
        return targetPos;
    }
}
