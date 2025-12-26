package com.solegendary.reignofnether.ability.heroAbilities.wildfire;

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
import com.solegendary.reignofnether.unit.units.piglins.WildfireUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class MoltenBomb extends HeroAbility {

    public static final int RANGE = 15;

    public static final int DAMAGE_RANK_1 = 6;
    public static final int DAMAGE_RANK_2 = 8;
    public static final int DAMAGE_RANK_3 = 10;

    public static final int RADIUS_RANK_1 = 4;
    public static final int RADIUS_RANK_2 = 5;
    public static final int RADIUS_RANK_3 = 6;

    public int damage = DAMAGE_RANK_1;

    public MoltenBomb() {
        super(3, 60, UnitAction.MOLTEN_BOMB, 30 * ResourceCost.TICKS_PER_SECOND, RANGE, RADIUS_RANK_1, false);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof WildfireUnit wildfireUnit) {
            GenericTargetedSpellGoal goal = wildfireUnit.getCastMoltenBombGoal();
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
            damage = DAMAGE_RANK_1;
            radius = RADIUS_RANK_1;
        } else if (getRank(hero) == 2) {
            damage = DAMAGE_RANK_2;
            radius = RADIUS_RANK_2;
        } else if (getRank(hero) == 3) {
            damage = DAMAGE_RANK_3;
            radius = RADIUS_RANK_3;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        return new AbilityButton("Molten Bomb",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/fireball.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.MOLTEN_BOMB,
                () -> getRank(hero) == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.MOLTEN_BOMB),
                null,
                getTooltipLines(hero),
                this,
                hero
        );
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Molten Bomb",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/fireball.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.molten_bomb") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.molten_bomb.stats", damage, cooldownMax / 20, RANGE, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.molten_bomb.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.molten_bomb.tooltip2"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.molten_bomb"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.molten_bomb.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.molten_bomb.tooltip2")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.molten_bomb.rank1", DAMAGE_RANK_1, RADIUS_RANK_1), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.molten_bomb.rank2", DAMAGE_RANK_2, RADIUS_RANK_2), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.molten_bomb.rank3", DAMAGE_RANK_3, RADIUS_RANK_3), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((WildfireUnit) unitUsing).getCastMoltenBombGoal().setAbility(this);
        ((WildfireUnit) unitUsing).getCastMoltenBombGoal().setTarget(targetBp);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((WildfireUnit) unitUsing).getCastMoltenBombGoal().setAbility(this);
        ((WildfireUnit) unitUsing).getCastMoltenBombGoal().setTarget(targetEntity);
    }
}
