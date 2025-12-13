package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.RangeIndicator;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import org.joml.Vector3d;

import java.util.*;

public class HealingFountainPlacement extends BuildingPlacement implements RangeIndicator {
    private final ArrayList<BuildingBlock> waterBlocks;
    public HealingFountainPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
        List<BuildingBlock> wbs = new ArrayList<>();
        for (BuildingBlock b : blocks) {
            if (b.getBlockPos().getY() < centrePos.getY() &&
                b.getBlockState().getBlock() == Blocks.WATER) {
                wbs.add(b);
            }
        }
        this.waterBlocks = new ArrayList<>(wbs);
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
    }

    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        List<LivingEntity> nearbyEntities = MiscUtil.getEntitiesWithinRange(
                new Vector3d(this.centrePos.getX(), this.centrePos.getY(), this.centrePos.getZ()),
                RANGE,
                LivingEntity.class,
                this.level);

        for (LivingEntity le : nearbyEntities) {
            if (isBuilt && tickAgeAfterBuilt % 20 == 0)  {
                // this actually isn't enough to cause a healing tick, but is just for effects
                le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, 0));
                le.heal(Math.min(1, le.getMaxHealth() / 100));
            }
        }

        // spawn random healing particle
        if (!waterBlocks.isEmpty() && isBuilt) {
            Collections.shuffle(waterBlocks);
            int col = 16262179; // red healing effect
            BlockPos bp = waterBlocks.get(0).getBlockPos();
            double d0 = (double)(col >> 16 & 255) / 255.0;
            double d1 = (double)(col >> 8 & 255) / 255.0;
            double d2 = (double)(col >> 0 & 255) / 255.0;
            this.level.addParticle(ParticleTypes.ENTITY_EFFECT, bp.getX(), bp.getY() + 1, bp.getZ(), d0, d1, d2);
        }
    }

    public static final int RANGE = 20;
    private final Set<BlockPos> borderBps = new HashSet<>();

    @Override
    public void updateBorderBps() {
        if (!level.isClientSide())
            return;
        this.borderBps.clear();
        this.borderBps.addAll(MiscUtil.getRangeIndicatorCircleBlocks(centrePos,
                RANGE - TimeClientEvents.VISIBLE_BORDER_ADJ, level));
    }

    @Override
    public Set<BlockPos> getBorderBps() {
        return borderBps;
    }

    @Override
    public boolean showOnlyWhenSelected() {
        return true;
    }
}
