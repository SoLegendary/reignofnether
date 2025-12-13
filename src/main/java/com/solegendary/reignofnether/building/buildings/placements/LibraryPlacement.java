package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.ability.EnchantAbility;
import com.solegendary.reignofnether.ability.abilities.*;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class LibraryPlacement extends ProductionPlacement {
    public EnchantAbility autoCastEnchant = null;
    public LibraryPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        if (tickAgeAfterBuilt > 0 && tickAgeAfterBuilt % 15 == 0 && isBuilt && autoCastEnchant != null
                && autoCastEnchant.isOffCooldown(this)) {

            List<Mob> mobs = new ArrayList<>();
            for (Mob e : MiscUtil.getEntitiesWithinRange(new Vector3d(
                    this.centrePos.getX(),
                    this.centrePos.getY(),
                    this.centrePos.getZ()
                ),
                autoCastEnchant.range - 1,
                Mob.class,
                tickLevel
            )) {
                if ((
                    autoCastEnchant.isCorrectUnitAndEquipment(e) && autoCastEnchant.canAfford(this)
                    && !autoCastEnchant.hasAnyEnchant(e)
                )) {
                    mobs.add(e);
                }
            }

            if (!mobs.isEmpty()) {
                autoCastEnchant.use(tickLevel, this, mobs.get(0));
            }
        }
    }
}
