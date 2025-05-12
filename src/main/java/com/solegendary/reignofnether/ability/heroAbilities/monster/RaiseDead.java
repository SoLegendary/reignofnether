package com.solegendary.reignofnether.ability.heroAbilities.monster;

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
    private static final int CD_MAX_SECONDS = 60 * ResourceCost.TICKS_PER_SECOND;

    public RaiseDead() {
        super(3, UnitAction.RAISE_DEAD, CD_MAX_SECONDS, 0, 0, false);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit hero) {
        return new AbilityButton("Raise Dead",
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/zombie.png"),
                hotkey,
                () -> false,
                () -> rank <= 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.RAISE_DEAD),
                null,
                getTooltipLines((HeroUnit) hero),
                this,
                hero
        );
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Raise Dead",
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/zombie.png"),
                hero
        );
    }

    private static final float BONUS_HEALTH_PER_SOUL = 2;
    private static final float BONUS_DAMAGE_PER_SOUL = 0.3f;

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.raise_dead") + " " + rankString(), true),
                fcsIcons(I18n.get("abilities.reignofnether.raise_dead.stats", CD_MAX_SECONDS / 20)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.raise_dead.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.raise_dead.tooltip2"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.raise_dead"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement()), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.raise_dead.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.raise_dead.tooltip2")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.raise_dead.rank1"), rank == 0),
                fcs(I18n.get("abilities.reignofnether.raise_dead.rank2"), rank == 1),
                fcs(I18n.get("abilities.reignofnether.raise_dead.rank3"), rank == 2)
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
