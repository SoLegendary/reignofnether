package com.solegendary.reignofnether.ability.heroAbilities.wretchedwraith;

import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericUntargetedSpellGoal;
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

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class IceNova extends HeroAbility {

    private static final int CD_MAX_SECONDS = 30 * ResourceCost.TICKS_PER_SECOND;

    public static final int DAMAGE_RANK_1 = 6;
    public static final int DAMAGE_RANK_2 = 8;
    public static final int DAMAGE_RANK_3 = 10;

    public static final int RADIUS_RANK_1 = 4;
    public static final int RADIUS_RANK_2 = 5;
    public static final int RADIUS_RANK_3 = 6;

    public int damage = DAMAGE_RANK_1;

    public IceNova() {
        super(3, 75, UnitAction.ICE_NOVA, CD_MAX_SECONDS, 0, 0, false);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof WretchedWraithUnit wraithUnit) {
            GenericUntargetedSpellGoal goal = wraithUnit.getCastIceNovaGoal();
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
        return new AbilityButton("Ice Nova",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/ice.png"),
                hotkey,
                () -> false,
                () -> getRank(hero) <= 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.ICE_NOVA),
                null,
                getTooltipLines(hero),
                this,
                hero
        );
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Ice Nova",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/ice.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.ice_nova") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.ice_nova.stats", CD_MAX_SECONDS / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.ice_nova.tooltip1"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.ice_nova"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.ice_nova.tooltip1")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.ice_nova.rank1", DAMAGE_RANK_1, RADIUS_RANK_1), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.ice_nova.rank2", DAMAGE_RANK_2, RADIUS_RANK_2), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.ice_nova.rank3", DAMAGE_RANK_3, RADIUS_RANK_3), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((WretchedWraithUnit) unitUsing).getCastIceNovaGoal().setAbility(this);
        ((WretchedWraithUnit) unitUsing).getCastIceNovaGoal().startCasting();
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((WretchedWraithUnit) unitUsing).getCastIceNovaGoal().setAbility(this);
        ((WretchedWraithUnit) unitUsing).getCastIceNovaGoal().startCasting();
    }
}
