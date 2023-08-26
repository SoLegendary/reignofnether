package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class CastFangsCircle extends Ability {

    public static final int CD_MAX_SECONDS = 10;

    private final EvokerUnit evokerUnit;

    public CastFangsCircle(EvokerUnit evokerUnit) {
        super(
            UnitAction.CAST_EVOKER_FANGS_CIRCLE,
            CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND,
            EvokerUnit.getFangsRange(),
            0,
            true
        );
        this.evokerUnit = evokerUnit;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
            "Evoker Fangs (Circular)",
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/shears.png"),
            hotkey,
            () -> {
                if (this.evokerUnit.getCastFangsCircleGoal() != null)
                    return this.evokerUnit.getCastFangsCircleGoal().isCasting();
                return false;
            },
            () -> false,
            () -> true,
            () -> UnitClientEvents.sendUnitCommand(UnitAction.CAST_EVOKER_FANGS_CIRCLE),
            null,
            List.of(
                FormattedCharSequence.forward("Evoker Fangs (Circular)", Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("\uE006  " + EvokerUnit.getFangsDamage() + "  " + "\uE004  " + CD_MAX_SECONDS + "s  \uE005  " + 3, MyRenderer.iconStyle),
                FormattedCharSequence.forward("After a short delay, summon a circle of snapping", Style.EMPTY),
                FormattedCharSequence.forward("fangs around the caster.", Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("Shares a cooldown with other fang abilities.", Style.EMPTY)
            ),
            this
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((EvokerUnit) unitUsing).getCastFangsCircleGoal().setAbility(this);
        ((EvokerUnit) unitUsing).getCastFangsCircleGoal().startCasting();
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((EvokerUnit) unitUsing).getCastFangsCircleGoal().setAbility(this);
        ((EvokerUnit) unitUsing).getCastFangsCircleGoal().startCasting();
    }

    public void setCooldownSingle(int cooldown) {
        super.setCooldown(cooldown);
    }

    @Override
    public void setCooldown(int cooldown) {
        super.setCooldown(cooldown);
        for (Ability ability : this.evokerUnit.getAbilities())
            if (ability instanceof CastFangsLine ab)
                ab.setCooldownSingle(cooldown);
    }
}
