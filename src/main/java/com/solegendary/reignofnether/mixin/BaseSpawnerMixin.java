package com.solegendary.reignofnether.mixin;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(BaseSpawner.class)
public class BaseSpawnerMixin {

    @Shadow private SpawnData nextSpawnData;
    @Shadow private int spawnDelay;
    @Shadow private void delay(Level pLevel, BlockPos pPos) { }
    @Shadow private SpawnData getOrCreateNextSpawnData(@Nullable Level pLevel, RandomSource pRandom, BlockPos pPos) { return null; }
    @Shadow private boolean isNearPlayer(Level pLevel, BlockPos pPos) { return true; }

    private static final int SPAWN_RANGE = 6;
    private static final int SPAWN_COUNT = 3;
    private static final int MAX_NEARBY_NEUTRAL_UNITS = 1;
    private static final int SPAWN_DELAY = 600;
    private static final int ACTIVATION_RANGE = Unit.ANCHOR_RETREAT_RANGE;

    @Inject(
            method = "delay",
            at = @At("HEAD")
    )
    private void delayNoRandom(Level pLevel, BlockPos pPos, CallbackInfo ci) {
        // avoid using the 10-40 randomised delay
        spawnDelay = SPAWN_DELAY;
    }

    // show spinning entity and flames
    @Inject(
            method = "isNearPlayer",
            at = @At("HEAD"),
            cancellable = true
    )
    public void isNearPlayer(Level pLevel, BlockPos pPos, CallbackInfoReturnable<Boolean> cir) {
        if (reignofnether$isValidNeutralUnitSpawner(pLevel, pPos))
            cir.setReturnValue(true);
    }

