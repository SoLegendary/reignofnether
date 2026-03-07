package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class Explode extends Ability {
    public Explode() {
        super(
            UnitAction.EXPLODE,
            0,
            0,
            0,
            false
        );
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton(
            "Explode",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/creeper_explode.png"),
            hotkey,
            () -> false,//CursorClientEvents.getLeftClickAction() == UnitAction.EXPLODE,
            () -> false,
            () -> true,
            () -> UnitClientEvents.sendUnitCommand(UnitAction.EXPLODE),//CursorClientEvents.setLeftClickAction(UnitAction.EXPLODE),
            null,
            List.of(
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.explode"), Style.EMPTY)
            ),
            null,
            unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((CreeperUnit)unitUsing).startToExplode();
    }
}
