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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class Eject extends Ability {
    public Eject() {
        super(
            UnitAction.EJECT,
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
            "Eject",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.EJECT,
            () -> entity.getPassengers().isEmpty(),
            () -> true,
            () -> UnitClientEvents.sendUnitCommand(UnitAction.EJECT),
            null,
            List.of(
                fcs(I18n.get("abilities.reignofnether.eject"))
            ),
            this,
            unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        for (Entity entity : ((LivingEntity) unitUsing).getPassengers()) {
            if (entity instanceof Unit unit)
                Unit.fullResetBehaviours(unit);
        }
        ((LivingEntity) unitUsing).ejectPassengers();

        for (Ability ability : unitUsing.getAbilities().get())
            if (ability instanceof SpinWebs spinWebs)
                spinWebs.setCooldown(spinWebs.getCooldown(unitUsing) / 4f, unitUsing);
    }
}
