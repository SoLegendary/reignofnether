package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.buildings.monsters.Laboratory;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.RangedAttackGroundGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

import java.util.List;

public class AttackGround extends Ability {

    private static final int CD_MAX = 0;

    private final RangedAttackerUnit rangedAttackerUnit;

    public AttackGround(RangedAttackerUnit rangedAttackerUnit) {
        super(
                UnitAction.ATTACK_GROUND,
                CD_MAX,
                ((AttackerUnit) rangedAttackerUnit).getAttackRange(),
                0,
                false
        );
        this.rangedAttackerUnit = rangedAttackerUnit;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
                "Attack Ground",
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/bow.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.ATTACK_GROUND,
                () -> false,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.ATTACK_GROUND),
                null,
                List.of(
                        FormattedCharSequence.forward("Attack Ground", Style.EMPTY)
                ),
                this
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        RangedAttackGroundGoal<?> attackGroundGoal = this.rangedAttackerUnit.getRangedAttackGroundGoal();
        if (attackGroundGoal != null)
            attackGroundGoal.setGroundTarget(targetBp);
    }
}
