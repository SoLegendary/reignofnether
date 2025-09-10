package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class DisconnectPortal extends Ability {

    private static final int CD_MAX = 0;
    private static final int RANGE = 0;

    PortalPlacement portalPlacement;

    public DisconnectPortal(PortalPlacement portalPlacement) {
        super(
            UnitAction.DISCONNECT_PORTAL,
            portalPlacement.getLevel(),
            CD_MAX,
            RANGE,
            0,
            true
        );
        this.portalPlacement = portalPlacement;
        this.defaultHotkey = Keybindings.keyE;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
            "Sever Connection",
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
            hotkey,
            () -> false,
            () -> !portalPlacement.hasDestination(),
            () -> true,
            () -> UnitClientEvents.sendUnitCommand(UnitAction.DISCONNECT_PORTAL),
            null,
            List.of(
                    FormattedCharSequence.forward(I18n.get("abilities.reignofnether.disconnect_portal"), Style.EMPTY.withBold(true))
            ),
            this
        );
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, BlockPos targetBp) {
        if (buildingUsing == portalPlacement)
            portalPlacement.disconnectPortal();
    }
}
