package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import com.solegendary.reignofnether.unit.units.monsters.WraithUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class Fear extends Ability {

    public static final int CD_MAX_SECONDS = 20;
    public static final int RANGE = 4;
    public static final int DURATION_SECONDS = 4;

    public Fear() {
        super(
            UnitAction.FEAR,
            CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND,
            RANGE,
            0,
            true,
            true
        );
        this.autocastEnableAction = UnitAction.SPIN_WEBS_AUTOCAST_ENABLE;
        this.autocastDisableAction = UnitAction.SPIN_WEBS_AUTOCAST_DISABLE;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton(
                "Fear",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/chilling_screech.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.FEAR || isAutocasting(unit),
                () -> false,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.FEAR),
                () -> toggleAutocast(unit),
                List.of(
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.fear"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("\uE004  " + CD_MAX_SECONDS + "s  \uE005  " + RANGE, MyRenderer.iconStyle),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.fear.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.fear.tooltip2", DURATION_SECONDS), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.autocast"), Style.EMPTY)
                ),
                this,
                unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (!isOffCooldown(unitUsing))
            return;
        if (unitUsing instanceof WraithUnit wraithUnit) {
            wraithUnit.getFearGoal().setAbility(this);
            wraithUnit.getFearGoal().setTarget(targetEntity);
        }
    }
}
