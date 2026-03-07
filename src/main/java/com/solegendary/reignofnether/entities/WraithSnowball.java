package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.ability.heroAbilities.wretchedwraith.Blizzard;
import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.BlockUtils;
import com.solegendary.reignofnether.unit.units.monsters.WretchedWraithUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class WraithSnowball extends Snowball {
    public WraithSnowball(EntityType<? extends Snowball> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        pResult.getEntity().hurt(this.damageSources().generic(), Blizzard.SNOWBALL_DAMAGE);
        placeSnow(pResult.getEntity().getOnPos().above());
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        placeSnow(pResult.getBlockPos().above());
        discard();
    }

    private void placeSnow(BlockPos pos) {
        if (!level().isClientSide())
            for (BlockPos snowPos : BlockServerEvents.getSnowPositions(level(), pos, 1).keySet())
                BlockServerEvents.placeWraithSnow((ServerLevel) level(), snowPos, this.getOwner() != null ? this.getOwner().getId() : -1);
    }
}
