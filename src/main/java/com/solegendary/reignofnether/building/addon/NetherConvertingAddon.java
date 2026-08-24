package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.NetherZone;
import com.solegendary.reignofnether.building.data.DataType;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

public interface NetherConvertingAddon extends BuildingAddon {
    DataType<NetherZone> NETHER_ZONE_DATA_TYPE = DataType.createRegistered(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "nether_converting_addon"), (nbt, server) -> null, (netherZone -> new CompoundTag())); //Save and load is handeled extern

    double getMaxNetherRange(BuildingPlacement placement);

    double getStartingNetherRange(BuildingPlacement placement);

    @Nullable
    default NetherZone getNetherZone(BuildingPlacement placement) {
        return placement.getDataStorage().getData(NETHER_ZONE_DATA_TYPE);
    }

    default void setNetherZone(BuildingPlacement placement, NetherZone nz, boolean save) {
        if (placement.getDataStorage().getData(NETHER_ZONE_DATA_TYPE) == null) {
            placement.getDataStorage().setData(NETHER_ZONE_DATA_TYPE, nz);
            if (!placement.level.isClientSide()) {
                if (placement.level.getGameRules().getRule(GameRuleRegistrar.DO_NETHER_CONVERSION).get()) {
                    BuildingServerEvents.netherZones.add(nz);
                    if (save)
                        BuildingServerEvents.saveNetherZones((ServerLevel) placement.level);
                }
            }
        }
    }
}
