package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.WindcallerUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Predicate;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ToggleFlying extends Ability {

    public ToggleFlying() {
        super(
                UnitAction.TOGGLE_FLYING,
                0,
                0,
                0,
                false
        );
    }

    private final static Predicate<LivingEntity> TOGGLE_CHECK = le ->
            HudClientEvents.hudSelectedEntity instanceof WindcallerUnit hudUnit &&
            le instanceof WindcallerUnit unit &&
            hudUnit.isFlying() == unit.isFlying();

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof WindcallerUnit windcallerUnit))
            return null;

        ResourceLocation rl = windcallerUnit.isFlying() ?
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/flying_land.png") :
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/flying_lift.png");

        List<FormattedCharSequence> tooltips = windcallerUnit.isFlying() ?
                List.of(fcs(I18n.get("abilities.reignofnether.flying_disable"))) :
                List.of(fcs(I18n.get("abilities.reignofnether.flying_enable")));

        return new AbilityButton(
                "Toggle Flying",
                rl,
                hotkey,
                () -> false,
                () -> false,
                () -> true,
                () -> sendUnitCommand(UnitAction.TOGGLE_FLYING, TOGGLE_CHECK),
                null,
                tooltips,
                this,
                unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (!(unitUsing instanceof WindcallerUnit windcallerUnit))
            return;
        windcallerUnit.toggleFlying();
        windcallerUnit.updateAbilityButtons();
    }

    @Override
    public boolean shouldResetBehaviours() { return false; }
}
