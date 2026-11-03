package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.BuildingAbilityServerboundPacket;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.GraveyardPlacement;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class SetGraveyardReleaseOn extends Ability {

    public SetGraveyardReleaseOn() {
        super(
            UnitAction.SET_GRAVEYARD_RELEASE_ON,
            0,
            0,
            0,
            false,
            false
        );
        this.defaultHotkey = Keybindings.abilitySlot9;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        if (!(placement instanceof GraveyardPlacement gy)) return null;

        return new AbilityButton(
            "Graveyard Release",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross.png"),
            hotkey,
            () -> false,
            () -> placement.getUpgradeLevel() == 0 || gy.autoRelease,
            () -> true,
            () -> {
                if (gy.getUpgradeLevel() >= 1)
                    BuildingAbilityServerboundPacket.doAbility(UnitAction.SET_GRAVEYARD_RELEASE_ON, gy.originPos, this.oneClickOneUse);
            },
            null,
            List.of(
                    fcs(I18n.get("abilities.reignofnether.graveyard_release_off"), true),
                    fcs(I18n.get("abilities.reignofnether.graveyard_release.tooltip1"))
            ),
            this,
            placement
        );
    }
}
