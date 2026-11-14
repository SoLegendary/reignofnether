package com.solegendary.reignofnether.ability.heroAbilities.witherborn;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericUntargetedSpellGoal;
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

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class WorldOfRot extends HeroAbility {

    public static final int CAST_TICKS = 60;
    public static final int DEBUFF_PERCENT = 20; // weakness and slow
    public static final int DURATION_SECONDS = 30;
    public static final int MAX_DURATION_SECONDS = 60;

    public WorldOfRot() {
        super(1, 125, UnitAction.WORLD_OF_ROT, 120 * ResourceCost.TICKS_PER_SECOND, 0, 0, false);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof WitherbornUnit witherbornUnit) {
            GenericUntargetedSpellGoal goal = witherbornUnit.getCastWorldOfRotGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("World of Rot",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/world_of_rot.png"),
                hotkey,
                () -> false,
                () -> getRank(hero) == 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.WORLD_OF_ROT),
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
                "World of Rot",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/world_of_rot.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.world_of_rot") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.world_of_rot.stats", cooldownMax / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.world_of_rot.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.world_of_rot.tooltip2", DEBUFF_PERCENT, DURATION_SECONDS)),
                fcs(I18n.get("abilities.reignofnether.world_of_rot.tooltip3", MAX_DURATION_SECONDS))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.world_of_rot"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.world_of_rot.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.world_of_rot.tooltip2", DEBUFF_PERCENT, DURATION_SECONDS)),
                fcs(I18n.get("abilities.reignofnether.world_of_rot.tooltip3", MAX_DURATION_SECONDS))
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((WitherbornUnit) unitUsing).getCastWorldOfRotGoal().setAbility(this);
        ((WitherbornUnit) unitUsing).getCastWorldOfRotGoal().startCasting();
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((WitherbornUnit) unitUsing).getCastWorldOfRotGoal().setAbility(this);
        ((WitherbornUnit) unitUsing).getCastWorldOfRotGoal().startCasting();
    }
}
