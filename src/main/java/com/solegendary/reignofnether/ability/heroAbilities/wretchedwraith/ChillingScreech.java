package com.solegendary.reignofnether.ability.heroAbilities.wretchedwraith;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
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

public class ChillingScreech extends HeroAbility {

    private static final int CD_MAX = 30 * ResourceCost.TICKS_PER_SECOND;

    public static final int DURATION_RANK_1 = 7 * 20;
    public static final int DURATION_RANK_2 = 9 * 20;
    public static final int DURATION_RANK_3 = 11 * 20;

    public static final int RADIUS_RANK_1 = 4;
    public static final int RADIUS_RANK_2 = 6;
    public static final int RADIUS_RANK_3 = 8;

    public int duration = DURATION_RANK_1;

    public ChillingScreech() {
        super(3, 60, UnitAction.CHILLING_SCREECH, CD_MAX, RADIUS_RANK_1, RADIUS_RANK_1, false);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof WretchedWraithUnit wraithUnit) {
            GenericUntargetedSpellGoal goal = wraithUnit.getCastChillingScreechGoal();
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
            duration = DURATION_RANK_1;
            radius = RADIUS_RANK_1;
        } else if (getRank(hero) == 2) {
            duration = DURATION_RANK_2;
            radius = RADIUS_RANK_2;
        } else if (getRank(hero) == 3) {
            duration = DURATION_RANK_3;
            radius = RADIUS_RANK_3;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Chilling Screech",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/chilling_screech.png"),
                hotkey,
                () -> false,
                () -> getRank(hero) <= 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.CHILLING_SCREECH),
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
                "Chilling Screech",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/chilling_screech.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.chilling_screech") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.chilling_screech.stats", CD_MAX / 20, range, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.chilling_screech.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.chilling_screech.tooltip2", duration / 20))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.chilling_screech"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.chilling_screech.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.chilling_screech.tooltip2", duration / 20)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.chilling_screech.rank1", DURATION_RANK_1 / 20, RADIUS_RANK_1), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.chilling_screech.rank2", DURATION_RANK_2 / 20, RADIUS_RANK_2), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.chilling_screech.rank3", DURATION_RANK_3 / 20, RADIUS_RANK_3), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((WretchedWraithUnit) unitUsing).getCastChillingScreechGoal().setAbility(this);
        ((WretchedWraithUnit) unitUsing).getCastChillingScreechGoal().startCasting();
        if (!level.isClientSide()) {
            SoundClientboundPacket.playSoundAtPos(SoundAction.WRETCHED_WRAITH_ATTACK_LOUD, ((WretchedWraithUnit) unitUsing).blockPosition(), 0.6f);
        }
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((WretchedWraithUnit) unitUsing).getCastChillingScreechGoal().setAbility(this);
        ((WretchedWraithUnit) unitUsing).getCastChillingScreechGoal().startCasting();
        if (!level.isClientSide()) {
            SoundClientboundPacket.playSoundAtPos(SoundAction.WRETCHED_WRAITH_ATTACK_LOUD, ((WretchedWraithUnit) unitUsing).blockPosition(), 0.6f);
        }
    }
}
