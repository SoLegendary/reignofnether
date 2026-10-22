package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import org.checkerframework.common.value.qual.ArrayLenRange;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class ToggleSiegeMode extends Ability {

    private static final int CD_MAX_SECONDS = 20;

    public ToggleSiegeMode() {
        super(UnitAction.TOGGLE_SIEGE_MODE, CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND, 0, 0, false, true);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton("Siege Mode",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/ravager_roar.png"),
            hotkey,
            () -> false,
            () -> false,
            () -> {
                if (unit instanceof RavagerUnit ravagerUnit) {
                    return switch (ravagerUnit.getSiegeState()) {
                        case NONE, SIEGED -> true;
                        case BURROWING, UNBURROWING -> false;
                    };
                }
                return false;
            },
            () -> UnitClientEvents.sendUnitCommand(UnitAction.TOGGLE_SIEGE_MODE),
            null,
            getTooltips(unit),
            this,
            unit
        );
    }

    private List<FormattedCharSequence> getTooltips(Unit unit) {
        if (unit instanceof RavagerUnit ravagerUnit && ravagerUnit.getSiegeState() == RavagerUnit.SiegeState.NONE) {
            return List.of(
                fcs(I18n.get("abilities.reignofnether.siege_mode"), true),
                fcs(I18n.get("abilities.reignofnether.siege_mode.enable.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.siege_mode.enable.tooltip2"))
            );
        } else {
            return List.of(
                fcs(I18n.get("abilities.reignofnether.siege_mode"), true),
                fcs(I18n.get("abilities.reignofnether.siege_mode.disable.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.siege_mode.disable.tooltip2"))
            );
        }
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (!(unitUsing instanceof RavagerUnit ravagerUnit)) return;
        unitUsing.resetBehaviours();
        ravagerUnit.toggleSiege();
    }
}
