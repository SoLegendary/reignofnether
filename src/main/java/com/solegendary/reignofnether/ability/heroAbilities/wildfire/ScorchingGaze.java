package com.solegendary.reignofnether.ability.heroAbilities.wildfire;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.WildfireUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class ScorchingGaze extends HeroAbility {

    public static final int RANGE = 15;
    public static final int SPREAD_RANGE = 3;
    public static final int DURATION_RANK_1 = 8;
    public static final int DURATION_RANK_2 = 11;
    public static final int DURATION_RANK_3 = 14;
    public int durationSeconds = DURATION_RANK_1;

    public static final int MIN_MAGMA_DURATION = 200;
    public static final int MAX_MAGMA_DURATION = 300;

    public ScorchingGaze() {
        super(3, 50, UnitAction.SCORCHING_GAZE, 30 * ResourceCost.TICKS_PER_SECOND, RANGE, 0, true);
    }

    @Override
    public boolean rankUp(HeroUnit hero) {
        if (super.rankUp(hero)) {
            updateStatsForRank(hero);
            return true;
        }
        return false;
    }

    @Override
    public void updateStatsForRank(HeroUnit hero) {
        if (getRank(hero) == 1) {
            durationSeconds = DURATION_RANK_1;
        } else if (getRank(hero) == 2) {
            durationSeconds = DURATION_RANK_2;
        } else if (getRank(hero) == 3) {
            durationSeconds = DURATION_RANK_3;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Scorching Gaze",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/scorching_gaze.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.SCORCHING_GAZE,
            () -> getRank(hero) == 0,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.SCORCHING_GAZE),
            null,
            getTooltipLines(hero),
            this,
            hero
        );
        button.stretchIconToBorders = true;
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Scorching Gaze",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/scorching_gaze.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.scorching_gaze") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.scorching_gaze.stats", cooldownMax / 20, RANGE, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.scorching_gaze.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.scorching_gaze.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.scorching_gaze.tooltip3"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
            fcs(I18n.get("abilities.reignofnether.scorching_gaze"), true),
            fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
            fcs(""),
            fcs(I18n.get("abilities.reignofnether.scorching_gaze.tooltip1")),
            fcs(I18n.get("abilities.reignofnether.scorching_gaze.tooltip2")),
            fcs(I18n.get("abilities.reignofnether.scorching_gaze.tooltip3")),
            fcs(""),
            fcs(I18n.get("abilities.reignofnether.scorching_gaze.rank1", DURATION_RANK_1), getRank(hero) == 0),
            fcs(I18n.get("abilities.reignofnether.scorching_gaze.rank2", DURATION_RANK_2), getRank(hero) == 1),
            fcs(I18n.get("abilities.reignofnether.scorching_gaze.rank3", DURATION_RANK_3), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (targetEntity == unitUsing)
            return;
        ((WildfireUnit) unitUsing).getCastScorchingGazeGoal().setAbility(this);
        ((WildfireUnit) unitUsing).getCastScorchingGazeGoal().setTarget(targetEntity);
    }
}
