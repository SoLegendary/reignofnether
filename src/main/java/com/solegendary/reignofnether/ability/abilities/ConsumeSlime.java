package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SlimeUnit;
import com.solegendary.reignofnether.unit.units.piglins.MagmaCubeUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class ConsumeSlime extends Ability {

    private static final int CD_MAX = 0;
    private static final int RANGE = 2;

    public ConsumeSlime() {
        super(UnitAction.CONSUME_SLIME, CD_MAX, RANGE, 0, true, true);
        this.autocastEnableAction = UnitAction.CONSUME_SLIME_AUTOCAST_ENABLE;
        this.autocastDisableAction = UnitAction.CONSUME_SLIME_AUTOCAST_DISABLE;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        SlimeUnit slime = (SlimeUnit) unit;
        return new AbilityButton("Consume",
            slime instanceof MagmaCubeUnit ?
                    ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/magma_cube.png") :
                    ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/slime.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.CONSUME_SLIME || isAutocasting(unit),
            () -> slime.getSize() <= 1,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.CONSUME_SLIME),
            () -> toggleAutocast(unit),
            List.of(FormattedCharSequence.forward(I18n.get("abilities.reignofnether.consume"),
                    Style.EMPTY.withBold(true)
                ),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.consume.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.consume.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.autocast"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.consume.tooltip3"), Style.EMPTY)
            ),
            this,
            unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (unitUsing instanceof SlimeUnit unit &&
            targetEntity instanceof SlimeUnit unitTarget &&
            unit.getOwnerName().equals(unitTarget.getOwnerName()) &&
            (unit.getSize() < unit.MAX_SIZE || unit.getHealth() < unit.getMaxHealth()) &&
            unit.getSize() >= unitTarget.getSize()) {
            unit.setUnitAttackTargetForced(unitTarget);
            unit.consumeTarget = unitTarget;
        } else if (level.isClientSide()) {
            if (unitUsing instanceof SlimeUnit unit &&
                unit.getSize() >= unit.MAX_SIZE &&
                unit.getHealth() >= unit.getMaxHealth()) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.consume.error1"));
            } else if (unitUsing instanceof SlimeUnit unit &&
                    targetEntity instanceof SlimeUnit unitTarget &&
                    unit.getSize() < unitTarget.getSize()) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.consume.error3"));
            } else {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.consume.error2"));
            }
        }
    }
}
