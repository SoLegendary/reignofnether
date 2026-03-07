package com.solegendary.reignofnether.entities;


import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

// AreaEffectCloud used by Wither Skeletons and Witches with options to:
// - Not diminish in size over time or when affecting enemies
public class AdjustableAreaEffectCloud extends AreaEffectCloud {

    public boolean diminishWithTimeAndUse = true;

    public AdjustableAreaEffectCloud(EntityType<? extends AreaEffectCloud> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public AdjustableAreaEffectCloud(Level pLevel, double pX, double pY, double pZ) {
        super(pLevel, pX, pY, pZ);
    }

    @Override
    public void tick() {
        float radius = getRadius();
        super.tick();
        if (!diminishWithTimeAndUse) {
            setRadius(radius);
        }
    }
}
