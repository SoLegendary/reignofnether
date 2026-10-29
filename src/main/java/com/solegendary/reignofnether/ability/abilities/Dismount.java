package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class Dismount extends Ability {
    public Dismount() {
        super(
            UnitAction.DISMOUNT,
            0,
            0,
            0,
            false
        );
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        LivingEntity entity = (LivingEntity) unit;
        return new AbilityButton(
            "Dismount",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.DISMOUNT,
            () -> entity.getVehicle() == null,
            () -> true,
            () -> UnitClientEvents.sendUnitCommand(UnitAction.DISMOUNT),
            null,
            List.of(
                fcs(I18n.get("abilities.reignofnether.dismount"))
            ),
            this,
            unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((LivingEntity) unitUsing).stopRiding();
    }
}
