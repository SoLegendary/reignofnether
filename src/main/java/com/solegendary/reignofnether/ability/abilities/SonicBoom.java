package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.SonicBoomGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.WardenUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class SonicBoom extends Ability {

    public static final int CD_MAX_SECONDS = 60;

    public SonicBoom() {
        super(UnitAction.CAST_SONIC_BOOM,
            CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND,
            WardenUnit.SONIC_BOOM_RANGE,
            0,
            true,
            true
        );
    }

    @Override
    public boolean isCasting(Unit unit) {
        SonicBoomGoal goal = ((WardenUnit)unit).getSonicBoomGoal();
        if (goal == null)
            return false;
        return goal.isCasting() || goal.getMoveTarget() != null || goal.getTargetEntity() != null;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton("Sonic Boom",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/sonic_boom.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.CAST_SONIC_BOOM,
            () -> false,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.CAST_SONIC_BOOM),
            null,
            List.of(FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sonic_boom"),
                    Style.EMPTY.withBold(true)
                ),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sonic_boom.tooltip1",
                        WardenUnit.SONIC_BOOM_DAMAGE,
                        CD_MAX_SECONDS
                    ) + WardenUnit.SONIC_BOOM_RANGE,
                    MyRenderer.iconStyle
                ),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sonic_boom.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sonic_boom.tooltip3"), Style.EMPTY)
            ),
            this,
            unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (targetEntity instanceof Unit unit && unit.equals(unitUsing)) {
            return;
        }
        ((WardenUnit) unitUsing).getSonicBoomGoal().setAbility(this);
        ((WardenUnit) unitUsing).getSonicBoomGoal().setTarget(targetEntity);
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        BuildingPlacement targetBuilding = BuildingUtils.findBuilding(level.isClientSide(), targetBp);
        if (targetBuilding != null) {
            ((WardenUnit) unitUsing).getSonicBoomGoal().setAbility(this);
            ((WardenUnit) unitUsing).getSonicBoomGoal().setTarget(targetBuilding);
        } else if (level.isClientSide()) {
            HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.sonic_boom.error1"));
        }
    }
}
