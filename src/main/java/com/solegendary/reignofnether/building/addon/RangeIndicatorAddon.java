package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.NetherZone;
import com.solegendary.reignofnether.building.data.DataType;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public interface RangeIndicatorAddon extends BuildingAddon {
    DataType<Set<BlockPos>> BORDER_BPS_CACHE = DataType.createRegistered(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "border_bps_cache"), (nbt, server) -> new HashSet<>(), (netherZone -> new CompoundTag()), () -> new HashSet<>()); //Cache only, shouldn't be saved

    int getRange(BuildingPlacement placement);
    //void updateBorderBps(BuildingPlacement placement);
    //Set<BlockPos> getBorderBps(BuildingPlacement placement);
    boolean showOnlyWhenSelected(BuildingPlacement placement);

    default void updateBorderBps(BuildingPlacement placement) {
        if (!placement.level.isClientSide())
            return;
        Set<BlockPos> borderBps = placement.getDataStorage().getData(RangeIndicatorAddon.BORDER_BPS_CACHE);
        borderBps.clear();
        borderBps.addAll(MiscUtil.getRangeIndicatorCircleBlocks(placement.centrePos,
                getRange(placement) - TimeClientEvents.VISIBLE_BORDER_ADJ, placement.level));
    }

    default Set<BlockPos> getBorderBps(BuildingPlacement placement) {
        return placement.getDataStorage().getData(RangeIndicatorAddon.BORDER_BPS_CACHE);
    }
}
