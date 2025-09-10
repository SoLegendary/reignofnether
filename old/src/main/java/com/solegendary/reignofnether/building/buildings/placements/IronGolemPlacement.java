package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;

public class IronGolemPlacement extends BuildingPlacement {
    public IronGolemPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        if (!this.getLevel().isClientSide()) {
            this.destroy((ServerLevel) this.getLevel());
            Entity entity = EntityRegistrar.IRON_GOLEM_UNIT.get().spawn((ServerLevel) this.getLevel(),
                    (CompoundTag) null,
                    null,
                    this.centrePos.offset(0, -1, 0),
                    MobSpawnType.SPAWNER,
                    true,
                    false
            );
            if (entity instanceof Unit unit) {
                unit.setOwnerName(ownerName);
            }
        }
    }
}
