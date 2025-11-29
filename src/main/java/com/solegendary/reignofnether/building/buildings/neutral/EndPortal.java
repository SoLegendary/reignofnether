package com.solegendary.reignofnether.building.buildings.neutral;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.EndPortalPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.faction.Faction;
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

public class EndPortal extends ProductionBuilding {

    public final static String buildingName = "End Portal";
    public final static String structureName = "end_portal";
    public final static ResourceCost cost = ResourceCost.Building(0,0,0,0);

    public EndPortal() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.END_PORTAL_FRAME;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/end_portal_frame_top.png");

        this.selfBuilding = true;

        this.capturable = true;
        this.invulnerable = true;
        this.shouldDestroyOnReset = false;

        this.startingBlockTypes.add(Blocks.DARK_PRISMARINE_STAIRS);

        this.explodeChance = 0.2f;

        this.productions.add(ProductionItems.ENDERMAN, Keybindings.keyQ);
    }

    public Faction getFaction() {return Faction.NONE;}

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new EndPortalPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        return new BuildingPlaceButton(
                buildingName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/end_portal_frame_top.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                () -> false,
                () -> true,
                List.of(
                        FormattedCharSequence.forward(I18n.get("buildings.neutral.reignofnether.end_portal"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.neutral.reignofnether.end_portal.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.neutral.reignofnether.end_portal.tooltip2"), Style.EMPTY)
                ),
                this
        );
    }
}
