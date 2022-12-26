package com.solegendary.reignofnether.unit.abilities;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.buildings.monsters.Laboratory;
import com.solegendary.reignofnether.unit.Ability;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CallLightningAbility extends Ability {

    public CallLightningAbility() {
        super(
            UnitAction.CALL_LIGHTNING,
            60 * ResourceCosts.TICKS_PER_SECOND,
            25,
            0
        );
    }

    @Override
    public void use(Level level, Building buildingUsing, BlockPos targetBp) {

        if (!level.isClientSide() && buildingUsing instanceof Laboratory lab) {
            BlockPos rodPos = lab.getLightningRodPos();

            if (lab.isAbilityOffCooldown(UnitAction.CALL_LIGHTNING) && rodPos != null) {
                BlockPos limitedBp = getXZRangeLimitedBlockPos(rodPos, targetBp);
                // getXZRangeLimitedBlockPos' Y value is always the same as rodPos, but we want the first sky-exposed block
                limitedBp = MiscUtil.getHighestSolidBlock(level, limitedBp);

                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(limitedBp.getX(), limitedBp.getY(), limitedBp.getZ());
                    level.addFreshEntity(bolt);
                }
                LightningBolt bolt2 = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt2 != null) {
                    bolt2.moveTo(rodPos.getX(), rodPos.getY(), rodPos.getZ());
                    level.addFreshEntity(bolt2);
                }
            }
        }
        this.setToMaxCooldown();
    }
}
