package com.solegendary.reignofnether.ability.heroAbilities.enchanter;

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
import com.solegendary.reignofnether.unit.units.villagers.EnchanterUnit;
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

public class MarchOfProgress extends HeroAbility {

    private static final int CD_MAX = 3 * ResourceCost.TICKS_PER_SECOND;

    public static final int RADIUS = 15;

    public static final int MANA_COST_PER_SECOND = 3;

    public MarchOfProgress() {
        super(1, 0, UnitAction.MARCH_OF_PROGRESS_TOGGLE,  CD_MAX, 0, RADIUS, false);
    }

    @Override
    public boolean isCasting(Unit hero) {
        if (hero instanceof EnchanterUnit enchanterUnit) {
            GenericUntargetedSpellGoal goal = enchanterUnit.getCastAuraGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("March of Progress",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/march_of_progress.png"),
                hotkey,
                () -> ((EnchanterUnit) unit).auraEnabled,
                () -> getRank(hero) == 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.MARCH_OF_PROGRESS_TOGGLE),
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
                "March of Progress",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/march_of_progress.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.march_of_progress"), true),
                fcsIcons(I18n.get("abilities.reignofnether.march_of_progress.stats", CD_MAX / 20, radius, MANA_COST_PER_SECOND)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.march_of_progress.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.march_of_progress.tooltip2", MANA_COST_PER_SECOND))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.march_of_progress"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.march_of_progress.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.march_of_progress.tooltip2", MANA_COST_PER_SECOND))
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((EnchanterUnit) unitUsing).getCastAuraGoal().setAbility(this);
        ((EnchanterUnit) unitUsing).getCastAuraGoal().startCasting();
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((EnchanterUnit) unitUsing).getCastAuraGoal().setAbility(this);
        ((EnchanterUnit) unitUsing).getCastAuraGoal().startCasting();
    }
}
