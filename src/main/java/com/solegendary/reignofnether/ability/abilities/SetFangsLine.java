package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.enchantments.VigorEnchantment;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class SetFangsLine extends Ability {

    public static final int CD_MAX_SECONDS = 7;

    public SetFangsLine() {
        super(UnitAction.SET_FANGS_LINE,
            CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND,
            EvokerUnit.FANGS_RANGE_LINE,
            0,
            true
        );
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof EvokerUnit evokerUnit))
            return null;
        return new AbilityButton("Evoker Fangs (Line)",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/evoker_fangs_line.png"),
            hotkey,
            () -> evokerUnit.isUsingLineFangs,
            () -> false,
            () -> true,
            () -> UnitClientEvents.sendUnitCommand(UnitAction.SET_FANGS_LINE),
            null,
            List.of(FormattedCharSequence.forward(
                    I18n.get("abilities.reignofnether.evoker_fangs_line"),
                    Style.EMPTY.withBold(true)
                ),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.evoker_fangs_line.tooltip1",
                    EvokerUnit.FANGS_DAMAGE * 2,
                    CD_MAX_SECONDS
                ) + EvokerUnit.FANGS_RANGE_LINE, MyRenderer.iconStyle),
                FormattedCharSequence.forward(
                    I18n.get("abilities.reignofnether.evoker_fangs_line.tooltip2"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward(
                    I18n.get("abilities.reignofnether.evoker_fangs_line.tooltip3"),
                    Style.EMPTY
                )
            ),
            this,
            unit
        );
    }

    public void setCooldownSingle(float cooldown, Unit unit) {
        super.setCooldown(cooldown, unit);
    }

    @Override
    public void setCooldown(float cooldown, Unit unit) {
        EvokerUnit evokerUnit = (EvokerUnit) unit;
        int vigorLevel = evokerUnit.getVigorLevel();
        if (vigorLevel > 0)
            cooldown *= Math.pow(VigorEnchantment.CD_MULTIPLIER, vigorLevel);

        super.setCooldown(cooldown, unit);
        for (Ability ability : evokerUnit.getAbilities().get())
            if (ability instanceof SetFangsCircle ab) {
                ab.setCooldownSingle(cooldown, unit);
            }
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        EvokerUnit evokerUnit = (EvokerUnit) unitUsing;
        evokerUnit.isUsingLineFangs = true;
    }

    @Override
    public boolean canBypassCooldown(Unit unit) {
        return true;
    }

    @Override
    public boolean shouldResetBehaviours() {
        return false;
    }
}
