package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class BoggedUnit extends SkeletonUnit implements Unit, AttackerUnit, RangedAttackerUnit {

    public BoggedUnit(EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, SkeletonUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, SkeletonUnit.maxHealth)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, SkeletonUnit.armorValue);
    }

    public static final int POISON_DAMAGE = 6;

    @Override
    public float getUnitAttackDamage() {return attackDamage;}

    final static public float attackDamage = 2.0f;

    @Override
    protected @NotNull AbstractArrow getArrow(@NotNull ItemStack pArrowStack, float pDistanceFactor) {
        AbstractArrow arrow = super.getArrow(pArrowStack, pDistanceFactor);
        if (arrow instanceof Arrow)
            ((Arrow)arrow).addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DAMAGE * 27));
        return arrow;
    }

    @Override
    public List<FormattedCharSequence> getAttackDamageStatTooltip() {
        return List.of(
                fcs(I18n.get("unitstats.reignofnether.attack_damage"), true),
                fcs(I18n.get("unitstats.reignofnether.attack_damage_bonus_poison_damage", POISON_DAMAGE, POISON_DAMAGE))
        );
    }

    @Override
    public boolean hasBonusDamage() {
        return true;
    }
}
