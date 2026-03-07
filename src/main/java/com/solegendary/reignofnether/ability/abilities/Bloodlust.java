package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import com.solegendary.reignofnether.unit.units.piglins.HeadhunterUnit;
import com.solegendary.reignofnether.unit.units.piglins.HoglinUnit;
import com.solegendary.reignofnether.unit.units.piglins.MarauderUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class Bloodlust extends Ability {

    private static final int HEALTH_COST = 12;
    private static final int HEALTH_COST_HOGLIN = 18;
    private static final int HEALTH_COST_MARAUDER = 24;
    private static final int DURATION_SECONDS = 10;

    public Bloodlust() {
        super(
                UnitAction.BLOOD_LUST,
                0,
                0,
                0,
                false,
                false
        );
    }

    private float getHealthCost(Unit unit) {
        float healthCost = HEALTH_COST;
        if (unit instanceof HoglinUnit)
            healthCost = HEALTH_COST_HOGLIN;
        else if (unit instanceof MarauderUnit)
            healthCost = HEALTH_COST_MARAUDER;
        return healthCost;
    }

    private List<FormattedCharSequence> getTooltip(Unit unit) {
        if (unit instanceof HoglinUnit) {
            return List.of(
                    fcs(I18n.get("abilities.reignofnether.bloodlust"), true),
                    FormattedCharSequence.forward("\uE007  " + getHealthCost(unit), MyRenderer.iconStyle),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("abilities.reignofnether.bloodlust.tooltip1", getHealthCost(unit)), Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("abilities.reignofnether.bloodlust.tooltip2", DURATION_SECONDS), Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("abilities.reignofnether.bloodlust.tooltip3"), Style.EMPTY)
            );
        } else {
            return List.of(
                    fcs(I18n.get("abilities.reignofnether.bloodlust"), true),
                    FormattedCharSequence.forward("\uE007  " + getHealthCost(unit), MyRenderer.iconStyle),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("abilities.reignofnether.bloodlust.tooltip1", getHealthCost(unit)), Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("abilities.reignofnether.bloodlust.tooltip2", DURATION_SECONDS), Style.EMPTY)
            );
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton(
                "Bloodlust",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/bloodlust.png"),
                hotkey,
                () -> unit.hasEffectWithDuration(MobEffectRegistrar.BLOODLUST.get()),
                () -> !ResearchClient.hasResearch(ProductionItems.RESEARCH_BLOODLUST),
                () -> !((LivingEntity) unit).isVehicle() && !((LivingEntity) unit).isPassenger(),
                () -> UnitClientEvents.sendUnitCommand(UnitAction.BLOOD_LUST),
                null,
                getTooltip(unit),
                this,
                unit
        );
    }
    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {

        if (((LivingEntity) unitUsing).getHealth() <= getHealthCost(unitUsing))
            return;
        else
            ((LivingEntity) unitUsing).hurt(level.damageSources().magic(), getHealthCost(unitUsing));

        ((LivingEntity) unitUsing).addEffect(new MobEffectInstance(MobEffectRegistrar.BLOODLUST.get(), DURATION_SECONDS * 20, 0));
        ((LivingEntity) unitUsing).addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) (getHealthCost(unitUsing) * 20 * 2.5f) + 40, 0));
    }

    @Override
    public boolean shouldResetBehaviours() { return false; }
}
