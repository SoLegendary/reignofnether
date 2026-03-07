package com.solegendary.reignofnether.ability.heroAbilities.wretchedwraith;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericTargetedSpellGoal;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.WretchedWraithUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class FrostBlink extends HeroAbility {

    public static final int RANGE_RANK_1 = 8;
    public static final int RANGE_RANK_2 = 10;
    public static final int RANGE_RANK_3 = 12;

    public static final int CD_RANK_1 = 20;
    public static final int CD_RANK_2 = 15;
    public static final int CD_RANK_3 = 10;

    public static final int RADIUS = 3;

    public FrostBlink() {
        super(3, 30, UnitAction.FROSTBLINK, CD_RANK_1 * ResourceCost.TICKS_PER_SECOND, RANGE_RANK_1, RADIUS, false);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof WretchedWraithUnit wraithUnit) {
            GenericTargetedSpellGoal goal = wraithUnit.getCastFrostblinkGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
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
            range = RANGE_RANK_1;
            cooldownMax = CD_RANK_1 * ResourceCost.TICKS_PER_SECOND;
        } else if (getRank(hero) == 2) {
            range = RANGE_RANK_2;
            cooldownMax = CD_RANK_2 * ResourceCost.TICKS_PER_SECOND;
        } else if (getRank(hero) == 3) {
            range = RANGE_RANK_3;
            cooldownMax = CD_RANK_3 * ResourceCost.TICKS_PER_SECOND;
        }
        if (hero instanceof WretchedWraithUnit wraith) {
            wraith.castFrostblinkGoal.range = range;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Frostblink",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/frostblink.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.FROSTBLINK,
                () -> getRank(hero) == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.FROSTBLINK),
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
                "Frostblink",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/frostblink.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.frostblink") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.frostblink.stats", cooldownMax / 20, range, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.frostblink.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.frostblink.tooltip2"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.frostblink"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.frostblink.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.frostblink.tooltip2")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.frostblink.rank1", RANGE_RANK_1, CD_RANK_1), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.frostblink.rank2", RANGE_RANK_2, CD_RANK_2), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.frostblink.rank3", RANGE_RANK_3, CD_RANK_3), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (level.getWorldBorder().isWithinBounds(targetBp)) {
            ((WretchedWraithUnit) unitUsing).getCastFrostblinkGoal().setAbility(this);
            ((WretchedWraithUnit) unitUsing).getCastFrostblinkGoal().setTarget(targetBp);
        } else if (level.isClientSide()) {
            HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.frostblink.out_of_bounds"), 200);
        }
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        use(level, unitUsing, targetEntity.getOnPos());
    }
}
