package com.solegendary.reignofnether.building;

import net.minecraft.core.BlockPos;

import java.util.Set;

public interface RangeIndicator {
    void updateBorderBps();
    Set<BlockPos> getBorderBps();
    boolean showOnlyWhenSelected();
}