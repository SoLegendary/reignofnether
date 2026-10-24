package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.buttons.Button;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.checkerframework.common.value.qual.ArrayLenRange;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class ToggleSiegeMode extends Ability {

    private static final int CD_MAX_SECONDS = 20;
    private static final String BUTTON_NAME = "Siege Mode";

    // client only
    public static boolean isButtonHovered() {
        Button hovered = HudClientEvents.getMousedOverButton();
        return hovered != null && hovered.name.equals(BUTTON_NAME);
    }

    public ToggleSiegeMode() {
        super(UnitAction.TOGGLE_SIEGE_MODE, CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND, 0, 0, false, true);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton(BUTTON_NAME,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/siege_mode.png"),
            hotkey,
            () -> {
                if (unit instanceof RavagerUnit ravagerUnit) {
                    return ravagerUnit.getSiegeState() == RavagerUnit.SiegeState.SIEGED;
                }
                return false;
            },
            () -> !((LivingEntity) unit).isVehicle(),
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
