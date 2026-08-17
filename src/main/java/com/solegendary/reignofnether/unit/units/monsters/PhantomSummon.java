package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ability.heroAbilities.necromancer.InsomniaCurse;
import com.solegendary.reignofnether.time.NightUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import static com.solegendary.reignofnether.ability.heroAbilities.necromancer.InsomniaCurse.PHANTOM_MAX_ATTACKS;

// still not a unit, but has overrides to make
public class PhantomSummon extends Phantom {

    final static public float maxHealth = 50.0f;
    public int attacksLeft = PHANTOM_MAX_ATTACKS; // at 0, dies after a few seconds
    public int ticksToDie = 60; // starts counting down after attacksLeft <= 0
    public int tickCountUntilDeath = 1200; // hard cap on lifespan
    public int attackCooldown = 0;
    public final int ATTACK_COOLDOWN_MAX = 120;
    public static final float HERO_DAMAGE_MULT = 0.7f;

    public LivingEntity entityTarget = null;

    public PhantomSummon(EntityType<? extends Phantom> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, InsomniaCurse.PHANTOM_DAMAGE)
                .add(Attributes.MAX_HEALTH, PhantomSummon.maxHealth);
    }

    @Override
    public void setPhantomSize(int pPhantomSize) {
        this.entityData.set(ID_SIZE, Mth.clamp(pPhantomSize, 0, 64));
    }

    @Override
    public void tick() {
        noPhysics = true;
        if (entityTarget != null && hasLineOfSight(entityTarget))
            anchorPoint = entityTarget.blockPosition();
        super.tick();

        if (getTarget() != entityTarget)
            setTarget(entityTarget);

        if (attacksLeft <= 0 || getTarget() == null || getTarget().isDeadOrDying() || getTarget().isRemoved())
            ticksToDie -= 1;
        if (ticksToDie <= 0 || tickCount > tickCountUntilDeath)
            kill();

        if (attackCooldown > 0)
            attackCooldown -= 1;
    }

    @Override
    public boolean hasLineOfSight(Entity pEntity) {
        if (attackCooldown > 0)
            return false;
        if (pEntity == entityTarget)
            return true;
        return super.hasLineOfSight(pEntity);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PhantomSummonAttackStrategyGoal());
        this.goalSelector.addGoal(2, new PhantomSummonSweepAttackGoal());
        this.goalSelector.addGoal(3, new PhantomSummonCircleAroundAnchorGoal());
        this.targetSelector.addGoal(1, new PhantomSummonAttackPlayerTargetGoal());
    }

    @Override
    protected boolean isSunBurnTick() {
        return NightUtils.isSunBurnTick(this);
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        if (super.doHurtTarget(pEntity)) {
            attackCooldown = ATTACK_COOLDOWN_MAX;
            attacksLeft -= 1;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return pSpawnData;
    }

    public class PhantomSummonAttackStrategyGoal extends Goal {
        private int nextSweepTick;

        PhantomSummonAttackStrategyGoal() {
        }

        public boolean canUse() {
            LivingEntity $$0 = PhantomSummon.this.getTarget();
            return $$0 != null ? PhantomSummon.this.canAttack($$0, TargetingConditions.DEFAULT) : false;
        }

        public void start() {
            this.nextSweepTick = this.adjustedTickDelay(10);
            PhantomSummon.this.attackPhase = PhantomSummon.AttackPhase.CIRCLE;
            this.setAnchorAboveTarget();
        }

        public void stop() {
            PhantomSummon.this.anchorPoint = PhantomSummon.this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, PhantomSummon.this.anchorPoint).above(10 + PhantomSummon.this.random.nextInt(20));
        }

        public void tick() {
            if (PhantomSummon.this.attackPhase == PhantomSummon.AttackPhase.CIRCLE) {
                --this.nextSweepTick;
                if (this.nextSweepTick <= 0) {
                    PhantomSummon.this.attackPhase = PhantomSummon.AttackPhase.SWOOP;
                    this.setAnchorAboveTarget();
                    this.nextSweepTick = this.adjustedTickDelay((8 + PhantomSummon.this.random.nextInt(4)) * 20);
                    PhantomSummon.this.playSound(SoundEvents.PHANTOM_SWOOP, 10.0F, 0.95F + PhantomSummon.this.random.nextFloat() * 0.1F);
                }
            }

        }

        private void setAnchorAboveTarget() {
            PhantomSummon.this.anchorPoint = PhantomSummon.this.getTarget().blockPosition().above(20 + PhantomSummon.this.random.nextInt(20));
            if (PhantomSummon.this.anchorPoint.getY() < PhantomSummon.this.level().getSeaLevel()) {
                PhantomSummon.this.anchorPoint = new BlockPos(PhantomSummon.this.anchorPoint.getX(), PhantomSummon.this.level().getSeaLevel() + 1, PhantomSummon.this.anchorPoint.getZ());
            }

        }
    }

    // prevent being scared of cats
    public class PhantomSummonSweepAttackGoal extends PhantomSummonMoveTargetGoal {
        PhantomSummonSweepAttackGoal() {
            super();
        }

        public boolean canUse() {
            return PhantomSummon.this.getTarget() != null && PhantomSummon.this.attackPhase == PhantomSummon.AttackPhase.SWOOP;
        }

        public boolean canContinueToUse() {
            LivingEntity $$0 = PhantomSummon.this.getTarget();
            if ($$0 == null) {
                return false;
            } else if (!$$0.isAlive()) {
                return false;
            } else {
                if ($$0 instanceof Player) {
                    Player $$1 = (Player)$$0;
                    if ($$0.isSpectator() || $$1.isCreative()) {
                        return false;
                    }
                }

                if (!this.canUse()) {
                    return false;
                } else {
                    return true;
                }
            }
        }

        public void start() {
        }

        public void stop() {
            PhantomSummon.this.setTarget((LivingEntity)null);
            PhantomSummon.this.attackPhase = PhantomSummon.AttackPhase.CIRCLE;
        }

        public void tick() {
            LivingEntity $$0 = PhantomSummon.this.getTarget();
            if ($$0 != null) {
                PhantomSummon.this.moveTargetPoint = new Vec3($$0.getX(), $$0.getY(0.5), $$0.getZ());
                if (PhantomSummon.this.getBoundingBox().inflate(0.20000000298023224).intersects($$0.getBoundingBox())) {
                    PhantomSummon.this.doHurtTarget($$0);
                    PhantomSummon.this.attackPhase = PhantomSummon.AttackPhase.CIRCLE;
                    if (!PhantomSummon.this.isSilent()) {
                        PhantomSummon.this.level().levelEvent(1039, PhantomSummon.this.blockPosition(), 0);
                    }
                } else if (PhantomSummon.this.horizontalCollision || PhantomSummon.this.hurtTime > 0) {
                    PhantomSummon.this.attackPhase = PhantomSummon.AttackPhase.CIRCLE;
                }

            }
        }
    }

    public class PhantomSummonCircleAroundAnchorGoal extends PhantomSummon.PhantomSummonMoveTargetGoal {
        private float angle;
        private float distance;
        private float height;
        private float clockwise;

        PhantomSummonCircleAroundAnchorGoal() {
            super();
        }

        public boolean canUse() {
            return PhantomSummon.this.getTarget() == null || PhantomSummon.this.attackPhase == PhantomSummon.AttackPhase.CIRCLE;
        }

        public void start() {
            this.distance = 5.0F + PhantomSummon.this.random.nextFloat() * 10.0F;
            this.height = -4.0F + PhantomSummon.this.random.nextFloat() * 9.0F;
            this.clockwise = PhantomSummon.this.random.nextBoolean() ? 1.0F : -1.0F;
            this.selectNext();
        }

        public void tick() {
            if (PhantomSummon.this.random.nextInt(this.adjustedTickDelay(350)) == 0) {
                this.height = -4.0F + PhantomSummon.this.random.nextFloat() * 9.0F;
            }

            if (PhantomSummon.this.random.nextInt(this.adjustedTickDelay(250)) == 0) {
                ++this.distance;
                if (this.distance > 15.0F) {
                    this.distance = 5.0F;
                    this.clockwise = -this.clockwise;
                }
            }

            if (PhantomSummon.this.random.nextInt(this.adjustedTickDelay(450)) == 0) {
                this.angle = PhantomSummon.this.random.nextFloat() * 2.0F * 3.1415927F;
                this.selectNext();
            }

            if (this.touchingTarget()) {
                this.selectNext();
            }

            if (PhantomSummon.this.moveTargetPoint.y < PhantomSummon.this.getY() && !PhantomSummon.this.level().isEmptyBlock(PhantomSummon.this.blockPosition().below(1))) {
                this.height = Math.max(1.0F, this.height);
                this.selectNext();
            }

            if (PhantomSummon.this.moveTargetPoint.y > PhantomSummon.this.getY() && !PhantomSummon.this.level().isEmptyBlock(PhantomSummon.this.blockPosition().above(1))) {
                this.height = Math.min(-1.0F, this.height);
                this.selectNext();
            }

        }

        private void selectNext() {
            if (BlockPos.ZERO.equals(PhantomSummon.this.anchorPoint)) {
                PhantomSummon.this.anchorPoint = PhantomSummon.this.blockPosition();
            }

            this.angle += this.clockwise * 15.0F * 0.017453292F;
            PhantomSummon.this.moveTargetPoint = Vec3.atLowerCornerOf(PhantomSummon.this.anchorPoint).add((double)(this.distance * Mth.cos(this.angle)), (double)(-4.0F + this.height), (double)(this.distance * Mth.sin(this.angle)));
        }
    }

    public class PhantomSummonAttackPlayerTargetGoal extends Goal {
        private final TargetingConditions attackTargeting = TargetingConditions.forCombat().range(64.0);
        private int nextScanTick = reducedTickDelay(20);

        PhantomSummonAttackPlayerTargetGoal() {
        }

        public boolean canUse() {
            if (this.nextScanTick > 0) {
                --this.nextScanTick;
                return false;
            } else {
                this.nextScanTick = reducedTickDelay(60);
                List<Player> $$0 = PhantomSummon.this.level().getNearbyPlayers(this.attackTargeting, PhantomSummon.this, PhantomSummon.this.getBoundingBox().inflate(16.0, 64.0, 16.0));
                if (!$$0.isEmpty()) {
                    $$0.sort(Comparator.comparing(player -> ((Entity) player).getY()).reversed());
                    Iterator var2 = $$0.iterator();

                    while(var2.hasNext()) {
                        Player $$1 = (Player)var2.next();
                        if (PhantomSummon.this.canAttack($$1, TargetingConditions.DEFAULT)) {
                            PhantomSummon.this.setTarget($$1);
                            return true;
                        }
                    }
                }

                return false;
            }
        }

        public boolean canContinueToUse() {
            LivingEntity $$0 = PhantomSummon.this.getTarget();
            return $$0 != null ? PhantomSummon.this.canAttack($$0, TargetingConditions.DEFAULT) : false;
        }
    }

    abstract class PhantomSummonMoveTargetGoal extends Goal {
        public PhantomSummonMoveTargetGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        protected boolean touchingTarget() {
            return PhantomSummon.this.moveTargetPoint.distanceToSqr(PhantomSummon.this.getX(), PhantomSummon.this.getY(), PhantomSummon.this.getZ()) < 4.0;
        }
    }
}