    @Unique
    private boolean reignofnether$isValidNeutralUnitSpawner(Level level, BlockPos pPos) {
        return !BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), pPos) &&
                nextSpawnData != null &&
                nextSpawnData.getEntityToSpawn().getAsString().contains("reignofnether");
    }

    @Unique
    private int reignofnether$getMaxNearbyNeutralUnits(Entity entity, int nearbySameTypeSpawners) {
        if (entity instanceof Unit unit && unit.getCost().population >= 5)
            return MAX_NEARBY_NEUTRAL_UNITS + nearbySameTypeSpawners;
        else
            return MAX_NEARBY_NEUTRAL_UNITS + (nearbySameTypeSpawners * SPAWN_COUNT);
    }

    @Inject(
            method = "serverTick",
            at = @At("HEAD"),
            cancellable = true
    )
    public void serverTick(ServerLevel pServerLevel, BlockPos pPos, CallbackInfo ci) {
        if (reignofnether$isValidNeutralUnitSpawner(pServerLevel, pPos)) {
            ci.cancel();

            if (this.spawnDelay == -1) {
                this.delay(pServerLevel, pPos);
            }

            if (this.spawnDelay > 0) {
                --this.spawnDelay;
            } else {
                ArrayList<Pair<Entity, Vector3d>> spawnedEntities = new ArrayList<>();
                RandomSource randomsource = pServerLevel.getRandom();
                SpawnData spawndata = this.getOrCreateNextSpawnData(pServerLevel, randomsource, pPos);
                CompoundTag compoundtag = spawndata.getEntityToSpawn();

                int nearbySameTypeSpawners = 0;
                for (int x = -3; x <= 3; x++) {
                    for (int y = -3; y <= 3; y++) {
                        for (int z = -3; z <= 3; z++) {
                            if (x == 0 && y == 0 && z == 0)
                                continue;
                            if (pServerLevel.getBlockEntity(pPos.offset(x,y,z)) instanceof SpawnerBlockEntity sbe &&
                                    sbe.getSpawner().nextSpawnData != null &&
                                    sbe.getSpawner().nextSpawnData.getEntityToSpawn().getAsString()
                                            .equals(nextSpawnData.getEntityToSpawn().getAsString())) {
                                nearbySameTypeSpawners += 1;
                            }
                        }
                    }
                }

                for (int i = 0; i < SPAWN_COUNT; i++) {

                    Optional<EntityType<?>> optional = EntityType.by(compoundtag);
                    if (optional.isEmpty()) {
                        this.delay(pServerLevel, pPos);
                        return;
                    }

                    ListTag listtag = compoundtag.getList("Pos", 6);
                    int j = listtag.size();
                    double d0;
                    double d1;
                    double d2;
                    int collisionRetries = 0;
                    do {
                        d0 = j >= 1 ? listtag.getDouble(0) : (double) pPos.getX() + (randomsource.nextDouble() - randomsource.nextDouble()) * (double) SPAWN_RANGE + 0.5;
                        d1 = j >= 2 ? listtag.getDouble(1) : (double) (pPos.getY() + randomsource.nextInt(3) - 1);
                        d2 = j >= 3 ? listtag.getDouble(2) : (double) pPos.getZ() + (randomsource.nextDouble() - randomsource.nextDouble()) * (double) SPAWN_RANGE + 0.5;
                        collisionRetries += 1;
                    } while (!pServerLevel.noCollision(optional.get().getAABB(d0, d1, d2)) && collisionRetries < 5);

                    if (pServerLevel.noCollision(optional.get().getAABB(d0, d1, d2))) {
                        label105:
                        {
                            BlockPos blockpos = BlockPos.containing(d0, d1, d2);
                            if (spawndata.getCustomSpawnRules().isPresent()) {
                                if (!(optional.get()).getCategory().isFriendly() && pServerLevel.getDifficulty() == Difficulty.PEACEFUL) {
                                    break label105;
                                }
                            } else if (!SpawnPlacements.checkSpawnRules((EntityType) optional.get(), pServerLevel, MobSpawnType.SPAWNER, blockpos, pServerLevel.getRandom())) {
                                break label105;
                            }

                            Entity entity = EntityType.loadEntityRecursive(compoundtag, pServerLevel, (e) -> {
                                e.moveTo(e.getX(), e.getY(), e.getZ(), e.getYRot(), e.getXRot());
                                return e;
                            });
                            if (entity == null) {
                                this.delay(pServerLevel, pPos);
                                return;
                            }

                            var nearbyNeutralUnitsOfTypeSum = 0;
                            var hasNearbyNonNeutralUnit = false;

                            for (LivingEntity le : UnitServerEvents.getAllUnits()) {
                                if (!(le instanceof Unit unit) || le.isDeadOrDying()) continue;
                                double range = (double) ACTIVATION_RANGE / 2;
                                boolean anchoredInRange = unit.getAnchor() != null &&
                                        unit.getAnchor().distToCenterSqr(pPos.getCenter()) < range * range;
                                if (anchoredInRange && unit.getOwnerName().isBlank() && le.getType() == entity.getType()) {
                                    nearbyNeutralUnitsOfTypeSum++;
                                } else if (!unit.getOwnerName().isBlank()) {
                                    hasNearbyNonNeutralUnit = true;
                                }
                            }

                            if (nearbyNeutralUnitsOfTypeSum >= reignofnether$getMaxNearbyNeutralUnits(entity, nearbySameTypeSpawners) ||
                                    hasNearbyNonNeutralUnit) {
                                this.delay(pServerLevel, pPos);
                                return;
                            }
                            spawnedEntities.add(new Pair<>(entity, new Vector3d(d0, d1, d2)));
                        }
                    }
                }
                for (Pair<Entity, Vector3d> pair : spawnedEntities) {

                    Entity entity = pair.getFirst();
                    BlockPos blockpos = BlockPos.containing(pair.getSecond().x, pair.getSecond().y, pair.getSecond().z);

                    entity.moveTo(pair.getSecond().x, pair.getSecond().y, pair.getSecond().z, randomsource.nextFloat() * 360.0F, 0.0F);
                    if (entity instanceof Mob mob) {
                        // if the mob is classed as a monster, this will check for light levels
                        if (entity instanceof Unit unit && unit.getSunlightEffect() == Unit.SunlightEffect.FIRE) {
                            if (!ForgeEventFactory.checkSpawnPositionSpawner(mob, pServerLevel, MobSpawnType.SPAWNER, spawndata, (BaseSpawner)(Object)this)) {
                                continue;
                            }
                        }
                        MobSpawnEvent.FinalizeSpawn event = ForgeEventFactory.onFinalizeSpawnSpawner(mob, pServerLevel, pServerLevel.getCurrentDifficultyAt(entity.blockPosition()), (SpawnGroupData) null, compoundtag, (BaseSpawner)(Object)this);
                        if (event != null && !event.isSpawnCancelled()) {
                            ((Mob) entity).finalizeSpawn(pServerLevel, event.getDifficulty(), event.getSpawnType(), event.getSpawnData(), event.getSpawnTag());
                            if (entity instanceof Unit unit)
                                unit.setAnchor(entity.getOnPos());
                        }
                    }
                    if (!pServerLevel.tryAddFreshEntityWithPassengers(entity)) {
                        this.delay(pServerLevel, pPos);
                        return;
                    }
                    pServerLevel.levelEvent(2004, pPos, 0);
                    pServerLevel.gameEvent(entity, GameEvent.ENTITY_PLACE, blockpos);

                    if (entity instanceof Unit unit && unit.getCost().population >= 5)
                        break;
                }
                this.delay(pServerLevel, pPos);
            }
        }
    }
}
