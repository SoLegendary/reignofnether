package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.abilities.ThrowHealingPotion;
import com.solegendary.reignofnether.ability.abilities.ThrowLingeringHarmingPotion;
import com.solegendary.reignofnether.ability.abilities.ThrowLingeringRegenPotion;
import com.solegendary.reignofnether.ability.abilities.ThrowWaterPotion;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.WitchUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ThrowPotionGoal extends MoveToTargetBlockGoal {

    private Potion potion = null;
    private LivingEntity targetEntity = null;

    public ThrowPotionGoal(Mob mob) {
        super(mob, false, 0);
    }

    public void setPotion(Potion potion) {
        this.potion = potion;
    }

    // set the target to throw a potion - the witch will move towards this location until we're within range
    // then throw a potion at it
    // if we set an entity target, on every tick we will follow that target
    // if we set a BlockPos as the target, remove any entity target
    public void setTarget(LivingEntity entity) {
        this.targetEntity = entity;
    }
    public void setTarget(BlockPos bpTarget) {
        this.setMoveTarget(bpTarget);
        this.setTarget((LivingEntity) null);
    }

    @Override
    public void tick() {
        if (this.targetEntity != null)
            this.setMoveTarget(this.targetEntity.getOnPos());

        WitchUnit witch = (WitchUnit) this.mob;
        if (moveTarget != null) {
            if (MyMath.distance(
                this.mob.getX(), this.mob.getZ(),
                moveTarget.getX(), moveTarget.getZ()) <= witch.getPotionThrowRange()) {

                this.mob.getLookControl().setLookAt(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ());
                if (moveTarget != null) {
                    witch.throwPotion(new Vec3(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ()), this.potion);

                    if (!this.mob.level().isClientSide())
                        for (Ability potionAbility : witch.getAbilities().get())
                            AbilityClientboundPacket.sendSetCooldownPacket(this.mob.getId(), potionAbility.action, potionAbility.cooldownMax);
                }
                this.stop();
            }
        }

        // autocast
        if (this.targetEntity == null && moveTarget == null && !mob.level().isClientSide() && witch.isIdle() && witch.tickCount % 4 == 0) {

            for (Ability potionAbility : witch.getAbilities().get()) {
                if (!potionAbility.isAutocasting(witch) || !potionAbility.isOffCooldown(witch))
                    continue;

                List<Mob> nearbyMobs = MiscUtil.getEntitiesWithinRange(
                        new Vector3d(witch.position().x, witch.position().y, witch.position().z),
                        potionAbility.range + 4,
                        Mob.class,
                        witch.level());

                if (potionAbility instanceof ThrowLingeringRegenPotion ||
                    potionAbility instanceof ThrowHealingPotion) {

                    List<Mob> nearbyFriendlyHurtUnits = new ArrayList<>();
                    for (Mob nearbyMob : nearbyMobs) {
                        if (nearbyMob instanceof Unit unit &&
                            nearbyMob.getMobType() != MobType.UNDEAD &&
                            nearbyMob.getHealth() < nearbyMob.getMaxHealth() && (
                                    unit.getOwnerName().equals(witch.getOwnerName()) ||
                                    AlliancesServerEvents.isAllied(unit.getOwnerName(), witch.getOwnerName())
                            )) {
                            nearbyFriendlyHurtUnits.add(nearbyMob);
                        }
                    }
                    nearbyFriendlyHurtUnits.sort(Comparator.comparing(mb -> mb.getHealth() / mb.getMaxHealth()));

                    if (!nearbyFriendlyHurtUnits.isEmpty()) {
                        if (potionAbility instanceof ThrowLingeringRegenPotion pa)
                            setPotion(pa.potion);
                        else if (potionAbility instanceof ThrowHealingPotion pa)
                            setPotion(pa.potion);
                        setTarget(nearbyFriendlyHurtUnits.get(0));
                        break;
                    }
                }
                else if (potionAbility instanceof ThrowWaterPotion throwWaterPotion) {
                    List<Mob> nearbyOnFireUnits = new ArrayList<>();
                    for (Mob nearbyMob : nearbyMobs) {
                        if (nearbyMob instanceof Unit unit &&
                            nearbyMob.isOnFire() && (
                                    unit.getOwnerName().equals(witch.getOwnerName()) ||
                                    AlliancesServerEvents.isAllied(unit.getOwnerName(), witch.getOwnerName())
                            )) {
                            nearbyOnFireUnits.add(nearbyMob);
                        }
                    }
                    nearbyOnFireUnits.sort(Comparator.comparing(mb -> mb.getHealth() / mb.getMaxHealth()));

                    if (!nearbyOnFireUnits.isEmpty()) {
                        setPotion(throwWaterPotion.potion);
                        setTarget(nearbyOnFireUnits.get(0));
                        break;
                    } else if (witch.tickCount % 20 == 0) {
                        BlockPos fireBp = findNearbyFireBlock();
                        if (fireBp != null) {
                            setPotion(throwWaterPotion.potion);
                            setTarget(fireBp);
                            break;
                        }
                    }
                }
                else if (potionAbility instanceof ThrowLingeringHarmingPotion throwHarmingPotion) {
                    List<Mob> nearbyEnemyUnits = new ArrayList<>();
                    for (Mob mb : nearbyMobs) {
                        if (mb instanceof Unit unit &&
                            mb.getMobType() != MobType.UNDEAD && (
                                    !unit.getOwnerName().equals(witch.getOwnerName()) &&
                                    !AlliancesServerEvents.isAllied(unit.getOwnerName(), witch.getOwnerName())
                            )) {
                            nearbyEnemyUnits.add(mb);
                        }
                    }

                    if (!nearbyEnemyUnits.isEmpty()) {
                        setPotion(throwHarmingPotion.potion);
                        setTarget(nearbyEnemyUnits.get(0));
                        break;
                    }
                }
            }
        }
    }

    @Nullable
    private BlockPos findNearbyFireBlock() {
        ArrayList<BlockPos> bps = new ArrayList<>();
        for (int x = -10; x < 10; x++)
            for (int y = -4; y < 4; y++)
                for (int z = -10; z < 10; z++) {
                    BlockPos bp = mob.blockPosition().offset(x, y, z);
                    if (mob.level().getBlockState(bp).getBlock() == Blocks.FIRE)
                        bps.add(bp);
                }
        bps.sort(Comparator.comparing(bp -> bp.distSqr(this.mob.blockPosition())));
        if (!bps.isEmpty())
            return bps.get(0);
        else
            return null;
    }

    @Override
    public void stop() {
        this.stopMoving();
        this.setTarget((LivingEntity) null);
        this.setPotion(null);
    }
}