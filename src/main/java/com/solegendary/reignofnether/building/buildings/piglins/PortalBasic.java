package com.solegendary.reignofnether.building.buildings.piglins;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

public class PortalBasic extends AbstractPortal {

    public final static String buildingName = "Basic Portal";
    public final static String structureName = "portal_basic";

    public final static ResourceCost cost = ResourceCosts.BASIC_PORTAL;

    public PortalBasic() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.GRAY_GLAZED_TERRACOTTA;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/gray_glazed_terracotta.png");
        this.canSetRallyPoint = false;

        this.productions.add(ProductionItems.RESEARCH_PORTAL_FOR_CIVILIAN, Keybindings.abilitySlot1);
        this.productions.add(ProductionItems.RESEARCH_PORTAL_FOR_MILITARY, Keybindings.abilitySlot2);
        this.productions.add(ProductionItems.RESEARCH_PORTAL_FOR_TRANSPORT, Keybindings.abilitySlot3);
    }

    @Override
    public PortalPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new PortalPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
    }

    @Override
    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/portal.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.PORTAL_BASIC,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.CENTRAL_PORTAL) || ResearchClient.hasCheat(
                "modifythephasevariance"),
            List.of(Component.translatable("buildings.reignofnether.portal_basic").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.portal_basic.tooltip1").getVisualOrderText(),
                Component.translatable("buildings.reignofnether.portal_basic.tooltip2").getVisualOrderText(),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.portal_basic.tooltip3").getVisualOrderText()
            ),
            this
        );
    }
}