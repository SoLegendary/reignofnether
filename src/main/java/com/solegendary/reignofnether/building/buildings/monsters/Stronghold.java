package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.addon.GarrisonableBuilding;
import com.solegendary.reignofnether.building.buildings.placements.StrongholdPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Stronghold extends ProductionBuilding implements GarrisonableBuilding {
    public final static int MAX_OCCUPANTS = 7;

    public final static String buildingName = "Stronghold";
    public final static String structureName = "stronghold";
    public final static ResourceCost cost = ResourceCosts.STRONGHOLD;
    public final static int nightRange = 60;
    private final Set<BlockPos> nightBorderBps = new HashSet<>();
    public Stronghold() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.REINFORCED_DEEPSLATE;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/reinforced_deepslate_side.png");

        this.buildTimeModifier = 0.5f;

        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE);
        this.startingBlockTypes.add(Blocks.DEEPSLATE_TILE_SLAB);
        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE_WALL);
        this.startingBlockTypes.add(Blocks.DEEPSLATE);

        this.productions.add(ProductionItems.WARDEN, Keybindings.keyQ);

        setActiveAddon(GarrisonableBuilding.class, this, true);
    }

    public Faction getFaction() {
        return Faction.MONSTERS;
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new StrongholdPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), true, nightRange,false, true);
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/reinforced_deepslate_side.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.STRONGHOLD,
            () -> false,
            () -> (
                BuildingClientEvents.hasFinishedBuilding(Buildings.LABORATORY)
                    && BuildingClientEvents.hasFinishedBuilding(Buildings.SPIDER_LAIR)
                    && BuildingClientEvents.hasFinishedBuilding(Buildings.DUNGEON)
            ) || ResearchClient.hasCheat("modifythephasevariance"),
            List.of(FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.stronghold"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.stronghold.tooltip1"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.stronghold.tooltip2",
                    StrongholdPlacement.MAX_OCCUPANTS
                ), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.stronghold.tooltip3",
                    nightRange
                ), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.stronghold.tooltip4"),
                    Style.EMPTY
                )
            ),
            this
        );
    }

    // don't use this for abilities as it may not be balanced
    @Override
    public int getAttackRange() {
        return 30;
    }

    // bonus for units attacking garrisoned units
    @Override
    public int getExternalAttackRangeBonus() {
        return 15;
    }

    @Override
    public boolean canDestroyBlock(BlockPos relativeBp, BuildingPlacement placement) {
        return relativeBp.getY() != 13 && relativeBp.getY() != 14;
    }

    @Override
    public BlockPos getIndoorSpawnPoint(ServerLevel level, BuildingPlacement placement) {
        return getExitPosition(placement);
    }

    @Override
    public BlockPos getEntryPosition(BuildingPlacement placement) {
        return placement.originPos.offset(GarrisonableBuilding.rotatePos(new BlockPos(5, 14, 5), placement.rotation));
    }

    @Override
    public BlockPos getExitPosition(BuildingPlacement placement) {
        return placement.originPos.offset(GarrisonableBuilding.rotatePos(new BlockPos(5, 2, 6), placement.rotation));
    }

    @Override
    public int getCapacity() { return MAX_OCCUPANTS; }
}
