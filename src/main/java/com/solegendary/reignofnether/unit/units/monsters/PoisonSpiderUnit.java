package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.SpinWebs;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class PoisonSpiderUnit extends SpiderUnit implements Unit, AttackerUnit {

    public PoisonSpiderUnit(EntityType<? extends Spider> entityType, Level level) {
        super(entityType, level);
    }

    private static final int POISON_DAMAGE = 7;

    // removes vanilla spider jockey spawn and random effects
    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return pSpawnData;
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity pEntity) {
        if (super.doHurtTarget(pEntity)) {
            if (pEntity instanceof LivingEntity)
                ((LivingEntity)pEntity).addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DAMAGE * 27, 0), this);
            for (Ability ability : abilities.get())
                if (ability instanceof SpinWebs spinWebs && spinWebs.isAutocasting(this) && spinWebs.isOffCooldown(this))
                    spinWebs.use(this.level(), this, pEntity.getOnPos());
            return true;
        } else {
            return false;
        }
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
