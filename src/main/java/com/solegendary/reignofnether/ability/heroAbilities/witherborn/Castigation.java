package com.solegendary.reignofnether.ability.heroAbilities.witherborn;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericUntargetedSpellGoal;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.WitherbornUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class Castigation extends HeroAbility {

    public static final int HEALTH_CONVERSION_PERCENT_RANK_1 = 20;
    public static final int HEALTH_CONVERSION_PERCENT_RANK_2 = 30;
    public static final int HEALTH_CONVERSION_PERCENT_RANK_3 = 40;
    public static final int COOLDOWN_REDUCTION_PERCENT_RANK_1 = 20;
    public static final int COOLDOWN_REDUCTION_PERCENT_RANK_2 = 30;
    public static final int COOLDOWN_REDUCTION_PERCENT_RANK_3 = 40;

    public Castigation() {
        super(3, 0, UnitAction.CASTIGATION_TOGGLE, 0, 0, 0, false);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof WitherbornUnit witherbornUnit) {
            GenericUntargetedSpellGoal goal = witherbornUnit.getCastCastigationGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero) || !(unit instanceof WitherbornUnit witherborn)) return null;
        AbilityButton button = new AbilityButton("Castigation",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/castigation.png"),
                hotkey,
                witherborn::isCastigationEnabled,
                () -> getRank(hero) == 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.CASTIGATION_TOGGLE),
                null,
                getTooltipLines(hero),
                this,
                hero
        );
        button.innerIconSizeModifier = 1;
        return button;
    }

    @Override
    public Button getRankUpButton(Keybinding hotkey, HeroUnit hero) {
        return super.getRankUpButtonProtected(
                hotkey,
                "Castigation",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/castigation.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.castigation") + " " + rankString(hero), true),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.castigation.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.castigation.tooltip2")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.castigation.tooltip3"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.castigation"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.castigation.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.castigation.tooltip2")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.can_be_toggled")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.castigation.rank1",
                        HEALTH_CONVERSION_PERCENT_RANK_1, COOLDOWN_REDUCTION_PERCENT_RANK_1), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.castigation.rank2",
                        HEALTH_CONVERSION_PERCENT_RANK_2, COOLDOWN_REDUCTION_PERCENT_RANK_2), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.castigation.rank3",
                        HEALTH_CONVERSION_PERCENT_RANK_3, COOLDOWN_REDUCTION_PERCENT_RANK_3), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((WitherbornUnit) unitUsing).getCastCastigationGoal().setAbility(this);
        ((WitherbornUnit) unitUsing).getCastCastigationGoal().startCasting();
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((WitherbornUnit) unitUsing).getCastCastigationGoal().setAbility(this);
        ((WitherbornUnit) unitUsing).getCastCastigationGoal().startCasting();
    }
}
