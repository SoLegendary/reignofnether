package com.solegendary.reignofnether.ability.heroAbilities.shared;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericUntargetedSpellGoal;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.WretchedWraithUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class PlaceholderUntargetedAbility extends HeroAbility {

    public PlaceholderUntargetedAbility() {
        super(3, 3, UnitAction.DEBUG2, 3 * ResourceCost.TICKS_PER_SECOND, 3, 0, true);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof WretchedWraithUnit wretchedWraithUnit) {
            GenericUntargetedSpellGoal goal = wretchedWraithUnit.getCastBlizzardGoal();
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
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        return new AbilityButton("Placeholder Untargeted",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/chain_command_block_front.png"),
                hotkey,
                () -> false,
                () -> getRank(hero) <= 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.DEBUG2),
                null,
                getTooltipLines(hero),
                this,
                hero
        );
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Placeholder Untargeted",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/chain_command_block_front.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs("Placeholder Untargeted")
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of();
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((WretchedWraithUnit) unitUsing).getCastBlizzardGoal().setAbility(this);
        ((WretchedWraithUnit) unitUsing).getCastBlizzardGoal().startCasting();
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((WretchedWraithUnit) unitUsing).getCastBlizzardGoal().setAbility(this);
        ((WretchedWraithUnit) unitUsing).getCastBlizzardGoal().startCasting();
    }
}
