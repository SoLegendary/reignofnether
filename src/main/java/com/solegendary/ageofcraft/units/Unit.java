package com.solegendary.ageofcraft.units;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

public interface Unit {

    // chase and attack the target ignoring all else until it is dead or out of sight
    void setAttackTarget(@Nullable LivingEntity target);

    // move to a block ignoring all else until reaching it
    void setMoveTarget(@Nullable BlockPos bp);

    // move to a block but chase/attack a target if there is one close by (for a limited distance)
    void setAttackMoveTarget(@Nullable BlockPos bp);

    // continuously move to a target until told to do something else
    void setFollowTarget(@Nullable LivingEntity target);
}
