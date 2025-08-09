package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.BeaconAbility;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.LanguageUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class BeaconRegeneration extends BeaconAbility {

    public final static MobEffect AURA_EFFECT = MobEffects.REGENERATION;

    public BeaconRegeneration() {
        super(UnitAction.BEACON_REGENERATION, AURA_EFFECT);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        if (!(placement instanceof BeaconPlacement beacon)) return null;

        return new AbilityButton(
                "Regeneration Aura",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/regeneration.png"),
                hotkey,
                () -> beacon.getAuraEffect() == AURA_EFFECT,
                () -> false,
                () -> beacon.getUpgradeLevel() >= 3,
                () -> UnitClientEvents.sendUnitCommand(UnitAction.BEACON_REGENERATION),
                null,
                List.of(
                        fcs(LanguageUtil.getTranslation("ability.reignofnether.beacon_aura.regeneration"), true),
                        fcs(""),
                        fcs(LanguageUtil.getTranslation("ability.reignofnether.beacon_aura.regeneration.tooltip1")),
                        fcs(LanguageUtil.getTranslation("ability.reignofnether.beacon_aura.one_aura"))
                ),
                this,
                placement
        );
    }
}