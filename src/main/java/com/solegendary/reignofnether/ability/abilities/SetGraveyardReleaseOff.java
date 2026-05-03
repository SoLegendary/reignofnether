package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.BuildingAbilityServerboundPacket;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.GraveyardPlacement;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class SetGraveyardReleaseOff extends Ability {

    public SetGraveyardReleaseOff() {
        super(
            UnitAction.SET_GRAVEYARD_RELEASE_OFF,
            0,
            0,
            0,
            false,
            true
        );
        this.defaultHotkey = Keybindings.keyO;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        if (!(placement instanceof GraveyardPlacement gy)) return null;

        return new AbilityButton(
            "Graveyard Release",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tick.png"),
            hotkey,
            () -> false,
            () -> placement.getUpgradeLevel() == 0 || !gy.autoRelease,
            () -> true,
            () -> {
                if (gy.getUpgradeLevel() >= 1)
                    BuildingAbilityServerboundPacket.doAbility(UnitAction.SET_GRAVEYARD_RELEASE_OFF, gy.originPos);
            },
            null,
            List.of(
                    FormattedCharSequence.forward(I18n.get("abilities.reignofnether.graveyard_release_on"), Style.EMPTY.withBold(true)),
                    FormattedCharSequence.forward(I18n.get("abilities.reignofnether.graveyard_release.tooltip1"), Style.EMPTY)
            ),
            this,
            placement
        );
    }
}
