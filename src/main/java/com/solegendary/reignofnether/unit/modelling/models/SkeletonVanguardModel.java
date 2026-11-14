// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings

package com.solegendary.reignofnether.unit.modelling.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.animations.SkeletonVanguardAnimations;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonVanguardUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SkeletonVanguardModel<T extends Entity> extends KeyframeHierarchicalModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "skeleton_vanguard"), "main");

	// torso is a child of the baked root, so the root must be used as the animation root for the "torso" channels to resolve
	private final ModelPart root;
	private final ModelPart torso;
	private final ModelPart left_arm;
	private final ModelPart vanguard_glaive;
	private final ModelPart g_attack_point;
	private final ModelPart right_arm;
	private final ModelPart shield;
	private final ModelPart left_leg;
	private final ModelPart right_leg;
	private final ModelPart head;
	private final ModelPart left_hip;
	private final ModelPart right_hip;

	public SkeletonVanguardModel(ModelPart root) {
		this.root = root;
		this.torso = root.getChild("torso");
		this.left_arm = this.torso.getChild("left_arm");
		this.vanguard_glaive = this.left_arm.getChild("vanguard_glaive");
		this.g_attack_point = this.vanguard_glaive.getChild("g_attack_point");
		this.right_arm = this.torso.getChild("right_arm");
		this.shield = this.right_arm.getChild("shield");
		this.left_leg = this.torso.getChild("left_leg");
		this.right_leg = this.torso.getChild("right_leg");
		this.head = this.torso.getChild("head");
		this.left_hip = this.torso.getChild("left_hip");
		this.right_hip = this.torso.getChild("right_hip");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition torso = partdefinition.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(32, 31).addBox(-4.0F, -11.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(32, 18).addBox(-5.0F, -11.0F, -3.0F, 10.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 11.0F, 3.0F));

		PartDefinition left_arm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 49).addBox(0.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -11.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

		PartDefinition vanguard_glaive = left_arm.addOrReplaceChild("vanguard_glaive", CubeListBuilder.create().texOffs(30, 57).addBox(-0.5031F, -21.5823F, -19.9862F, 1.0063F, 4.025F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(50, 58).addBox(-0.5031F, -20.5761F, -18.98F, 1.0063F, 4.025F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(34, 57).addBox(-0.5031F, -19.5698F, -17.9737F, 1.0063F, 4.025F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(28, 40).addBox(-0.5031F, -18.5636F, -16.9675F, 1.0063F, 4.025F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(54, 58).addBox(-0.5031F, -17.5573F, -15.9612F, 1.0063F, 4.025F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(38, 57).addBox(-0.5031F, -16.5511F, -14.955F, 1.0063F, 4.025F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(58, 58).addBox(-0.5031F, -15.5448F, -13.9487F, 1.0063F, 4.025F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(42, 57).addBox(-0.5031F, -14.5386F, -12.9425F, 1.0063F, 4.025F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(46, 58).addBox(-0.5031F, -13.5323F, -11.9362F, 1.0063F, 4.025F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(28, 34).addBox(-0.5031F, -12.5261F, -10.93F, 1.0063F, 5.0313F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(62, 0).addBox(-0.5031F, -11.5198F, -9.9237F, 1.0063F, 3.0188F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(54, 15).addBox(-0.5031F, -9.5073F, -8.9175F, 1.0063F, 2.0125F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(0, 63).addBox(-0.5031F, -8.5011F, -7.9112F, 1.0063F, 2.0125F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(16, 55).addBox(-0.5031F, -8.5011F, -11.9362F, 1.0063F, 1.0063F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(62, 13).addBox(-0.5031F, -7.4948F, -6.905F, 1.0063F, 2.0125F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(4, 63).addBox(-0.5031F, -6.4886F, -5.8987F, 1.0063F, 2.0125F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(30, 62).addBox(-0.5031F, -5.4823F, -4.8925F, 1.0063F, 2.0125F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(58, 15).addBox(-0.5031F, -4.4761F, -3.8862F, 1.0063F, 2.0125F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(34, 62).addBox(-0.5031F, -3.4698F, -2.88F, 1.0063F, 2.0125F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(38, 62).addBox(-0.5031F, -2.4636F, -1.8737F, 1.0063F, 2.0125F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(62, 4).addBox(-0.5031F, -1.4573F, -0.8675F, 1.0063F, 2.0125F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(42, 62).addBox(-0.5031F, -0.4511F, 0.1388F, 1.0063F, 2.0125F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(62, 10).addBox(-0.5031F, 0.5552F, 1.145F, 1.0063F, 2.0125F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(8, 63).addBox(-0.5031F, 1.5614F, 2.1513F, 1.0063F, 2.0125F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(62, 58).addBox(-0.5031F, 2.5677F, 3.1575F, 1.0063F, 2.0125F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(62, 7).addBox(-0.5031F, 3.5739F, 4.1638F, 1.0063F, 2.0125F, 1.0063F, new CubeDeformation(0.0F))
		.texOffs(62, 61).addBox(-0.5031F, 4.5802F, 5.17F, 1.0063F, 2.0125F, 1.0062F, new CubeDeformation(0.0F))
		.texOffs(22, 43).addBox(-0.5031F, 5.5864F, 6.1763F, 1.0063F, 2.0125F, 2.0125F, new CubeDeformation(0.0F))
		.texOffs(28, 45).addBox(-0.5031F, 7.5989F, 8.1888F, 1.0063F, 1.0063F, 1.0063F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.931F, 11.7241F, 0.25F, 0.7854F, 0.0F, 0.0F));

		PartDefinition g_attack_point = vanguard_glaive.addOrReplaceChild("g_attack_point", CubeListBuilder.create(), PartPose.offset(0.0F, -16.0F, -16.0F));

		PartDefinition right_arm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 31).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -11.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

		PartDefinition shield = right_arm.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(48, 47).addBox(-5.0F, 2.9F, -0.5F, 10.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 15).addBox(-3.0F, -8.1F, -0.5F, 6.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(22, 47).addBox(-6.0F, -6.1F, -0.5F, 12.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(16, 49).addBox(-8.0F, -2.1F, -0.5F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(24, 57).addBox(6.0F, -2.1F, -0.5F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 11.6F, -0.5F));

		PartDefinition left_leg = torso.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 49).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 1.0F, 0.0F));

		PartDefinition right_leg = torso.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 57).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 1.0F, 0.0F));

		PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(22, 34).addBox(-1.0F, -11.0F, -6.0F, 2.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -11.0F, 0.0F));

		PartDefinition left_hip = torso.addOrReplaceChild("left_hip", CubeListBuilder.create().texOffs(0, 34).addBox(0.0F, 0.0F, -3.0F, 5.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.0F, 0.0F));

		PartDefinition right_hip = torso.addOrReplaceChild("right_hip", CubeListBuilder.create().texOffs(40, 0).addBox(-5.0F, 0.0F, -3.0F, 5.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		SkeletonVanguardUnit vanguard = ((SkeletonVanguardUnit) entity);

		if (vanguard.stopActiveAnimation) {
			vanguard.stopActiveAnimation = false;
			vanguard.activeAnimDef = null;
			vanguard.activeAnimState = null;
			vanguard.stopAllAnimations();
		}

		AttributeInstance ms = vanguard.getAttribute(Attributes.MOVEMENT_SPEED);
		if (ms == null)
			return;
		float speed = (float) ms.getValue() * 10;

		// any once-off animation like attack
		if (vanguard.activeAnimDef != null && vanguard.activeAnimState != null && vanguard.animateTicks > 0) {
			restartThenAnimate(vanguard, vanguard.activeAnimState, vanguard.activeAnimDef, ageInTicks, vanguard.getAnimationSpeed());
		}
		// walk animation
		else if (isWalking(entity, limbSwingAmount)) {
			beginWalkOverIdle(vanguard, vanguard.idleAnimState, SkeletonVanguardAnimations.IDLE, ageInTicks);
			animateWalk(SkeletonVanguardAnimations.WALK, limbSwing, limbSwingAmount, speed, speed);
		}
		// idle animation
		else {
			restartThenAnimate(vanguard, vanguard.idleAnimState, SkeletonVanguardAnimations.IDLE, ageInTicks);
		}
		finishPose(entity, ageInTicks);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		torso.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	public ModelPart root() {
		return root;
	}
}
