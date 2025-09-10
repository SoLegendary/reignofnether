package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// lines and boxes drawn to entities and blocks to indicate unit intent like movement
public class Checkpoint {

    public static final int CHECKPOINT_TICKS_FADE = 15; // ticks left at which the lines start to fade

    public final BlockPos bp;
    public final Entity entity;
    public final BuildingPlacement placement;
    public final boolean isGreen;
    private boolean isFading = false;
    public int ticksLeft = CHECKPOINT_TICKS_FADE;

    public Checkpoint(BlockPos bp, boolean isGreen) {
        this.bp = bp;
        this.entity = null;
        this.isGreen = isGreen;
        this.placement = BuildingUtils.findBuilding(true, bp);
    }

    public Checkpoint(Entity entity, boolean isGreen) {
        this.bp = null;
        this.entity = entity;
        this.isGreen = isGreen;
        this.placement = null;
    }

    public void startFading() {
        isFading = true;
    }

    public void tick() {
        if (isFading && ticksLeft > 0)
            ticksLeft -= 1;
    }

    public boolean isForEntity() {
        return entity != null;
    }

    public Vec3 getPos() {
        if (isForEntity())
            return entity.getEyePosition().add(0, 1.74f - entity.getEyeHeight() - 1, 0);
        else
            return new Vec3(bp.getX() + 0.5f, bp.getY() + 1.0f, bp.getZ() + 0.5f);
    }
}
