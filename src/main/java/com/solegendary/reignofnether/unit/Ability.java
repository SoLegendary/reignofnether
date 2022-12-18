package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

public class Ability {
    public final UnitAction action; // null for worker building production items (handled specially in BuildingClientEvents)
    public final int cooldownMax;
    private int cooldown = 0;
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

    public int getCooldown() { return this.cooldown; }

    public void setToMaxCooldown() {
        this.cooldown = cooldownMax;
    }

    public void use(Level level, Unit unitUsing, BlockPos targetBp) { }

    public void use(Level level, Building buildingUsing, BlockPos targetBp) { }

    // limits the targetPos to within this.range distance of originPos, ignoring Y values
    // https://math.stackexchange.com/questions/2045174/how-to-find-a-point-between-two-points-with-given-distance
    public BlockPos getXZRangeLimitedBlockPos(BlockPos originPos, BlockPos targetPos) {

        float d = this.range;
        float x1 = originPos.getX();
        float x2 = targetPos.getX();
        float z1 = originPos.getZ();
        float z2 = targetPos.getZ();

        double D = Math.sqrt(new Vec2(x1,z1).distanceToSqr(new Vec2(x2,z2)));

        if (D <= this.range)
            return targetPos;

        double x3 = x1 + ((d/D) * (x2 - x1));
        double z3 = z1 + ((d/D) * (z2 - z1));

        return new BlockPos(x3, originPos.getY(), z3);
    }
}
