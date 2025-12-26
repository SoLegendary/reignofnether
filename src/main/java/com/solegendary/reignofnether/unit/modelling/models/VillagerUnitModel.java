package com.solegendary.reignofnether.unit.modelling.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.goals.SelectedTargetGoal;
import com.solegendary.reignofnether.unit.interfaces.ArmSwingingUnit;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.unit.units.villagers.MilitiaUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.item.ArmorItem;

import java.util.List;

public class VillagerUnitModel<T extends AbstractIllager> extends HumanoidModel<T> {

    public ModelPart jacket = this.body.getChild("jacket");
    public ModelPart crossedArms;

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "villager_unit_layer"), "main");

    public enum ArmPose {
        CROSSED,
        ATTACKING,
        CROSSBOW_HOLD,
        CROSSBOW_CHARGE,
        BUILDING,
        GATHERING,
        SPELLCASTING,
        BOW_AND_ARROW
    }

    public VillagerUnitModel(ModelPart part) {
        super(part);
        this.crossedArms = part.getChild("arms");
        this.hat.visible = false;
        this.getHatRim().visible = false;
    }

    public ModelPart getHatRim() {
        return this.head.getChild("hat").getChild("hat_rim");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F,
                8.0F, 12.0F, 8.0F, new CubeDeformation(0.45F)), PartPose.ZERO);
        hat.addOrReplaceChild("hat_rim", CubeListBuilder.create().texOffs(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F), PartPose.rotation(-1.5707964F, 0.0F, 0.0F));
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition body = partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("jacket", CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 18.0F,
                6.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition partdefinition2 = partdefinition.addOrReplaceChild("arms",
                CubeListBuilder.create().texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F).texOffs(40, 38)
                        .addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F));
        partdefinition2.addOrReplaceChild("left_shoulder",
                CubeListBuilder.create().texOffs(44, 22).mirror().addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F),
                PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 46).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 46).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return Iterables.concat(super.bodyParts(), ImmutableList.of(this.crossedArms, this.jacket));
    }

    private VillagerUnitModel.ArmPose getArmPose(Entity entity) {
        if (entity instanceof WorkerUnit workerUnit && workerUnit.getGatherResourceGoal() != null && workerUnit.getGatherResourceGoal().isGathering()) {
            return VillagerUnitModel.ArmPose.GATHERING;
        }
        if (entity instanceof WorkerUnit workerUnit && workerUnit.getBuildRepairGoal() != null && workerUnit.getBuildRepairGoal().isBuilding()) {
            return VillagerUnitModel.ArmPose.BUILDING;
        }
        else if (entity instanceof EvokerUnit evokerUnit) {
            return evokerUnit.getEvokerArmPose();
        }
        else if (entity instanceof PillagerUnit) {
            // CROSSBOW_HOLD
            // CROSSBOW_CHARGE
            return VillagerUnitModel.ArmPose.CROSSBOW_CHARGE;
        }
        else if (entity instanceof MilitiaUnit militiaUnit) {
            if (militiaUnit.isUsingBow()) {
                if (militiaUnit.isAggressive())
                    return VillagerUnitModel.ArmPose.BOW_AND_ARROW;
                else
                    return VillagerUnitModel.ArmPose.CROSSBOW_CHARGE;
            }
            else
                return VillagerUnitModel.ArmPose.ATTACKING;
        }
        else if (entity instanceof AttackerUnit attackerUnit) {
            SelectedTargetGoal<?> goal = ((Unit) entity).getTargetGoal();
            if (goal != null && goal.getTarget() != null ||
                    (attackerUnit.getAttackBuildingGoal() instanceof MeleeAttackBuildingGoal mabg && mabg.getBuildingTarget() != null))
                return VillagerUnitModel.ArmPose.ATTACKING;
        }
        return VillagerUnitModel.ArmPose.CROSSED;
    }

    @Override
    public void setupAnim(T entity, float p_102929_, float p_102930_, float p_102931_, float p_102932_, float p_102933_) {

        if (this.riding) {
            if (rightArm.visible) {
                this.rightArm.xRot = (-(float) Math.PI / 5F);
                this.rightArm.yRot = 0.0F;
                this.rightArm.zRot = 0.0F;
            }
            if (leftArm.visible) {
                this.leftArm.xRot = (-(float)Math.PI / 5F);
                this.leftArm.yRot = 0.0F;
                this.leftArm.zRot = 0.0F;
            }
            this.rightLeg.xRot = -1.4137167F;
            this.rightLeg.yRot = ((float)Math.PI / 10F);
            this.rightLeg.zRot = 0.07853982F;
            this.leftLeg.xRot = -1.4137167F;
            this.leftLeg.yRot = (-(float)Math.PI / 10F);
            this.leftLeg.zRot = -0.07853982F;
        } else {
            if (rightArm.visible) {
                this.rightArm.xRot = Mth.cos(p_102929_ * 0.6662F + (float)Math.PI) * 2.0F * p_102930_ * 0.5F;
                this.rightArm.yRot = 0.0F;
                this.rightArm.zRot = 0.0F;
            }
            if (leftArm.visible) {
                this.leftArm.xRot = Mth.cos(p_102929_ * 0.6662F) * 2.0F * p_102930_ * 0.5F;
                this.leftArm.yRot = 0.0F;
                this.leftArm.zRot = 0.0F;
            }
            this.rightLeg.xRot = Mth.cos(p_102929_ * 0.6662F) * 1.4F * p_102930_ * 0.5F;
            this.rightLeg.yRot = 0.0F;
            this.rightLeg.zRot = 0.0F;
            this.leftLeg.xRot = Mth.cos(p_102929_ * 0.6662F + (float)Math.PI) * 1.4F * p_102930_ * 0.5F;
            this.leftLeg.yRot = 0.0F;
            this.leftLeg.zRot = 0.0F;
        }

        this.head.yRot = p_102932_ * ((float)Math.PI / 180F);
        this.head.xRot = p_102933_ * ((float)Math.PI / 180F);

        this.jacket.copyFrom(this.body);
        boolean isWearingChestplateOrLeggings = entity.getItemBySlot(EquipmentSlot.CHEST)
                .getItem() instanceof ArmorItem
                || entity.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof ArmorItem;
        this.jacket.visible = !isWearingChestplateOrLeggings;

        VillagerUnitModel.ArmPose armPose = getArmPose(entity);

        switch(armPose) {
            case ATTACKING -> {
                if (entity.getMainHandItem().isEmpty())
                    AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, this.attackTime, p_102931_);
                else
                    AnimationUtils.swingWeaponDown(this.rightArm, this.leftArm, entity, this.attackTime, p_102931_);
            }
            case SPELLCASTING -> {
                this.rightArm.z = 0.0F;
                this.rightArm.x = -5.0F;
                this.leftArm.z = 0.0F;
                this.leftArm.x = 5.0F;
                this.rightArm.xRot = Mth.cos(p_102931_ * 0.6662F) * 0.25F;
                this.leftArm.xRot = Mth.cos(p_102931_ * 0.6662F) * 0.25F;
                this.rightArm.zRot = 2.3561945F;
                this.leftArm.zRot = -2.3561945F;
                this.rightArm.yRot = 0.0F;
                this.leftArm.yRot = 0.0F;
            }
            case BOW_AND_ARROW -> {
                this.rightArm.yRot = -0.1F + this.head.yRot;
                this.rightArm.xRot = (-(float)Math.PI / 2F) + this.head.xRot;
                this.leftArm.xRot = -0.9424779F + this.head.xRot;
                this.leftArm.yRot = this.head.yRot - 0.4F;
                this.leftArm.zRot = ((float)Math.PI / 2F);
            }
            case CROSSBOW_HOLD -> AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, true);
            case CROSSBOW_CHARGE -> AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, entity, true);
        }

        boolean armsCrossed = armPose == VillagerUnitModel.ArmPose.CROSSED;
        this.crossedArms.visible = armsCrossed;
        this.leftArm.visible = !armsCrossed;
        this.rightArm.visible = !armsCrossed;

        if (entity instanceof ArmSwingingUnit armSwinger &&
                (armSwinger.isSwingingArmRepeatedly() ||
                        armSwinger.isSwingingArmOnce())) {

            List<Float> armRots = armSwinger.getNextArmRot();
            this.rightArm.xRot = armRots.get(0);
            this.rightArm.yRot = armRots.get(1);
            this.rightArm.zRot = armRots.get(2);

            int swingTime = armSwinger.getSwingTime();
            armSwinger.setSwingTime(swingTime + 1);

            if (swingTime >= ArmSwingingUnit.SWING_TIME_MAX) {
                armSwinger.setSwingTime(0);
                if (armSwinger.isSwingingArmOnce())
                    armSwinger.setSwingingArmOnce(false);
            }
        }
    }

    @Override
    protected void setupAttackAnimation(T pLivingEntity, float pAgeInTicks) {
        if (this.attackTime > 0.0F && pLivingEntity.getArmPose() == AbstractIllager.IllagerArmPose.ATTACKING) {
            AnimationUtils.swingWeaponDown(this.rightArm, this.leftArm, pLivingEntity, this.attackTime, pAgeInTicks);
        } else {
            super.setupAttackAnimation(pLivingEntity, pAgeInTicks);
        }
    }
}