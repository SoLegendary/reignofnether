package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.enchantments.VigorEnchantment;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class CastSummonVexes extends Ability {

    public static final int CD_MAX_SECONDS = 60;
    public static final int VEX_DURATION_SECONDS = 30;

    public CastSummonVexes() {
        super(
                UnitAction.CAST_SUMMON_VEXES,
                CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND,
                0,
                0,
                true,
                false
        );
        this.autocastEnableAction = UnitAction.CAST_SUMMON_VEXES_AUTOCAST_ENABLE;
        this.autocastDisableAction = UnitAction.CAST_SUMMON_VEXES_AUTOCAST_DISABLE;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        EvokerUnit evokerUnit = getEvoker(unit);
        return new AbilityButton(
                "Summon Vexes",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/vex.png"),
                hotkey,
                () -> {
                    if (evokerUnit != null && evokerUnit.getCastSummonVexesGoal() != null)
                        return evokerUnit.getCastSummonVexesGoal().isCasting()|| isAutocasting(unit);
                    return false;
                },
                () -> evokerUnit == null || !ResearchClient.hasResearch(ProductionItems.RESEARCH_EVOKER_VEXES),
                () -> true,
                () -> UnitClientEvents.sendUnitCommand(UnitAction.CAST_SUMMON_VEXES),
                () -> {
                    if (evokerUnit != null)
                        toggleAutocast(evokerUnit);
                },
                List.of(
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.summon_vexes"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.summon_vexes.tooltip1", CD_MAX_SECONDS), MyRenderer.iconStyle),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.summon_vexes.tooltip2", EvokerUnit.SUMMON_VEXES_AMOUNT), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.summon_vexes.tooltip3", VEX_DURATION_SECONDS), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.autocast"),Style.EMPTY)
                ),
                evokerUnit != null ? evokerUnit.getSummonVexes() : this,
                evokerUnit
        );
    }

    @Override
    public void setCooldown(float cooldown, Unit unit) {
        EvokerUnit evokerUnit = getEvoker(unit);
        if (evokerUnit == null) return;
        int vigorLevel = evokerUnit.getVigorLevel();
        if (vigorLevel > 0)
            cooldown *= Math.pow(VigorEnchantment.CD_MULTIPLIER, vigorLevel);
        super.setCooldown(cooldown, evokerUnit);
    }


    @Override
    public void setToMaxCooldown(Unit unit) {
        EvokerUnit evokerUnit = getEvoker(unit);
        if (evokerUnit == null) return;
        float cd = cooldownMax;
        int vigorLevel = evokerUnit.getVigorLevel();
        if (vigorLevel > 0)
            cd *= Math.pow(VigorEnchantment.CD_MULTIPLIER, vigorLevel);
        setCooldown(cd, evokerUnit);
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        use(unitUsing);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        use(unitUsing);
    }

    @Nullable
    private EvokerUnit getEvoker(Unit unitUsing) {
        if (unitUsing instanceof RavagerUnit ravager && ravager.getFirstPassenger() instanceof EvokerUnit)
            return (EvokerUnit) ravager.getFirstPassenger();
        if (unitUsing instanceof EvokerUnit)
            return (EvokerUnit) unitUsing;
        return null;
    }

    private void use(Unit unitUsing) {
        EvokerUnit evokerUnit = getEvoker(unitUsing);
        if (evokerUnit != null) {
            evokerUnit.getCastSummonVexesGoal().setAbility(evokerUnit.getSummonVexes());
            evokerUnit.getCastSummonVexesGoal().startCasting();
        }
    }
}