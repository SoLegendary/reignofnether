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

public class BeaconRegeneration extends BeaconAbility {

    public final static MobEffect AURA_EFFECT = MobEffects.REGENERATION;

    public BeaconRegeneration(BeaconPlacement beacon) {
        super(UnitAction.BEACON_REGENERATION, AURA_EFFECT, beacon);
        this.defaultHotkey = Keybindings.keyE;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
                "Regeneration Aura",
                new ResourceLocation("minecraft", "textures/mob_effect/regeneration.png"),
                hotkey,
                () -> beacon.getAuraEffect() == AURA_EFFECT,
                () -> false,
                () -> beacon.getUpgradeLevel() >= 3,
                () -> UnitClientEvents.sendUnitCommand(UnitAction.BEACON_REGENERATION),
                null,
                List.of(
                        fcs(I18n.get("ability.reignofnether.beacon_aura.regeneration"), true),
                        fcs(""),
                        fcs(I18n.get("ability.reignofnether.beacon_aura.regeneration.tooltip1")),
                        fcs(I18n.get("ability.reignofnether.beacon_aura.one_aura"))
                ),
                this
        );
    }
}