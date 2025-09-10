package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.goals.BuildRepairGoal;
import com.solegendary.reignofnether.unit.goals.GatherResourcesGoal;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import com.solegendary.reignofnether.unit.units.monsters.ZombieVillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import static com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession.*;

public interface WorkerUnit {

    public BuildRepairGoal getBuildRepairGoal();
    public GatherResourcesGoal getGatherResourceGoal();
    public BlockState getReplantBlockState();

    public static void tick(WorkerUnit unit) {
        BuildRepairGoal buildRepairGoal = unit.getBuildRepairGoal();
        if (buildRepairGoal != null)
            buildRepairGoal.tick();
        GatherResourcesGoal gatherResourcesGoal = unit.getGatherResourceGoal();
        if (gatherResourcesGoal != null)
            gatherResourcesGoal.tick();

        LivingEntity entity = (LivingEntity) unit;
        ItemStack mainHandItem = entity.getItemBySlot(EquipmentSlot.MAINHAND);

        if (unit.getBuildRepairGoal().isBuilding()) {
            if (!mainHandItem.is(Items.IRON_SHOVEL)) {
                if (entity instanceof VillagerUnit vUnit && vUnit.isVeteran() && vUnit.getUnitProfession() == MASON)
                    entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SHOVEL));
                else
                    entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
            }
        }
        else if (unit.getGatherResourceGoal().isGathering()) {
            switch (unit.getGatherResourceGoal().getTargetResourceName()) {
                case FOOD -> {
                    if (!mainHandItem.is(Items.IRON_HOE)) {
                        if (entity instanceof VillagerUnit vUnit && vUnit.isVeteran() && vUnit.getUnitProfession() == FARMER &&
                                vUnit.getGatherResourceGoal().isFarming())
                            entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_HOE));
                        else
                            entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_HOE));
                    }
                }
                case WOOD -> {
                    if (!mainHandItem.is(Items.IRON_AXE)) {
                        if (entity instanceof VillagerUnit vUnit && vUnit.isVeteran() && vUnit.getUnitProfession() == LUMBERJACK)
                            entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
                        else
                            entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                    }
                }
                case ORE -> {
                    if (!mainHandItem.is(Items.IRON_PICKAXE)) {
                        if (entity instanceof VillagerUnit vUnit && vUnit.isVeteran() && vUnit.getUnitProfession() == MINER)
                            entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
                        else
                            entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
                    }
                }
                case NONE -> {
                    if (!mainHandItem.is(Items.AIR))
                        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.AIR));
                }
            }
        } else if (entity instanceof AttackerUnit attackerUnit &&
                ((Unit) entity).getTargetGoal().getTarget() != null &&
                !(entity instanceof ZombieVillagerUnit)) {
            if (!mainHandItem.is(Items.WOODEN_SWORD) && !mainHandItem.is(Items.STONE_SWORD)) {
                if (entity instanceof VillagerUnit vUnit && vUnit.getUnitProfession() == VillagerUnitProfession.HUNTER && vUnit.isVeteran())
                    entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
                else
                    entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
                if (!entity.level().isClientSide())
                    UnitAnimationClientboundPacket.sendEntityPacket(UnitAnimationAction.NON_KEYFRAME_START, entity, ((Unit) entity).getTargetGoal().getTarget());
            }
        } else {
            if (!mainHandItem.is(Items.AIR)) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.AIR));
                if (!entity.level().isClientSide())
                    UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.NON_KEYFRAME_STOP, entity);
            }
        }
    }

    public static void resetBehaviours(WorkerUnit unit) {
        unit.getBuildRepairGoal().stopBuilding();
        unit.getGatherResourceGoal().stopGathering();
    }

    // only properly works serverside - clientside requires packet updates
    public static boolean isIdle(WorkerUnit unit) {
        GatherResourcesGoal resGoal = unit.getGatherResourceGoal();

        boolean isMoving = !((Mob) unit).getNavigation().isDone();
        boolean isGathering = resGoal.isGathering();
        boolean isGatheringIdle = resGoal.isIdle();
        boolean isBuilding = unit.getBuildRepairGoal().getBuildingTarget() != null;
        boolean isFarming = resGoal.isFarming();
        boolean isAttacking = ((Unit) unit).getTargetGoal().getTarget() != null;

        return !isMoving && !isGathering && !isBuilding && isGatheringIdle && !isAttacking && !isFarming;
    }
}
