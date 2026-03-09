package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.data.DataType;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public interface RangeIndicatorAddon extends BuildingAddon {
    DataType<Set<BlockPos>> HIGHLIGHT_BPS_CACHE = DataType.createRegistered(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "highlight_bps_cache"), (nbt, server) -> new HashSet<>(), (netherZone -> new CompoundTag()), () -> new HashSet<>()); //Cache only, shouldn't be saved

    int getRange(BuildingPlacement placement);
    //void updateBorderBps(BuildingPlacement placement);
    //Set<BlockPos> getBorderBps(BuildingPlacement placement);
    boolean showOnlyWhenSelected(BuildingPlacement placement);

    public default void updateHighlightBps(BuildingPlacement placement) {
        if (this instanceof Unit unit) {
            LivingEntity le = (LivingEntity) unit;
            if (le.level().isClientSide()) {
                setHighlightBps(placement, new HashSet<>());
                for (Ability ability : unit.getAbilities().get()) {
                    if (CursorClientEvents.getLeftClickAction() == ability.action) {
                        setHighlightBps(placement, MiscUtil.getRangeIndicatorCircleBlocks(le.blockPosition(),
                                (int) (ability.range - 1),
                                le.level()
                        ));
                    }
                }
            }
        } else if (this instanceof BuildingPlacement bpl) {
            if (bpl.level.isClientSide()) {
                setHighlightBps(placement, new HashSet<>());
                for (Ability ability : bpl.getAbilities()) {
                    if (CursorClientEvents.getLeftClickAction() == ability.action) {
                        setHighlightBps(placement, MiscUtil.getRangeIndicatorCircleBlocks(bpl.centrePos,
                                (int) (ability.range - 1),
                                bpl.level
                        ));
                    }
                }
            }
        }
    }
    default void setHighlightBps(BuildingPlacement placement, Set<BlockPos> bps) {
        placement.getDataStorage().setData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE, bps);
    }

    default Set<BlockPos> getHighlightBps(BuildingPlacement placement) {
        return placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE);
    }
}
