package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.unit.UnitAction;
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

public class CastFangsLine extends Ability {

    public static final int CD_MAX_SECONDS = 10;

    private final EvokerUnit evokerUnit;

    public CastFangsLine(EvokerUnit evokerUnit) {
        super(
            UnitAction.CAST_EVOKER_FANGS_LINE,
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
            "Evoker Fangs (Line)",
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/shears.png"),
            hotkey,
            () -> {
                if (this.evokerUnit.getCastFangsLineGoal() != null)
                    if (this.evokerUnit.getCastFangsLineGoal().isCasting())
                        return true;
                return CursorClientEvents.getLeftClickAction() == UnitAction.CAST_EVOKER_FANGS_LINE;
            },
            () -> false,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.CAST_EVOKER_FANGS_LINE),
            null,
            List.of(
                FormattedCharSequence.forward("Evoker Fangs (Line)", Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("\uE006  " + EvokerUnit.getFangsDamage() + "  " + "\uE004  " + CD_MAX_SECONDS + "s  \uE005  " + EvokerUnit.getFangsRange(), MyRenderer.iconStyle),
                FormattedCharSequence.forward("After a short delay, summon a line of snapping", Style.EMPTY),
                FormattedCharSequence.forward("fangs towards the targeted location.", Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("Shares a cooldown with other fang abilities.", Style.EMPTY)
            ),
            this
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((EvokerUnit) unitUsing).getCastFangsLineGoal().setAbility(this);
        ((EvokerUnit) unitUsing).getCastFangsLineGoal().setTarget(targetBp);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((EvokerUnit) unitUsing).getCastFangsLineGoal().setAbility(this);
        ((EvokerUnit) unitUsing).getCastFangsLineGoal().setTarget(targetEntity);
    }

    public void setCooldownSingle(int cooldown) {
        super.setCooldown(cooldown);
    }

    @Override
    public void setCooldown(int cooldown) {
        super.setCooldown(cooldown);
        for (Ability ability : this.evokerUnit.getAbilities())
            if (ability instanceof CastFangsCircle ab)
                ab.setCooldownSingle(cooldown);
    }
}
