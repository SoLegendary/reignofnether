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

public class BeaconHaste extends BeaconAbility {

    public final static MobEffect AURA_EFFECT = MobEffects.DIG_SPEED;

    public BeaconHaste(BeaconPlacement beacon) {
        super(UnitAction.BEACON_HASTE, AURA_EFFECT, beacon);
        this.defaultHotkey = Keybindings.keyW;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
                "Haste Aura",
                new ResourceLocation("minecraft", "textures/mob_effect/haste.png"),
                hotkey,
                () -> beacon.getAuraEffect() == AURA_EFFECT,
                () -> false,
                () -> beacon.getUpgradeLevel() >= 2,
                () -> UnitClientEvents.sendUnitCommand(UnitAction.BEACON_HASTE),
                null,
                List.of(
                        fcs(I18n.get("ability.reignofnether.beacon_aura.haste"), true),
                        fcs(""),
                        fcs(I18n.get("ability.reignofnether.beacon_aura.haste.tooltip1")),
                        fcs(I18n.get("ability.reignofnether.beacon_aura.one_aura"))
                ),
                this
        );
    }
}