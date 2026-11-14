package com.solegendary.reignofnether.ability.heroAbilities.witherborn;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericTargetedSpellGoal;
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

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class WitheringMark extends HeroAbility {

    public static final int RANGE = 12;
    public static final int CAST_TICKS = 20;
    public static final int DURATION_SECONDS = 10;
    public static final int ATTACK_SPEED_BONUS_RANK_1 = 20;
    public static final int ATTACK_SPEED_BONUS_RANK_2 = 30;
    public static final int ATTACK_SPEED_BONUS_RANK_3 = 40;
    public static final int LIFESTEAL_RANK_1 = 10;
    public static final int LIFESTEAL_RANK_2 = 15;
    public static final int LIFESTEAL_RANK_3 = 20;

    public WitheringMark() {
        super(3, 40, UnitAction.WITHERING_MARK, 15 * ResourceCost.TICKS_PER_SECOND, RANGE, 0, true);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof WitherbornUnit witherbornUnit) {
            GenericTargetedSpellGoal goal = witherbornUnit.getCastWitheringMarkGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Withering Mark",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/withering_mark.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.WITHERING_MARK,
                () -> getRank(hero) == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.WITHERING_MARK),
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
                "Withering Mark",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/withering_mark.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.withering_mark") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.withering_mark.stats", cooldownMax / 20, DURATION_SECONDS, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.withering_mark.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.withering_mark.tooltip2", DURATION_SECONDS)),
                fcs(I18n.get("abilities.reignofnether.withering_mark.tooltip3"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.withering_mark"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.withering_mark.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.withering_mark.tooltip2", DURATION_SECONDS)),
                fcs(I18n.get("abilities.reignofnether.withering_mark.tooltip3")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.withering_mark.rank1", ATTACK_SPEED_BONUS_RANK_1, LIFESTEAL_RANK_1), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.withering_mark.rank2", ATTACK_SPEED_BONUS_RANK_2, LIFESTEAL_RANK_2), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.withering_mark.rank3", ATTACK_SPEED_BONUS_RANK_3, LIFESTEAL_RANK_3), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((WitherbornUnit) unitUsing).getCastWitheringMarkGoal().setAbility(this);
        ((WitherbornUnit) unitUsing).getCastWitheringMarkGoal().setTarget(targetBp);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (targetEntity == unitUsing)
            return;
        ((WitherbornUnit) unitUsing).getCastWitheringMarkGoal().setAbility(this);
        ((WitherbornUnit) unitUsing).getCastWitheringMarkGoal().setTarget(targetEntity);
    }
}
