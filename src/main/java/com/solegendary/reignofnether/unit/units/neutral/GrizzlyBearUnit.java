package com.solegendary.reignofnether.unit.units.neutral;

import com.solegendary.reignofnether.util.AttributeHelpers;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.level.Level;

public class GrizzlyBearUnit extends PolarBearUnit {

    public GrizzlyBearUnit(EntityType<? extends PolarBear> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Unit.createDefaultAttributes()
                .add(Attributes.ATTACK_DAMAGE, PolarBearUnit.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, PolarBearUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, PolarBearUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, PolarBearUnit.armorValue)
                .add(AttributeHelpers.holder(AttributeRegistrar.ATTACK_DAMAGE.get()), attackDamage)
                .add(AttributeHelpers.holder(AttributeRegistrar.ATTACKS_PER_SECOND.get()), attacksPerSecond)
                .add(AttributeHelpers.holder(AttributeRegistrar.ATTACK_RANGE.get()), attackRange)
                .add(AttributeHelpers.holder(AttributeRegistrar.AGGRO_RANGE.get()), aggroRange)
                .add(AttributeHelpers.holder(AttributeRegistrar.SIGHT_RANGE.get()), Unit.DEFAULT_SIGHT_RANGE)
                .add(AttributeHelpers.holder(AttributeRegistrar.RANGED_DAMAGE_RESIST.get()), 0)
                .add(AttributeHelpers.holder(AttributeRegistrar.MAGIC_DAMAGE_RESIST.get()), 0);
    }
}
