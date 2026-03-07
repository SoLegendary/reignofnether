package com.solegendary.reignofnether.ability.heroAbilities.royalguard;

//Winds up and then smashes the ground in front of the guard, dealing area damage and stunning enemy units for a few seconds
//Higher levels incease the damage and stun duration

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericTargetedSpellGoal;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.RoyalGuardUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class MaceSlam extends HeroAbility {

    public static final float RADIUS = 3.0f;
    public static final float RANGE = 3;
    public float damage = 10f;
    public int stunDuration = 2 * ResourceCost.TICKS_PER_SECOND;
    public static final float KNOCKBACK = 0.5f;

    public MaceSlam() {
        super(3, 60, UnitAction.MACE_SLAM, 20 * ResourceCost.TICKS_PER_SECOND, RANGE, 0, true);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof RoyalGuardUnit royalGuardUnit) {
            GenericTargetedSpellGoal goal = royalGuardUnit.getCastMaceSlamGoal();
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
            damage = 10;
            stunDuration = 2 * ResourceCost.TICKS_PER_SECOND;
        } else if (getRank(hero) == 2) {
            damage = 15;
            stunDuration = 3 * ResourceCost.TICKS_PER_SECOND;
        } else if (getRank(hero) == 3) {
            damage = 20;
            stunDuration = 4 * ResourceCost.TICKS_PER_SECOND;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Mace Slam",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/mace_slam.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.MACE_SLAM,
                () -> getRank(hero) == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.MACE_SLAM),
                null,
                getTooltipLines((HeroUnit) hero),
                this,
                hero
        );
        button.stretchIconToBorders = true;
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Mace Slam",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/mace_slam.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.mace_slam") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.mace_slam.stats", damage, cooldownMax / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.mace_slam.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.mace_slam.tooltip2", stunDuration / 20))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.mace_slam"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.mace_slam.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.mace_slam.tooltip2", stunDuration / 20)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.mace_slam.rank1"), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.mace_slam.rank2"), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.mace_slam.rank3"), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((RoyalGuardUnit) unitUsing).getCastMaceSlamGoal().setAbility(this);
        ((RoyalGuardUnit) unitUsing).getCastMaceSlamGoal().setTarget(targetEntity);
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((RoyalGuardUnit) unitUsing).getCastMaceSlamGoal().setAbility(this);
        ((RoyalGuardUnit) unitUsing).getCastMaceSlamGoal().setTarget(targetBp);
    }
}

