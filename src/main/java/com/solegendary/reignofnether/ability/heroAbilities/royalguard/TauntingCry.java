package com.solegendary.reignofnether.ability.heroAbilities.royalguard;

//Forces all nearby enemy units to lose control for a few seconds and target the royal guard
//While active, the guard gains knockback and push immunity
//Higher levels incease the taunt duration

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
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

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class TauntingCry extends HeroAbility {

    public static final int RANGE = 6;
    private static final int CD_MAX_SECONDS = 40 * ResourceCost.TICKS_PER_SECOND;
    public int duration = 4 * ResourceCost.TICKS_PER_SECOND;
    public static final float DAMAGE_MULT = 0.5f;

    public TauntingCry() {
        super(3, 75, UnitAction.TAUNTING_CRY, CD_MAX_SECONDS, RANGE, 0, false);
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
            duration = 4 * ResourceCost.TICKS_PER_SECOND;
        } else if (getRank(hero) == 2) {
            duration = 6 * ResourceCost.TICKS_PER_SECOND;
        } else if (getRank(hero) == 3) {
            duration= 8 * ResourceCost.TICKS_PER_SECOND;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Taunting Cry",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/taunting_cry.png"),
                hotkey,
                () -> false,
                () -> getRank(hero) <= 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.TAUNTING_CRY),
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
                "Taunting Cry",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/taunting_cry.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.taunting_cry") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.taunting_cry.stats", CD_MAX_SECONDS / 20, range, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip2", duration / 20)),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip3"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.taunting_cry"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip2", duration / 20)),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip3")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.rank1"), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.rank2"), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.rank3"), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((RoyalGuardUnit) unitUsing).getCastTauntingCryGoal().setAbility(this);
        ((RoyalGuardUnit) unitUsing).getCastTauntingCryGoal().startCasting();
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((RoyalGuardUnit) unitUsing).getCastTauntingCryGoal().setAbility(this);
        ((RoyalGuardUnit) unitUsing).getCastTauntingCryGoal().startCasting();
    }
}
