package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.ConnectPortal;
import com.solegendary.reignofnether.ability.abilities.DisconnectPortal;
import com.solegendary.reignofnether.ability.abilities.GotoPortal;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

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

        Ability connectPortal = new ConnectPortal();
        this.abilities.add(connectPortal, Keybindings.keyQ);
        Ability gotoPortal = new GotoPortal();
        this.abilities.add(gotoPortal, Keybindings.keyW);
        Ability disconnectPortal = new DisconnectPortal();
        this.abilities.add(disconnectPortal, Keybindings.keyE);

        this.productions.add(ProductionItems.RESEARCH_PORTAL_FOR_CIVILIAN, Keybindings.keyQ);
        this.productions.add(ProductionItems.RESEARCH_PORTAL_FOR_MILITARY, Keybindings.keyW);
        this.productions.add(ProductionItems.RESEARCH_PORTAL_FOR_TRANSPORT, Keybindings.keyE);
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
            null,
                (BuildingPlacement) null
        );
    }
}




















