package com.solegendary.reignofnether.ability.heroAbilities.wretchedwraith;

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

public class Blizzard extends HeroAbility {

    private static final int CD_MAX_SECONDS = 360 * ResourceCost.TICKS_PER_SECOND;
    public static final int CHANNEL_DURATION = 20 * ResourceCost.TICKS_PER_SECOND;
    public static final int FREEZE_DURATION = 8 * ResourceCost.TICKS_PER_SECOND;
    public static final int SNOWBALL_DAMAGE = 5;
    public static final int RADIUS = 20;

    public Blizzard() {
        super(1, 100, UnitAction.BLIZZARD, CD_MAX_SECONDS, 0, RADIUS, false);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof WretchedWraithUnit wraithUnit) {
            GenericUntargetedSpellGoal goal = wraithUnit.getCastBlizzardGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Blizzard",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/blizzard.png"),
                hotkey,
                () -> false,
                () -> getRank(hero) <= 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.BLIZZARD),
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
                "Blizzard",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/blizzard.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.blizzard") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.blizzard.stats", CD_MAX_SECONDS / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.blizzard.tooltip1", CHANNEL_DURATION / 20)),
                fcs(I18n.get("abilities.reignofnether.blizzard.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.blizzard.tooltip3"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.blizzard"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.blizzard.tooltip1", CHANNEL_DURATION / 20)),
                fcs(I18n.get("abilities.reignofnether.blizzard.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.blizzard.tooltip3"))
        );
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
