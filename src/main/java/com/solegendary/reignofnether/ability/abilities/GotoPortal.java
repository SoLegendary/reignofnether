package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class GotoPortal extends Ability {

    private static final int CD_MAX = 0;
    private static final int RANGE = 0;


    public GotoPortal() {
        super(
            UnitAction.GOTO_PORTAL,
            CD_MAX,
            RANGE,
            0,
            true
        );
        this.defaultHotkey = Keybindings.keyW;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        if (!(placement instanceof PortalPlacement)) return null;
        PortalPlacement portal = (PortalPlacement) placement;
        return new AbilityButton(
                "Go to connected portal",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/map.png"),
                hotkey,
                () -> false,
                () -> {
                    // hidden if the portal does not have a connection Or isn't a transport portal
                    if (portal.getPortalType() != PortalPlacement.PortalType.TRANSPORT)
                        return true;
                    return !portal.hasDestination();
                },
                () -> true,
                () -> UnitClientEvents.sendUnitCommand(UnitAction.GOTO_PORTAL),
                null,
                List.of(
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.go_to_portal"), Style.EMPTY.withBold(true))
                ),
                this,
                placement
        );
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, BlockPos targetBp) {
        if (level.isClientSide() && buildingUsing instanceof PortalPlacement portalPlacement &&
            portalPlacement.hasDestination()) {
            BuildingPlacement targetBuilding = BuildingUtils.findBuilding(level.isClientSide(), portalPlacement.destination);
            if (targetBuilding instanceof PortalPlacement targetPortal && targetPortal.getPortalType() == PortalPlacement.PortalType.TRANSPORT)
                OrthoviewClientEvents.centreCameraOnPos(targetPortal.centrePos);
        }
    }
}
