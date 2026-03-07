package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.controls.SlimeUnitMoveControl;
import com.solegendary.reignofnether.unit.goals.AbstractMeleeAttackUnitGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SlimeUnit;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;

public class MagmaCubeUnit extends SlimeUnit implements Unit, AttackerUnit {
    final static private int SET_FIRE_TICKS_MAX = 20;
    private int setFireTicks = 0;
    private static final int FIRE_DURATION_PER_SIZE = 40;
    private static final int MAGMA_DURATION = 100;

    @Override protected ParticleOptions getParticleType() {
        return ParticleTypes.FLAME;
    }
    @Override public boolean isOnFire() {
        return false;
    }
    @Override public Faction getFaction() {return Faction.PIGLINS;}

    public MagmaCubeUnit(EntityType<? extends SlimeUnit> entityType, Level level) {
        super(entityType, level);
        this.shouldSpawnSlimes = false;
    }

    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return this.isTiny() ? SoundEvents.MAGMA_CUBE_HURT_SMALL : SoundEvents.MAGMA_CUBE_HURT;
    }
    protected SoundEvent getDeathSound() {
        return this.isTiny() ? SoundEvents.MAGMA_CUBE_DEATH_SMALL : SoundEvents.MAGMA_CUBE_DEATH;
    }
    protected SoundEvent getSquishSound() {
        return this.isTiny() ? SoundEvents.MAGMA_CUBE_SQUISH_SMALL : SoundEvents.MAGMA_CUBE_SQUISH;
    }

    @Override
    public SunlightEffect getSunlightEffect() {
        return SunlightEffect.NONE;
    }

    @Override
    protected void spawnTinySlime() { }

    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            setFireTicks += 1;
            if (setFireTicks >= SET_FIRE_TICKS_MAX) {
                setFireTicks = 0;
                createMagma();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        this.addUnitSaveData(pCompound);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.readUnitSaveData(pCompound);
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
        super.checkFallDamage(pY, pOnGround, pState, pPos);
        if (!level().isClientSide() && pOnGround && !wasOnGround)
            createMagma();
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity pEntity) {
        boolean result = super.doHurtTarget(pEntity);
        if (result && getSize() >= 2) {
            pEntity.setRemainingFireTicks(FIRE_DURATION_PER_SIZE * getSize());
        }
        return result;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (getSize() >= 2 && pSource.getEntity() instanceof AttackerUnit aUnit && aUnit.getAttackGoal() instanceof AbstractMeleeAttackUnitGoal)
            pSource.getEntity().setRemainingFireTicks((FIRE_DURATION_PER_SIZE * getSize()) / 2);

        if (pSource.is(DamageTypeTags.IS_FIRE) ||
            pSource == damageSources().lava())
            return false;

        return super.hurt(pSource, pAmount);
    }


    public void createMagma() {
        if (getSize() < 4 || level().isClientSide())
            return;

        if (!ResearchServerEvents.playerHasResearch(getOwnerName(), ProductionItems.RESEARCH_CUBE_MAGMA))
            return;

        BlockState bsToPlace = BlockRegistrar.WALKABLE_MAGMA_BLOCK.get().defaultBlockState();
        BlockPos bpOn = getOnPos();
        if (level().getBlockState(bpOn).isAir())
            return;

        ArrayList<BlockPos> bps = new ArrayList<>();
        if (getSize() >= 2) {
            bps.add(bpOn);
        }
        if (getSize() >= 3) {
            bps.add(bpOn.north());
            bps.add(bpOn.east());
            bps.add(bpOn.south());
            bps.add(bpOn.west());
        }
        if (getSize() >= 4) {
            bps.add(bpOn.north().east());
            bps.add(bpOn.south().west());
            bps.add(bpOn.north().west());
            bps.add(bpOn.south().east());
        }
        if (getSize() >= 5) {
            bps.add(bpOn.north().north());
            bps.add(bpOn.south().south());
            bps.add(bpOn.east().east());
            bps.add(bpOn.west().west());
        }
        if (getSize() >= 6) {
            bps.add(bpOn.north().north().east());
            bps.add(bpOn.south().south().east());
            bps.add(bpOn.north().north().west());
            bps.add(bpOn.south().south().west());
            bps.add(bpOn.east().east().south());
            bps.add(bpOn.west().west().south());
            bps.add(bpOn.east().east().north());
            bps.add(bpOn.west().west().north());
        }

        for (BlockPos bp : bps) {
            BlockState bsOld = level().getBlockState(bp);
            if (MiscUtil.isSolidBlocking(level(), bp)) {
                BlockServerEvents.addTempBlock((ServerLevel) level(), bp,
                    BlockRegistrar.WALKABLE_MAGMA_BLOCK.get().defaultBlockState(), bsOld, MAGMA_DURATION);
            }
        }
    }
    public void createFire() {
        if (getSize() < MAX_SIZE || level().isClientSide())
            return;

        ArrayList<BlockPos> bps = new ArrayList<>();
        for (int x = -4; x < 4; x++)
            for (int y = -4; y < 4; y++)
                for (int z = -4; z < 4; z++)
                    if (level().getBlockState(getOnPos().offset(x,y,z)).getBlock() ==
                            BlockRegistrar.WALKABLE_MAGMA_BLOCK.get() &&
                            level().getBlockState(getOnPos().offset(x,y+1,z)).isAir())
                        bps.add(getOnPos().offset(x,y,z));
        Collections.shuffle(bps);
        if (bps.size() >= 1)
            level().setBlockAndUpdate(bps.get(0).above(), Blocks.FIRE.defaultBlockState());
    }
}
