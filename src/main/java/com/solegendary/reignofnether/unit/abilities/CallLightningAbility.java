package com.solegendary.reignofnether.unit.abilities;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.buildings.monsters.Laboratory;
import com.solegendary.reignofnether.unit.Ability;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

public class CallLightningAbility extends Ability {

    public CallLightningAbility() {
        super(
            UnitAction.CALL_LIGHTNING,
            60 * ResourceCosts.TICKS_PER_SECOND,
            20,
            0
        );
    }

    @Override
    public void use(Level level, Building buildingUsing, BlockPos targetedBp) {

        if (!level.isClientSide() && buildingUsing instanceof Laboratory lab) {
            BlockPos rodPos = lab.getLightningRodPos();
            if (lab.isAbilityOffCooldown(UnitAction.CALL_LIGHTNING) && rodPos != null) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(targetedBp.getX(), targetedBp.getY(), targetedBp.getZ());
                    level.addFreshEntity(bolt);
                }
                LightningBolt bolt2 = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt2 != null) {
                    bolt2.moveTo(rodPos.getX(), rodPos.getY(), rodPos.getZ());
                    level.addFreshEntity(bolt2);
                }
            }
        }
        this.setCooldown();
    }
}
