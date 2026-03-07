package com.solegendary.reignofnether.ability.heroAbilities.necromancer;

//Summons an empowered zombie pair (only one pair at a time)
//Higher levels give the units armor and weapons, then enchanted armor and weapons
//Soul Siphon raises their base size/damage/health

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericUntargetedSpellGoal;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.NecromancerUnit;
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

public class RaiseDead extends HeroAbility {

    public static final int CHANNEL_TICKS = 40;
    private static final int CD_MAX_SECONDS = 75 * ResourceCost.TICKS_PER_SECOND;
    public static final int ZOMBIE_TICKS_BEFORE_DECAY = 60 * ResourceCost.TICKS_PER_SECOND;

    public RaiseDead() {
        super(3, 75, UnitAction.RAISE_DEAD, CD_MAX_SECONDS, 0, 0, false);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof NecromancerUnit necromancerUnit) {
            GenericUntargetedSpellGoal goal = necromancerUnit.getCastRaiseDeadGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Raise Dead",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/raise_dead.png"),
                hotkey,
                () -> false,
                () -> getRank(hero) <= 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.RAISE_DEAD),
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
                "Raise Dead",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/raise_dead.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.raise_dead") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.raise_dead.stats", CD_MAX_SECONDS / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.raise_dead.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.raise_dead.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.raise_dead.tooltip3", ZOMBIE_TICKS_BEFORE_DECAY / 20))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.raise_dead"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.raise_dead.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.raise_dead.tooltip2")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.raise_dead.rank1"), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.raise_dead.rank2"), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.raise_dead.rank3"), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((NecromancerUnit) unitUsing).getCastRaiseDeadGoal().setAbility(this);
        ((NecromancerUnit) unitUsing).getCastRaiseDeadGoal().startCasting();
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((NecromancerUnit) unitUsing).getCastRaiseDeadGoal().setAbility(this);
        ((NecromancerUnit) unitUsing).getCastRaiseDeadGoal().startCasting();
    }
}
