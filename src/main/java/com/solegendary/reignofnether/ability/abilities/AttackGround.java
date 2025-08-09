package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.RangedFlyingAttackGroundGoal;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.LanguageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class AttackGround extends Ability {

    private static final int CD_MAX = 0;


    public AttackGround(float attackRange) {
        super(
                UnitAction.ATTACK_GROUND,
                CD_MAX,
                attackRange,
                0,
                false,
                false
        );
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton(
                "Attack Ground",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/fireball.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.ATTACK_GROUND,
                () -> false,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.ATTACK_GROUND),
                null,
                List.of(
                        FormattedCharSequence.forward(LanguageUtil.getTranslation("abilities.reignofnether.attack_ground"), Style.EMPTY)
                ),
                this,
                unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        RangedFlyingAttackGroundGoal<?> attackGroundGoal = ((RangedAttackerUnit)unitUsing).getRangedAttackGroundGoal();
        if (attackGroundGoal != null)
            attackGroundGoal.setGroundTarget(targetBp);
    }
}
