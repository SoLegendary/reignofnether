package com.solegendary.reignofnether.building.buildings.neutral;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

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

        this.productions.add(ProductionItems.ENDERMAN, Keybindings.abilitySlot1);
    }

    public Faction getFaction() {return Factions.NEUTRAL;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        return new BuildingPlaceButton(
                buildingName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/end_portal_frame_top.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                () -> false,
                () -> true,
                List.of(
                        Component.translatable("buildings.reignofnether.end_portal").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        Component.translatable("buildings.reignofnether.end_portal.tooltip1").getVisualOrderText(),
                        Component.translatable("buildings.reignofnether.end_portal.tooltip2").getVisualOrderText()
                ),
                this
        );
    }

    @Override
    public void onBuilt(BuildingPlacement placement) {
        Level level = placement.level;
        BlockPos centrePos = placement.centrePos;
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(centrePos.above(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().north(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().south(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().east(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().west(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().north().east(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().north().west(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().south().east(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().south().west(), Blocks.END_PORTAL.defaultBlockState());
        }
    }
}
