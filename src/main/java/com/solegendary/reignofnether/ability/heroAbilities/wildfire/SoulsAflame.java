package com.solegendary.reignofnether.ability.heroAbilities.wildfire;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.goals.GenericUntargetedSpellGoal;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.WildfireUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class SoulsAflame extends HeroAbility {

    private static final int CD_MAX = 360 * ResourceCost.TICKS_PER_SECOND;
    public static final int DURATION = 45 * ResourceCost.TICKS_PER_SECOND;
    public static final int RANGE = 20;

    public SoulsAflame() {
        super(1, 140, UnitAction.SOULS_AFLAME, CD_MAX, 20, 0, false);
    }

    @Override
    public boolean isCasting(Unit hero) {
        if (hero instanceof WildfireUnit wildfireUnit) {
            GenericUntargetedSpellGoal goal = wildfireUnit.getCastSoulsAflameGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Souls Aflame",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/souls_aflame.png"),
            hotkey,
            () -> false,
            () -> getRank(hero) == 0,
            () -> true,
            () -> UnitClientEvents.sendUnitCommand(UnitAction.SOULS_AFLAME),
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
                "Souls Aflame",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/souls_aflame.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.souls_aflame"), true),
                fcsIcons(I18n.get("abilities.reignofnether.souls_aflame.stats", CD_MAX / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.souls_aflame.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.souls_aflame.tooltip2", DURATION / 20)),
                fcs(I18n.get("abilities.reignofnether.souls_aflame.tooltip3"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.souls_aflame"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.souls_aflame.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.souls_aflame.tooltip2", DURATION / 20)),
                fcs(I18n.get("abilities.reignofnether.souls_aflame.tooltip3"))
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        use(level, unitUsing);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        use(level, unitUsing);
    }

    private void use(Level level, Unit unitUsing) {
        LivingEntity le = (LivingEntity) unitUsing;
        boolean isSoulsAflameActive = le.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get());

        if (level.isClientSide() && isSoulsAflameActive) {
            HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.souls_aflame.already_active"));
        }
        if (!isSoulsAflameActive) {
            ((WildfireUnit) unitUsing).getCastSoulsAflameGoal().setAbility(this);
            ((WildfireUnit) unitUsing).getCastSoulsAflameGoal().startCasting();
        }
    }
}
