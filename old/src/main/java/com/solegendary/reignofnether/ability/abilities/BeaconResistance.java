package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.BeaconAbility;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class BeaconResistance extends BeaconAbility {

    public final static MobEffect AURA_EFFECT = MobEffects.DAMAGE_RESISTANCE;

    public BeaconResistance(BeaconPlacement beacon) {
        super(UnitAction.BEACON_RESISTANCE, AURA_EFFECT, beacon);
        this.defaultHotkey = Keybindings.keyR;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
                "Resistance Aura",
                new ResourceLocation("minecraft", "textures/mob_effect/resistance.png"),
                hotkey,
                () -> beacon.getAuraEffect() == AURA_EFFECT,
                () -> false,
                () -> beacon.getUpgradeLevel() >= 4,
                () -> UnitClientEvents.sendUnitCommand(UnitAction.BEACON_RESISTANCE),
                null,
                List.of(
                        fcs(I18n.get("ability.reignofnether.beacon_aura.resistance"), true),
                        fcs(""),
                        fcs(I18n.get("ability.reignofnether.beacon_aura.resistance.tooltip1")),
                        fcs(I18n.get("ability.reignofnether.beacon_aura.one_aura"))
                ),
                this
        );
    }
}
