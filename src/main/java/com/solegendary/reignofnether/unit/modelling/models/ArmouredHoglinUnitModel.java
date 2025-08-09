package com.solegendary.reignofnether.unit.modelling.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;

public class ArmouredHoglinUnitModel<T extends Entity> extends EntityModel<T> {

    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "armoured_hoglin_unit_layer"), "main");

    private final ModelPart head;
    private final ModelPart head_rotation;
    private final ModelPart left_ear;
    private final ModelPart left_ear_rotation;
    private final ModelPart left_ear_rotation2;
    private final ModelPart right_ear;
    private final ModelPart right_ear_rotation;
    private final ModelPart right_ear_rotation2;
    private final ModelPart body;
    private final ModelPart mane;
    private final ModelPart front_left_leg;
    private final ModelPart front_right_leg;
    private final ModelPart back_left_leg;
    private final ModelPart back_right_leg;

    public ArmouredHoglinUnitModel(ModelPart root) {
        this.head = root.getChild("head");
        this.head_rotation = this.head.getChild("head_rotation");
        this.left_ear = root.getChild("left_ear");
        this.left_ear_rotation = this.left_ear.getChild("left_ear_rotation");
        this.left_ear_rotation2 = this.left_ear_rotation.getChild("left_ear_rotation2");
        this.right_ear = root.getChild("right_ear");
        this.right_ear_rotation = this.right_ear.getChild("right_ear_rotation");
        this.right_ear_rotation2 = this.right_ear_rotation.getChild("right_ear_rotation2");
        this.body = root.getChild("body");
        this.mane = root.getChild("mane");
        this.front_left_leg = root.getChild("front_left_leg");
        this.front_right_leg = root.getChild("front_right_leg");
        this.back_left_leg = root.getChild("back_left_leg");
        this.back_right_leg = root.getChild("back_right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 3.0F, -8.5F));

        PartDefinition head_rotation = head.addOrReplaceChild("head_rotation", CubeListBuilder.create().texOffs(0, 80).addBox(-7.0F, -3.0F, -19.0F, 14.0F, 6.0F, 19.0F, new CubeDeformation(0.4F))
        .texOffs(66, 80).addBox(-7.0F, -3.0F, -19.0F, 14.0F, 6.0F, 19.0F, new CubeDeformation(0.0F))
        .texOffs(40, 105).addBox(6.0F, -9.0F, -13.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 105).addBox(-8.0F, -9.0F, -13.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.8727F, 0.0F, 0.0F));

        PartDefinition armor_crest_r1 = head_rotation.addOrReplaceChild("armor_crest_r1", CubeListBuilder.create().texOffs(57, 107).addBox(-2.0F, -3.3F, -2.7F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, -6.5F, -0.3054F, 0.0F, 0.0F));

        PartDefinition left_ear = partdefinition.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(6.0F, 1.0F, -11.5F));

        PartDefinition left_ear_rotation = left_ear.addOrReplaceChild("left_ear_rotation", CubeListBuilder.create(), PartPose.offsetAndRotation(-6.5F, 1.75F, 3.0F, 0.8727F, 0.0F, 0.0F));

        PartDefinition left_ear_rotation2 = left_ear_rotation.addOrReplaceChild("left_ear_rotation2", CubeListBuilder.create().texOffs(84, 69).addBox(0.0F, -1.0F, -2.0F, 6.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.5F, -1.75F, -3.0F, 0.0F, 0.0F, 0.7854F));

        PartDefinition right_ear = partdefinition.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-6.0F, 1.0F, -11.5F));

        PartDefinition right_ear_rotation = right_ear.addOrReplaceChild("right_ear_rotation", CubeListBuilder.create(), PartPose.offsetAndRotation(6.5F, 1.75F, 3.0F, 0.8727F, 0.0F, 0.0F));

        PartDefinition right_ear_rotation2 = right_ear_rotation.addOrReplaceChild("right_ear_rotation2", CubeListBuilder.create().texOffs(84, 74).addBox(-6.0F, -1.0F, -2.0F, 6.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.5F, -1.75F, -3.0F, 0.0F, 0.0F, -0.7854F));

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -7.0F, -13.0F, 16.0F, 14.0F, 26.0F, new CubeDeformation(0.0F))
        .texOffs(0, 40).addBox(-8.0F, -7.0F, -13.0F, 16.0F, 14.0F, 26.0F, new CubeDeformation(0.4F)), PartPose.offset(0.0F, 7.0F, 4.5F));

        PartDefinition mane = partdefinition.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(84, 0).addBox(0.0F, 0.0F, -9.0F, 0.0F, 10.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -7.0F, -2.5F));

        PartDefinition front_left_leg = partdefinition.addOrReplaceChild("front_left_leg", CubeListBuilder.create().texOffs(84, 29).addBox(-3.0F, 0.0F, -2.75F, 6.0F, 14.0F, 6.0F, new CubeDeformation(0.0F))
        .texOffs(108, 29).addBox(-3.0F, 0.0F, -2.75F, 6.0F, 14.0F, 6.0F, new CubeDeformation(0.4F)), PartPose.offset(4.0F, 10.0F, -4.75F));

        PartDefinition front_right_leg = partdefinition.addOrReplaceChild("front_right_leg", CubeListBuilder.create().texOffs(84, 49).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F, new CubeDeformation(0.0F))
        .texOffs(108, 49).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F, new CubeDeformation(0.4F)), PartPose.offset(-4.0F, 10.0F, -4.5F));

        PartDefinition back_left_leg = partdefinition.addOrReplaceChild("back_left_leg", CubeListBuilder.create().texOffs(0, 105).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F, new CubeDeformation(0.0F))
        .texOffs(0, 121).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F, new CubeDeformation(0.4F)), PartPose.offset(4.5F, 13.0F, 14.0F));

        PartDefinition back_right_leg = partdefinition.addOrReplaceChild("back_right_leg", CubeListBuilder.create().texOffs(20, 105).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F, new CubeDeformation(0.0F))
        .texOffs(21, 121).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F, new CubeDeformation(0.4F)), PartPose.offset(-4.5F, 13.0F, 14.0F));

        return LayerDefinition.create(meshdefinition, 150, 150);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.right_ear.zRot = -0.6981317F - limbSwingAmount * Mth.sin(limbSwing);
        this.left_ear.zRot = 0.6981317F + limbSwingAmount * Mth.sin(limbSwing);
        this.head.yRot = netHeadYaw * 0.017453292F;
        int $$6 = ((HoglinBase)entity).getAttackAnimationRemainingTicks();
        float $$7 = 1.0F - (float)Mth.abs(10 - 2 * $$6) / 10.0F;
        this.head.xRot = Mth.lerp($$7, 0, -1.2217F);
        this.head.y = 2.0F;
        this.mane.z = -7.0F;
        float $$8 = 1.2F;
        this.front_right_leg.xRot = Mth.cos(limbSwing) * 1.2F * limbSwingAmount;
        this.front_left_leg.xRot = Mth.cos(limbSwing + 3.1415927F) * 1.2F * limbSwingAmount;
        this.back_right_leg.xRot = this.front_left_leg.xRot;
        this.back_left_leg.xRot = this.front_right_leg.xRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        left_ear.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        right_ear.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        mane.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        front_left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        front_right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        back_left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        back_right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}