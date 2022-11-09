package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.units.CreeperUnit;
import com.solegendary.reignofnether.unit.units.ZombieUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.world.entity.LivingEntity;

import javax.swing.text.html.parser.Entity;

public class CreeperAttackUnitGoal extends MeleeAttackUnitGoal {
    private final CreeperUnit creeperUnit;

    public CreeperAttackUnitGoal(CreeperUnit creeperUnit, int attackInterval, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(creeperUnit, attackInterval, speedModifier, followingTargetEvenIfNotSeen);
        this.creeperUnit = creeperUnit;
    }

    public void tick() {
        if (creeperUnit.canExplodeOnTarget())
            creeperUnit.setSwellDir(1);
    }
}
