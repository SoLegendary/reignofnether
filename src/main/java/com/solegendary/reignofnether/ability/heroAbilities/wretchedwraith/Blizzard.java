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

public class Blizzard extends HeroAbility {

    private static final int CD_MAX_SECONDS = 300 * ResourceCost.TICKS_PER_SECOND;
    public static final int DURATION_SECONDS = 10 * ResourceCost.TICKS_PER_SECOND;
    public static final int RADIUS = 10;

    public Blizzard() {
        super(3, 100, UnitAction.BLIZZARD, CD_MAX_SECONDS, 0, RADIUS, false);
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
        return new AbilityButton("Blizzard",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/blue_ice.png"),
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
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Blizzard",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/blue_ice.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.blizzard") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.blizzard.stats", CD_MAX_SECONDS / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.blizzard.tooltip1"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.blizzard"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.blizzard.tooltip1")),
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
