package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit;
import com.solegendary.reignofnether.unit.units.monsters.StrayUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class Mount extends Ability {

    private final LivingEntity entity;

    public Mount(LivingEntity entity) {
        super(
            UnitAction.MOUNT,
            0,
            3,
            0,
            true
        );
        this.entity = entity;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
            "Mount",
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/saddle.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.MOUNT,
            entity::isPassenger,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.MOUNT),
            null,
            List.of(
                FormattedCharSequence.forward("Mount", Style.EMPTY)
            ),
            this
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (unitUsing instanceof SkeletonUnit)
            ((SkeletonUnit) unitUsing).getMountGoal().setTarget(targetEntity);
        else if (unitUsing instanceof StrayUnit)
            ((StrayUnit) unitUsing).getMountGoal().setTarget(targetEntity);
        else if (unitUsing instanceof PillagerUnit)
            ((PillagerUnit) unitUsing).getMountGoal().setTarget(targetEntity);
    }
}
