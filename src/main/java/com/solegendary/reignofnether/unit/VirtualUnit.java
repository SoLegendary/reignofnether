package com.solegendary.reignofnether.unit;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

// for tracking serverside Units that don't yet exist on clientside
public class VirtualUnit {
    public BlockPos pos;
    public final int id;
    public final String ownerName;
    public final AABB aabb;
    public final int population;

    public VirtualUnit(BlockPos pos, int id, String ownerName, int population) {
        this.pos = pos;
        this.id = id;
        this.ownerName = ownerName;
        this.aabb = null;
        this.population = population;
    }

    public VirtualUnit(int id, AABB aabb) { // neutral unit
        this.pos = new BlockPos((int) aabb.getCenter().x, (int) aabb.minY, (int) aabb.getCenter().z);
        this.id = id;
        this.ownerName = "";
        this.aabb = aabb;
        this.population = 0;
    }
}