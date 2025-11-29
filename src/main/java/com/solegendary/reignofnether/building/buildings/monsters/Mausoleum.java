package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.placements.DarknessProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Mausoleum extends ProductionBuilding {

    public final static String buildingName = "Mausoleum";
    public final static String structureName = "mausoleum";
    public final static ResourceCost cost = ResourceCosts.MAUSOLEUM;
    public final static int nightRange = 80;
    public final static int nightRangeReduced = 40;

    private final Set<BlockPos> nightBorderBps = new HashSet<>();

    public Mausoleum() {
        super(structureName, cost, true);
        this.name = buildingName;
        this.portraitBlock = Blocks.DEEPSLATE_TILES;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/deepslate_tiles.png");
        this.foundationYLayers = 4;

        this.buildTimeModifier = 0.274f; // 60s total build time with 3 villagers
        this.canAcceptResources = true;

        this.startingBlockTypes.add(Blocks.STONE);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);
        this.startingBlockTypes.add(Blocks.STONE_BRICKS);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);

        this.productions.add(ProductionItems.ZOMBIE_VILLAGER, Keybindings.keyQ);
    }

    public Faction getFaction() {
        return Faction.MONSTERS;
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        int nRange = nightRange;
        if ((level.isClientSide() && BuildingClientEvents.playerHasFinishedBuilding(this, ownerName)) ||
            (!level.isClientSide() && BuildingServerEvents.playerHasFinishedBuilding(this, ownerName))) {
            nRange = nightRangeReduced;
        }
        return new DarknessProductionBuilding(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), true, nRange,false, true);
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/deepslate_tiles.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.MAUSOLEUM,
            () -> false,
            () -> true,
            List.of(FormattedCharSequence.forward(
                    I18n.get("buildings.monsters.reignofnether.mausoleum"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPop(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(
                    I18n.get("buildings.monsters.reignofnether.mausoleum.tooltip1"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.mausoleum.tooltip2",  nightRange), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.mausoleum.tooltip4",  nightRangeReduced), Style.EMPTY)
            ),
            this
        );
    }
}