package com.solegendary.reignofnether.unit.modelling.models;
// Made with Blockbench 5.0.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.animations.WildfireAnimations;
import com.solegendary.reignofnether.unit.units.piglins.WildfireUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class WildfireModel<T extends Entity> extends KeyframeHierarchicalModel<T> {

	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire_layer"), "main");

	@Override
	public ModelPart root() {
		return this.main;
	}

	public ModelPart getHead() {
		return this.head;
	}

	private final ModelPart main;
	private final ModelPart head;
	private final ModelPart bodySegment1;
	private final ModelPart bodySegment2;
	private final ModelPart bodySegment3;
	private final ModelPart bodyParts;
	private final ModelPart upperBodyParts0;
	private final ModelPart upperBodyParts1;
	private final ModelPart upperBodyParts2;
	private final ModelPart netherPiece0;
	private final ModelPart netherPiece1;
	private final ModelPart netherPiece2;
	private final ModelPart shields;
	private final ModelPart shield1;
	private final ModelPart shield2;
	private final ModelPart shield3;
	private final ModelPart shield4;

	public WildfireModel(ModelPart root) {
		this.main = root.getChild("main");
		this.head = this.main.getChild("head");
		this.bodySegment1 = this.main.getChild("bodySegment1");
		this.bodySegment2 = this.bodySegment1.getChild("bodySegment2");
		this.bodySegment3 = this.bodySegment2.getChild("bodySegment3");
		this.bodyParts = this.main.getChild("bodyParts");
		this.upperBodyParts0 = this.bodyParts.getChild("upperBodyParts0");
		this.upperBodyParts1 = this.bodyParts.getChild("upperBodyParts1");
		this.upperBodyParts2 = this.bodyParts.getChild("upperBodyParts2");
		this.netherPiece0 = this.bodyParts.getChild("netherPiece0");
		this.netherPiece1 = this.bodyParts.getChild("netherPiece1");
		this.netherPiece2 = this.bodyParts.getChild("netherPiece2");
		this.shields = this.main.getChild("shields");
		this.shield1 = this.shields.getChild("shield1");
		this.shield2 = this.shields.getChild("shield2");
		this.shield3 = this.shields.getChild("shield3");
		this.shield4 = this.shields.getChild("shield4");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition head = main.addOrReplaceChild("head", CubeListBuilder.create().texOffs(46, 46).addBox(-6.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(86, 51).addBox(-9.0F, -12.0F, -7.0F, 16.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(124, 44).addBox(-3.0F, -6.0F, -7.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(116, 59).addBox(-5.0F, -15.0F, -7.0F, 8.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(94, 80).addBox(-2.0F, -12.0F, -5.0F, 2.0F, 2.0F, 12.0F, new CubeDeformation(0.0F))
				.texOffs(122, 77).addBox(-2.0F, -10.0F, 5.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(106, 113).addBox(0.0F, -12.0F, -1.0F, 8.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(114, 94).addBox(-10.0F, -12.0F, -1.0F, 8.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(56, 117).addBox(5.0F, -20.0F, -1.0F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(0, 119).addBox(-10.0F, -17.0F, -1.0F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(76, 125).addBox(-10.0F, -19.0F, -1.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(122, 88).addBox(2.0F, -20.0F, -1.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -48.0F, -2.0F));

		PartDefinition bodySegment1 = main.addOrReplaceChild("bodySegment1", CubeListBuilder.create().texOffs(94, 59).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 16.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, -47.0F, -1.0F));

		PartDefinition bodySegment2 = bodySegment1.addOrReplaceChild("bodySegment2", CubeListBuilder.create().texOffs(90, 113).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 17.0F, 0.0F));

		PartDefinition bodySegment3 = bodySegment2.addOrReplaceChild("bodySegment3", CubeListBuilder.create().texOffs(18, 92).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition bodyParts = main.addOrReplaceChild("bodyParts", CubeListBuilder.create(), PartPose.offset(-1.0F, -34.3333F, -2.0F));

		PartDefinition upperBodyParts0 = bodyParts.addOrReplaceChild("upperBodyParts0", CubeListBuilder.create().texOffs(24, 124).addBox(0.0F, 0.0F, 0.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(9.0F, -11.6667F, -1.0F));

		PartDefinition upperBodyParts1 = bodyParts.addOrReplaceChild("upperBodyParts1", CubeListBuilder.create().texOffs(32, 124).addBox(0.0F, 0.0F, 0.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, 3.3333F, 7.0F));

		PartDefinition upperBodyParts2 = bodyParts.addOrReplaceChild("upperBodyParts2", CubeListBuilder.create().texOffs(124, 34).addBox(0.0F, -1.0F, 0.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -3.6667F, -6.0F));

		PartDefinition netherPiece0 = bodyParts.addOrReplaceChild("netherPiece0", CubeListBuilder.create().texOffs(78, 11).addBox(-2.0F, -6.0F, -1.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(93, 17).addBox(-3.0F, -4.0F, 2.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-9.0F, -1.6667F, 2.0F));

		PartDefinition netherPiece1 = bodyParts.addOrReplaceChild("netherPiece1", CubeListBuilder.create().texOffs(83, 20).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(88, 13).addBox(-2.0F, -4.0F, 0.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(76, 6).addBox(-1.0F, 0.0F, 0.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, 4.3333F, 6.0F));

		PartDefinition netherPiece2 = bodyParts.addOrReplaceChild("netherPiece2", CubeListBuilder.create().texOffs(122, 51).addBox(-3.0F, -4.0F, -1.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(89, 17).addBox(-2.0F, -6.0F, 0.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 9.3333F, -8.0F));

		PartDefinition shields = main.addOrReplaceChild("shields", CubeListBuilder.create(), PartPose.offset(-0.4983F, -21.4462F, -0.875F));

		PartDefinition shield1 = shields.addOrReplaceChild("shield1", CubeListBuilder.create().texOffs(183, 2).addBox(-11.65F, -23.7F, -0.75F, 24.0F, 40.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(190, 2).addBox(-8.65F, -29.7F, -0.75F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(197, 2).addBox(2.35F, -31.7F, -1.75F, 3.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(196, 46).addBox(-6.65F, 16.3F, -0.75F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(197, 46).addBox(-10.65F, 16.3F, -0.75F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(-1.65F, 16.3F, -0.75F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(6.35F, 16.3F, -0.75F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(11.35F, 16.3F, -0.75F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(196, 2).addBox(7.35F, -27.7F, -0.75F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.8517F, 0.1462F, -28.375F, -0.0436F, 0.0F, 0.0F));

		PartDefinition shield2 = shields.addOrReplaceChild("shield2", CubeListBuilder.create().texOffs(183, 2).addBox(-13.15F, -23.7F, -0.75F, 24.0F, 40.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(185, 3).addBox(-12.15F, -23.7F, -1.75F, 22.0F, 40.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(192, 2).addBox(-10.65F, -27.7F, -0.75F, 6.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(197, 2).addBox(-4.65F, -30.7F, -1.75F, 5.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(197, 46).addBox(-8.65F, 16.3F, -0.75F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(199, 46).addBox(-10.65F, 16.3F, -0.75F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(-0.65F, 16.3F, -0.75F, 5.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(6.35F, 16.3F, -0.75F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(4.35F, 16.3F, -0.75F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(196, 2).addBox(3.35F, -27.7F, -0.75F, 5.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(196, 2).addBox(0.35F, -27.7F, -0.75F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(198, 2).addBox(2.35F, -34.7F, -0.75F, 1.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(24.7483F, 0.1462F, -1.475F, 0.0F, -1.5708F, -0.0436F));

		PartDefinition shield3 = shields.addOrReplaceChild("shield3", CubeListBuilder.create().texOffs(184, 2).addBox(-12.65F, -23.7F, -0.75F, 24.0F, 40.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(186, 3).addBox(-11.65F, -23.7F, -1.75F, 22.0F, 40.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(192, 2).addBox(-11.15F, -25.7F, -0.75F, 14.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(192, 2).addBox(3.85F, -27.7F, -0.75F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(192, 2).addBox(7.85F, -25.7F, -0.75F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(191, 2).addBox(-12.15F, -24.7F, -0.75F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(191, 2).addBox(2.85F, -29.7F, -0.75F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(199, 47).addBox(-3.65F, 16.3F, -0.75F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(2.35F, 16.3F, -0.75F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(-6.65F, 16.3F, -0.75F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(5.35F, 16.3F, -0.75F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(-8.65F, 16.3F, -0.75F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2517F, 0.1462F, 30.525F, 3.098F, 0.0F, 3.1416F));

		PartDefinition shield4 = shields.addOrReplaceChild("shield4", CubeListBuilder.create().texOffs(183, 2).addBox(-13.65F, -23.7F, -0.75F, 24.0F, 40.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(187, 3).addBox(-12.65F, -23.7F, -1.75F, 22.0F, 40.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(197, 46).addBox(-9.65F, 16.3F, -0.75F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(197, 46).addBox(-13.65F, 16.3F, -0.75F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(196, 48).addBox(-5.65F, 16.3F, -0.75F, 9.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(5.35F, 16.3F, -0.75F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 48).addBox(3.35F, 16.3F, -0.75F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(191, 2).addBox(-4.65F, -26.7F, -0.75F, 11.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(194, 2).addBox(-13.65F, -25.7F, -0.75F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(200, 2).addBox(6.35F, -28.7F, -0.75F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-23.2517F, 0.1462F, -1.475F, 0.0F, 1.5708F, 0.0436F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	private float partialTick = 0;

	@Override
	public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTicks) {
		super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
		this.partialTick = partialTicks;
	}

	private void desyncShieldRotations(WildfireUnit wildfire) {
		float bodyYaw = Mth.lerp(partialTick, wildfire.yRotO, wildfire.getYRot());
		this.shields.yRot = -bodyYaw * (float)(Math.PI / 180f);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		WildfireUnit wildfire = ((WildfireUnit) entity);

		if (wildfire.animateScale > 0 && wildfire.animateScaleReducing) {
			wildfire.animateScale -= 0.02f;
		}
		if (wildfire.animateScale <= 0) {
			wildfire.animateScale = 1.0f;
			wildfire.activeAnimDef = null;
			wildfire.activeAnimState = null;
			wildfire.animateScaleReducing = false;
			wildfire.stopAllAnimations();
		}

		AttributeInstance ms = wildfire.getAttribute(Attributes.MOVEMENT_SPEED);
		if (ms == null)
			return;
		float speed = (float) ms.getValue() * 10;

		// any once-off animation like attack or cast spell
		if (wildfire.activeAnimDef != null && wildfire.activeAnimState != null && wildfire.animateTicks > 0) {
			restartThenAnimate(wildfire, wildfire.activeAnimState, wildfire.activeAnimDef, ageInTicks, wildfire.animateScale, wildfire.animateSpeed);
		} else {
			if (wildfire.getTarget() == null) {
				desyncShieldRotations(wildfire);
			}
			// walk animation
			if (!entity.isInWaterOrBubble() && limbSwingAmount > 0.001f) {
				restart(wildfire, wildfire.walkAnimState, ageInTicks);
				animateWalk(WildfireAnimations.WALK, limbSwing, limbSwingAmount, speed, speed);
			}
			// idle animation
			else {
				restartThenAnimate(wildfire, wildfire.idleAnimState, WildfireAnimations.IDLE, ageInTicks);
			}
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}