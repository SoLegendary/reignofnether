package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.ability.abilities.Sacrifice;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.NightSourceAddon;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.building.buildings.placements.SculkCatalystPlacement;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.util.MiscUtil;
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

public class SculkCatalyst extends Building implements NightSourceAddon, RangeIndicatorAddon {
    //TODO public static final DataType<ArrayList<BlockPos>> SCULK_BPS_CACHE = DataType.createRegistered(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "sculk_bps_cache"), (nbt, server) -> new ArrayList<>(), (netherZone -> new CompoundTag()), () -> new ArrayList<>()); //Cache only, shouldn't be saved

    public final static String buildingName = "Sculk Catalyst";
    public final static String structureName = "sculk_catalyst";
    public final static ResourceCost cost = ResourceCosts.SCULK_CATALYST;

    // vanilla logic determines the actual range, but this is what we're guessing it to be for the range limiter
    // mixin and the dirt path fix
    public final static int ESTIMATED_RANGE = 10;
    public final static int nightRangeMin = 25;
    public final static int nightRangeMax = 50;

    public SculkCatalyst() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.SCULK_CATALYST;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/sculk_catalyst_side.png");

        this.buildTimeModifier = 2.5f;

        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE);

        this.abilities.add(new Sacrifice(), Keybindings.keyQ);

        setActiveAddon(NightSourceAddon.class, this, true);
        setActiveAddon(RangeIndicatorAddon.class, this, true);
    }

    public Faction getFaction() {
        return Faction.MONSTERS;
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new SculkCatalystPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/sculk_catalyst_side.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.SCULK_CATALYST,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.MAUSOLEUM) || ResearchClient.hasCheat(
                "modifythephasevariance"),
            List.of(FormattedCharSequence.forward(
                    I18n.get("buildings.monsters.reignofnether.sculk_catalyst"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(
                    I18n.get("buildings.monsters.reignofnether.sculk_catalyst.tooltip1"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get(
                    "buildings.monsters.reignofnether.sculk_catalyst.tooltip2",
                    nightRangeMin
                ), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get(
                    "buildings.monsters.reignofnether.sculk_catalyst.tooltip3",
                    nightRangeMax
                ), Style.EMPTY),
                FormattedCharSequence.forward(
                    I18n.get("buildings.monsters.reignofnether.sculk_catalyst.tooltip4"),
                    Style.EMPTY
                )
            ),
            this
        );
    }

    public int getRange(BuildingPlacement placement) {
        if ((placement.isBuilt || placement.isBuiltServerside) && placement instanceof SculkCatalystPlacement scp) {
            return (int) Math.min(SculkCatalyst.nightRangeMin + (scp.sculkBps.size() * SculkCatalystPlacement.RANGE_PER_SCULK), SculkCatalyst.nightRangeMax);
        }
        return 0;
    }

    @Override
    public int getNightRange(BuildingPlacement placement) {
        return getRange(placement);
    }

    @Override
    public void updateHighlightBps(BuildingPlacement placement) {
        if (!placement.level.isClientSide()) {
            return;
        }
        if (placement instanceof SculkCatalystPlacement scp) {
            scp.updateSculkBps();
        }
        placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE).clear();
        placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE).addAll(MiscUtil.getRangeIndicatorCircleBlocks(placement.centrePos,
                getNightRange(placement) - BlockClientEvents.VISIBLE_BORDER_ADJ,
                placement.level, true
        ));
        if (CursorClientEvents.getLeftClickAction() == UnitAction.SACRIFICE) {
            placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE).addAll(MiscUtil.getRangeIndicatorCircleBlocks(placement.centrePos,
                    Sacrifice.RANGE - 1,
                    placement.level
            ));
        }

    }

    @Override
    public boolean showOnlyWhenSelected(BuildingPlacement placement) {
        return false;
    }
}
