package com.solegendary.reignofnether.unit.modelling.models;// Made with Blockbench 5.0.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.unit.modelling.animations.MarauderAnimations;
import com.solegendary.reignofnether.unit.units.piglins.MarauderUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class MarauderModel<T extends Entity> extends KeyframeHierarchicalModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("reignofnether", "piglin_marauder"), "main");
	private final ModelPart body;
	private final ModelPart chest;
	private final ModelPart frontleftskirt;
	private final ModelPart frontrightskirt;
	private final ModelPart backrightskirt;
	private final ModelPart backleftskirt;
	private final ModelPart rightskirt;
	private final ModelPart leftskirt;
	private final ModelPart head;
	private final ModelPart left_ear;
	private final ModelPart right_ear;
	private final ModelPart left_arm;
	private final ModelPart flail;
	private final ModelPart chain1;
	private final ModelPart chain2;
	private final ModelPart chain3;
	private final ModelPart chain4;
	private final ModelPart chain5;
	private final ModelPart chain6;
	private final ModelPart chain7;
	private final ModelPart flail_end;
	private final ModelPart right_arm;
	private final ModelPart left_leg;
	private final ModelPart right_leg;

	public MarauderModel(ModelPart root) {
		this.body = root.getChild("body");
		this.chest = this.body.getChild("chest");
		this.frontleftskirt = this.chest.getChild("frontleftskirt");
		this.frontrightskirt = this.chest.getChild("frontrightskirt");
		this.backrightskirt = this.chest.getChild("backrightskirt");
		this.backleftskirt = this.chest.getChild("backleftskirt");
		this.rightskirt = this.chest.getChild("rightskirt");
		this.leftskirt = this.chest.getChild("leftskirt");
		this.head = this.chest.getChild("head");
		this.left_ear = this.head.getChild("left_ear");
		this.right_ear = this.head.getChild("right_ear");
		this.left_arm = this.chest.getChild("left_arm");
		this.flail = this.left_arm.getChild("flail");
		this.chain1 = this.flail.getChild("chain1");
		this.chain2 = this.chain1.getChild("chain2");
		this.chain3 = this.chain2.getChild("chain3");
		this.chain4 = this.chain3.getChild("chain4");
		this.chain5 = this.chain4.getChild("chain5");
		this.chain6 = this.chain5.getChild("chain6");
		this.chain7 = this.chain6.getChild("chain7");
		this.flail_end = this.chain7.getChild("flail_end");
		this.right_arm = this.chest.getChild("right_arm");
		this.left_leg = this.body.getChild("left_leg");
		this.right_leg = this.body.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -23.0F, -6.0F, 20.0F, 23.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(0, 35).addBox(-10.0F, -23.0F, -6.0F, 20.0F, 13.0F, 12.0F, new CubeDeformation(0.4F))
		.texOffs(118, 110).addBox(-4.0F, -8.0F, -7.0F, 8.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -14.0F, 0.0F, 0.0873F, -0.1134F, -0.0436F));

		PartDefinition frontleftskirt = chest.addOrReplaceChild("frontleftskirt", CubeListBuilder.create(), PartPose.offset(5.0F, 0.0F, -6.0F));

		PartDefinition frontleftskirt_r1 = frontleftskirt.addOrReplaceChild("frontleftskirt_r1", CubeListBuilder.create().texOffs(116, 22).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0873F, 0.0F, 0.0F));

		PartDefinition frontrightskirt = chest.addOrReplaceChild("frontrightskirt", CubeListBuilder.create().texOffs(96, 22).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 0.0F, -6.0F, -0.1745F, 0.0F, 0.0F));

		PartDefinition backrightskirt = chest.addOrReplaceChild("backrightskirt", CubeListBuilder.create(), PartPose.offset(-5.0F, 0.0F, 6.0F));

		PartDefinition backrightskirt_r1 = backrightskirt.addOrReplaceChild("backrightskirt_r1", CubeListBuilder.create().texOffs(116, 28).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0436F, 0.0F, 0.0F));

		PartDefinition backleftskirt = chest.addOrReplaceChild("backleftskirt", CubeListBuilder.create(), PartPose.offset(5.0F, 0.0F, 6.0F));

		PartDefinition backleftskirt_r1 = backleftskirt.addOrReplaceChild("backleftskirt_r1", CubeListBuilder.create().texOffs(96, 28).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0436F, 0.0F, 0.0F));

		PartDefinition rightskirt = chest.addOrReplaceChild("rightskirt", CubeListBuilder.create().texOffs(70, 109).addBox(0.0F, 0.0F, -6.0F, 0.0F, 3.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1745F));

		PartDefinition leftskirt = chest.addOrReplaceChild("leftskirt", CubeListBuilder.create().texOffs(94, 109).addBox(0.0F, 0.0F, -6.0F, 0.0F, 3.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1745F));

		PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 60).addBox(-6.0F, -9.0F, -5.0F, 12.0F, 9.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(0, 97).addBox(-6.0F, -9.0F, -5.0F, 12.0F, 7.0F, 6.0F, new CubeDeformation(0.4F))
		.texOffs(110, 50).addBox(-1.5F, -11.0F, -7.1F, 3.0F, 5.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(50, 110).addBox(-3.0F, -6.0F, -9.0F, 6.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(110, 63).addBox(3.0F, -4.0F, -10.0F, 4.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(110, 89).addBox(-7.0F, -4.0F, -10.0F, 4.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -23.0F, -4.0F, -0.0873F, 0.0873F, 0.0436F));

		PartDefinition left_ear = head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(116, 73).addBox(0.0F, 0.0F, -2.0F, 1.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.5F, -7.0F, 0.0F, 0.0F, 0.0F, -0.6109F));

		PartDefinition right_ear = head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(118, 99).addBox(-1.0F, 0.0F, -2.0F, 1.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.5F, -7.0F, 0.0F, 0.0F, 0.0F, 0.6109F));

		PartDefinition left_arm = chest.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(44, 60).addBox(-1.0F, -4.0F, -4.0F, 8.0F, 22.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(98, 34).addBox(3.0F, 9.0F, -5.0F, 5.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(76, 74).addBox(-2.0F, 14.0F, -5.0F, 10.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(76, 89).addBox(1.0F, -5.0F, -5.0F, 7.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.0F, -19.0F, 1.0F, -0.1745F, -0.3491F, -0.1745F));

		PartDefinition flail = left_arm.addOrReplaceChild("flail", CubeListBuilder.create(), PartPose.offset(2.9F, -4.0F, 0.0F));

		PartDefinition chain1 = flail.addOrReplaceChild("chain1", CubeListBuilder.create().texOffs(70, 52).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.1F, 0.0F, 0.0F));

		PartDefinition chain2 = chain1.addOrReplaceChild("chain2", CubeListBuilder.create().texOffs(64, 52).addBox(0.0F, 0.0F, -1.5F, 0.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.0F, 0.0F));

		PartDefinition chain3 = chain2.addOrReplaceChild("chain3", CubeListBuilder.create().texOffs(70, 52).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.0F, 0.0F));

		PartDefinition chain4 = chain3.addOrReplaceChild("chain4", CubeListBuilder.create().texOffs(64, 52).addBox(0.0F, 0.0F, -1.5F, 0.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.0F, 0.0F));

		PartDefinition chain5 = chain4.addOrReplaceChild("chain5", CubeListBuilder.create().texOffs(70, 52).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.0F, 0.0F));

		PartDefinition chain6 = chain5.addOrReplaceChild("chain6", CubeListBuilder.create().texOffs(64, 52).addBox(0.0F, 0.0F, -1.5F, 0.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.0F, 0.0F));

		PartDefinition chain7 = chain6.addOrReplaceChild("chain7", CubeListBuilder.create().texOffs(70, 52).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.0F, 0.0F));

		PartDefinition flail_end = chain7.addOrReplaceChild("flail_end", CubeListBuilder.create().texOffs(0, 79).addBox(-4.5F, 3.0F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));

		PartDefinition spike2_r1 = flail_end.addOrReplaceChild("spike2_r1", CubeListBuilder.create().texOffs(96, 0).addBox(-1.5F, -5.5F, -5.5F, 3.0F, 11.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 7.5F, 0.0F, -0.7854F, 0.0F, 0.0F));

		PartDefinition spike1_r1 = flail_end.addOrReplaceChild("spike1_r1", CubeListBuilder.create().texOffs(0, 110).addBox(-5.5F, -5.5F, -1.5F, 11.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 7.5F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition right_arm = chest.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(64, 0).addBox(-7.0F, -4.0F, -4.0F, 8.0F, 22.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(36, 90).addBox(-8.0F, -5.0F, -5.0F, 7.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(28, 110).addBox(-7.0F, 6.0F, -4.0F, 3.0F, 9.0F, 8.0F, new CubeDeformation(0.4F)), PartPose.offsetAndRotation(-11.0F, -17.0F, -2.0F, -0.2618F, -0.0873F, 0.1745F));

		PartDefinition left_leg = body.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(64, 30).addBox(-4.6F, -2.0F, -2.0F, 9.0F, 14.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.2F, -12.0F, 0.0F, 0.0F, -0.3054F, 0.0F));

		PartDefinition right_leg = body.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(76, 52).addBox(-4.2691F, -2.0F, -2.0029F, 9.0F, 14.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.2F, -12.0F, -3.0F, 0.0F, -0.0436F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		MarauderUnit marauder = ((MarauderUnit) entity);

		if (marauder.animateScale > 0 && marauder.animateScaleReducing) {
			marauder.animateScale -= 0.02f;
		}
		if (marauder.animateScale <= 0) {
			marauder.animateScale = 1.0f;
			marauder.activeAnimDef = null;
			marauder.activeAnimState = null;
			marauder.animateScaleReducing = false;
			marauder.stopAllAnimations();
		}

		AttributeInstance ms = marauder.getAttribute(Attributes.MOVEMENT_SPEED);
		if (ms == null)
			return;
		float speed = (float) ms.getValue() * 10;

		// any once-off animation like attack or cast spell
		if (marauder.activeAnimDef != null && marauder.activeAnimState != null && marauder.animateTicks > 0) {
			restartThenAnimate(marauder, marauder.activeAnimState, marauder.activeAnimDef, ageInTicks, marauder.animateScale, marauder.getAnimationSpeed());
		}
		// walk animation
		else if (!entity.isInWaterOrBubble() && limbSwingAmount > 0.001f) {
			restart(marauder, marauder.walkAnimState, ageInTicks);
			animateWalk(MarauderAnimations.WALK, limbSwing, limbSwingAmount, speed, speed);
		}
		// idle animation
		else {
			restartThenAnimate(marauder, marauder.idleAnimState, MarauderAnimations.IDLE, ageInTicks);
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	public ModelPart root() {
		return body;
	}
}