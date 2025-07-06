package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.Level;

public class ArmouredHoglinUnit extends HoglinUnit implements Unit, AttackerUnit {

    private static final float armorValue = 8;

    public ArmouredHoglinUnit(EntityType<? extends Hoglin> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, HoglinUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, HoglinUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, HoglinUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, ArmouredHoglinUnit.armorValue);
    }
}
