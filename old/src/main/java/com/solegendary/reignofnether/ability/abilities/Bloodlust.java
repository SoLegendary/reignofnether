package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import com.solegendary.reignofnether.unit.units.piglins.HeadhunterUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class Bloodlust extends Ability {

    private static final int HEALTH_COST = 12;
    private static final int DURATION_SECONDS = 10;

    public static final float BLOODLUST_ATTACK_SPEED_MULTIPLIER = 1.6f;

    private final Unit unit;

    public Bloodlust(Unit unit) {
        super(
                UnitAction.BLOOD_LUST,
                ((Entity) unit).level(),
                0,
                0,
                0,
                false,
                false
        );
        this.unit = unit;
    }

    private static int getDurationLeft(Unit unit) {
        if (unit instanceof HeadhunterUnit headhunterUnit) {
            return headhunterUnit.bloodlustTicks;
        } else if (unit instanceof BruteUnit bruteUnit) {
            return bruteUnit.bloodlustTicks;
        }
        return 0;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
                "Bloodlust",
                new ResourceLocation("minecraft", "textures/block/redstone_block.png"),
                hotkey,
                () -> getDurationLeft(unit) > 0,
                () -> !ResearchClient.hasResearch(ProductionItems.RESEARCH_BLOODLUST),
                () -> true,
                () -> UnitClientEvents.sendUnitCommand(UnitAction.BLOOD_LUST),
                null,
                List.of(
                        fcs(I18n.get("abilities.reignofnether.bloodlust"), true),
                        FormattedCharSequence.forward("\uE007  " + HEALTH_COST, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.bloodlust.tooltip1", HEALTH_COST), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.bloodlust.tooltip2", DURATION_SECONDS), Style.EMPTY)
                ),
                this
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        int duration = DURATION_SECONDS * ResourceCost.TICKS_PER_SECOND;
        if (((LivingEntity) unit).getHealth() <= HEALTH_COST)
            return;
        else
            ((LivingEntity) unit).hurt(level.damageSources().magic(), HEALTH_COST);

        if (unit instanceof HeadhunterUnit headhunterUnit) {
            headhunterUnit.bloodlustTicks = duration;
            headhunterUnit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0));
            headhunterUnit.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) (HEALTH_COST * 20 * 2.5f) + 40, 0));
        } else if (unit instanceof BruteUnit bruteUnit) {
            bruteUnit.bloodlustTicks = duration;
            bruteUnit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0));
            bruteUnit.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) (HEALTH_COST * 20 * 2.5f) + 40, 0));
        }
    }

    @Override
    public boolean shouldResetBehaviours() { return false; }
}
