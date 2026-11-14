package com.solegendary.reignofnether.ability.heroAbilities.witherborn;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericTargetedSpellGoal;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.WitherbornUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class EssenceDrain extends HeroAbility {

    public static final int RANGE = 10;
    public static final int CHANNEL_SECONDS = 15;
    public static final int CHANNEL_TICKS = CHANNEL_SECONDS * ResourceCost.TICKS_PER_SECOND;
    public static final int DAMAGE_PER_SECOND_RANK_1 = 3;
    public static final int DAMAGE_PER_SECOND_RANK_2 = 5;
    public static final int DAMAGE_PER_SECOND_RANK_3 = 7;

    public EssenceDrain() {
        super(3, 50, UnitAction.ESSENCE_DRAIN, 25 * ResourceCost.TICKS_PER_SECOND, RANGE, 0, true);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof WitherbornUnit witherbornUnit) {
            GenericTargetedSpellGoal goal = witherbornUnit.getCastEssenceDrainGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Essence Drain",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/essence_drain.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.ESSENCE_DRAIN,
                () -> getRank(hero) == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.ESSENCE_DRAIN),
                null,
                getTooltipLines(hero),
                this,
                hero
        );
        button.innerIconSizeModifier = 1;
        return button;
    }

    @Override
    public Button getRankUpButton(Keybinding hotkey, HeroUnit hero) {
        return super.getRankUpButtonProtected(
                hotkey,
                "Essence Drain",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/essence_drain.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.essence_drain") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.essence_drain.stats", cooldownMax / 20, CHANNEL_SECONDS, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.essence_drain.tooltip1", CHANNEL_SECONDS)),
                fcs(I18n.get("abilities.reignofnether.essence_drain.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.essence_drain.tooltip3", RANGE))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.essence_drain"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.essence_drain.tooltip1", CHANNEL_SECONDS)),
                fcs(I18n.get("abilities.reignofnether.essence_drain.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.essence_drain.tooltip3", RANGE)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.essence_drain.rank1", DAMAGE_PER_SECOND_RANK_1), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.essence_drain.rank2", DAMAGE_PER_SECOND_RANK_2), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.essence_drain.rank3", DAMAGE_PER_SECOND_RANK_3), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((WitherbornUnit) unitUsing).getCastEssenceDrainGoal().setAbility(this);
        ((WitherbornUnit) unitUsing).getCastEssenceDrainGoal().setTarget(targetBp);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (targetEntity == unitUsing)
            return;
        ((WitherbornUnit) unitUsing).getCastEssenceDrainGoal().setAbility(this);
        ((WitherbornUnit) unitUsing).getCastEssenceDrainGoal().setTarget(targetEntity);
    }
}
