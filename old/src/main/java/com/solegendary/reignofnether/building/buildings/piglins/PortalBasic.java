package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.ConnectPortal;
import com.solegendary.reignofnether.ability.abilities.DisconnectPortal;
import com.solegendary.reignofnether.ability.abilities.GotoPortal;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class PortalBasic extends AbstractPortal {

    public final static String buildingName = "Basic Portal";
    public final static String structureName = "portal_basic";

    public final static ResourceCost cost = ResourceCosts.BASIC_PORTAL;

    public PortalBasic() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.GRAY_GLAZED_TERRACOTTA;
        this.icon = new ResourceLocation("minecraft", "textures/block/gray_glazed_terracotta.png");
        this.canSetRallyPoint = false;

        this.productions.add(ProductionItems.RESEARCH_PORTAL_FOR_CIVILIAN, Keybindings.keyQ);
        this.productions.add(ProductionItems.RESEARCH_PORTAL_FOR_MILITARY, Keybindings.keyW);
        this.productions.add(ProductionItems.RESEARCH_PORTAL_FOR_TRANSPORT, Keybindings.keyE);
    }

    @Override
    public PortalPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        PortalPlacement portalPlacement = new PortalPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
        Ability connectPortal = new ConnectPortal(portalPlacement);
        portalPlacement.getAbilities().add(connectPortal);
        Ability gotoPortal = new GotoPortal(portalPlacement);
        portalPlacement.getAbilities().add(gotoPortal);
        Ability disconnectPortal = new DisconnectPortal(portalPlacement);
        portalPlacement.getAbilities().add(disconnectPortal);
        return portalPlacement;
    }

    @Override
    public AbilityButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new AbilityButton(name,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/portal.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.PORTAL_BASIC,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.CENTRAL_PORTAL) || ResearchClient.hasCheat(
                "modifythephasevariance"),
            () -> BuildingClientEvents.setBuildingToPlace(Buildings.PORTAL_BASIC),
            null,
            List.of(FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.portal_basic"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.portal_basic.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.portal_basic.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.portal_basic.tooltip3"), Style.EMPTY)
            ),
            null
        );
    }
}




















