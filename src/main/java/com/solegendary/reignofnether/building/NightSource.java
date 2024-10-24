package com.solegendary.reignofnether.building;

import net.minecraft.core.BlockPos;

import java.util.List;

public interface NightSource {
    public int getNightRange();
    public BlockPos getNightCentre();
    public void updateNightBorderBps();
    public List<BlockPos> getNightBorderBps();
}
