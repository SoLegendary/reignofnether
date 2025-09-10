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

public class BeaconStrength extends BeaconAbility {

    public final static MobEffect AURA_EFFECT = MobEffects.DAMAGE_BOOST;

    public BeaconStrength(BeaconPlacement beacon) {
        super(UnitAction.BEACON_STRENGTH, AURA_EFFECT, beacon);
        this.defaultHotkey = Keybindings.keyT;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
                "Strength Aura",
                new ResourceLocation("minecraft", "textures/mob_effect/strength.png"),
                hotkey,
                () -> beacon.getAuraEffect() == AURA_EFFECT,
                () -> false,
                () -> beacon.getUpgradeLevel() >= 5,
                () -> UnitClientEvents.sendUnitCommand(UnitAction.BEACON_STRENGTH),
                null,
                List.of(
                        fcs(I18n.get("ability.reignofnether.beacon_aura.strength"), true),
                        fcs(""),
                        fcs(I18n.get("ability.reignofnether.beacon_aura.strength.tooltip1")),
                        fcs(I18n.get("ability.reignofnether.beacon_aura.one_aura"))
                ),
                this
        );
    }
}