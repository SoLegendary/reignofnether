package com.solegendary.reignofnether.building;

import javax.annotation.Nullable;

public interface NetherConvertingBuilding {

    double getMaxNetherRange();

    double getStartingNetherRange();

    @Nullable NetherZone getNetherZone();

    void setNetherZone(NetherZone nz, boolean save);
}
