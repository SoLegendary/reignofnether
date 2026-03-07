package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.ability.abilities.FirewallShot;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.BlazeUnit;
import com.solegendary.reignofnether.unit.units.piglins.WildfireUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class BlazeUnitFireball extends SmallFireball {

    boolean isFirewallShot;
    private static final int MAX_TICKS = 60;
    private static final int MAX_TICKS_FIREWALL = (int) (FirewallShot.RANGE * 1.5f);

    public static final int FIRE_SECONDS = 5;

    public BlazeUnitFireball(Level pLevel, LivingEntity pShooter, double pOffsetX, double pOffsetY, double pOffsetZ, boolean isFirewallShot) {
        super(pLevel, pShooter, pOffsetX, pOffsetY, pOffsetZ);
        this.isFirewallShot = isFirewallShot;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && isFirewallShot) {
            BlockState fireState = Blocks.FIRE.defaultBlockState();
            if (getOwner() instanceof Blaze blaze && blaze.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get())) {
                fireState = BlockRegistrar.UNEXTINGUISHABLE_SOUL_FIRE.get().defaultBlockState();
            }
            Block block = this.level().getBlockState(this.getOnPos()).getBlock();
            Block blockBelow = this.level().getBlockState(this.getOnPos().below()).getBlock();
            Block blockBelow2 = this.level().getBlockState(this.getOnPos().below().below()).getBlock();

            List<Block> nonSolidBlocks = List.of(
                    Blocks.AIR, Blocks.TALL_GRASS, Blocks.GRASS,
                    Blocks.CRIMSON_ROOTS, Blocks.WARPED_ROOTS,
                    Blocks.DEAD_BUSH, Blocks.SNOW,
                    BlockRegistrar.WRAITH_SNOW_LAYER.get()
            );
            if (nonSolidBlocks.contains(blockBelow2)) {
                meltSnow(this.getOnPos().below().below().below());
                replacePathWithDirt(this.getOnPos().below().below().below());
                this.level().setBlockAndUpdate(this.getOnPos().below().below(), fireState);
            }
            if (nonSolidBlocks.contains(blockBelow)) {
                meltSnow(this.getOnPos().below().below());
                replacePathWithDirt(this.getOnPos().below().below());
                this.level().setBlockAndUpdate(this.getOnPos().below(), fireState);
            }
            else if (nonSolidBlocks.contains(block)) {
                meltSnow(this.getOnPos());
                replacePathWithDirt(this.getOnPos());
                this.level().setBlockAndUpdate(this.getOnPos(), fireState);
            }
        }
        if (tickCount > MAX_TICKS || (tickCount > MAX_TICKS_FIREWALL && isFirewallShot))
            this.discard();
    }

    private void meltSnow(BlockPos bp) {
        if (this.level().getBlockState(bp).getBlock() == BlockRegistrar.WRAITH_SNOW_LAYER.get() ||
            this.level().getBlockState(bp).getBlock() == Blocks.SNOW)
            this.level().setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
    }
    private void replacePathWithDirt(BlockPos bp) {
        if (this.level().getBlockState(bp).getBlock() == Blocks.DIRT_PATH)
            this.level().setBlockAndUpdate(bp, Blocks.DIRT.defaultBlockState());
    }

    // let fireballs pierce garrison blocks and entities that are on fire
    @Override
    protected void onHit(HitResult pResult) {

        // Projectile class code
        HitResult.Type hitresult$type = pResult.getType();
        if (hitresult$type == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult)pResult;
            boolean targetOnFire = entityHitResult.getEntity().isOnFire();

            this.onHitEntity(entityHitResult);
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, pResult.getLocation(), GameEvent.Context.of(this, null));
            if (this.getOwner() instanceof LivingEntity le && le.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get()) &&
                entityHitResult.getEntity() instanceof LivingEntity leTarget) {
                leTarget.addEffect(new MobEffectInstance(MobEffectRegistrar.SOULS_AFLAME.get(), 120, 0, false, false));
            }
            if (!this.level().isClientSide && !targetOnFire && !this.isFirewallShot)
                this.discard();
            if (this.getOwner() instanceof WildfireUnit wildfireUnit)
                entityHitResult.getEntity().hurt(damageSources().mobProjectile(this, wildfireUnit), wildfireUnit.getUnitAttackDamage());

        } else if (hitresult$type == HitResult.Type.BLOCK && !isNoPhysics()) {
            BlockHitResult blockhitresult = (BlockHitResult)pResult;
            BlockPos blockpos = blockhitresult.getBlockPos();
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, blockpos, GameEvent.Context.of(this, this.level().getBlockState(blockpos)));

            if (!this.level().isClientSide)
                this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        if (!this.level().isClientSide) {
            Entity entity = pResult.getEntity();
            int i = entity.getRemainingFireTicks();
            if (i > 0) // if we just set to 100 every time, we reset the damage delay back to 20 ticks
                entity.setRemainingFireTicks((i % 20) + (FIRE_SECONDS * 20) - 20);
            else
                entity.setRemainingFireTicks(FIRE_SECONDS * 20);
        }
    }

    public boolean isNoPhysics() {
        if (this.getOwner() instanceof Unit unit) {
            BuildingPlacement building = GarrisonableBuildingAddon.getGarrison(unit);

            if (building != null ) {
                GarrisonableBuildingAddon garr = building.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class);

                // only have nophysics at a high Y value so we can still attack enemies at the base of the building
                if (garr != null && building.isPosInsideBuilding(this.blockPosition()) &&
                        this.blockPosition().getY() > building.originPos.getY() + 5)
                    return true;
            }
        }
        return false;
    }
}
