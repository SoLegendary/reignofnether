package com.solegendary.reignofnether.unit.modelling.models;// Made with Blockbench 5.0.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.animations.WretchedWraithAnimations;
import com.solegendary.reignofnether.unit.units.monsters.WretchedWraithUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class WretchedWraithModel<T extends Entity> extends KeyframeHierarchicalModel<T> {

	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretched_wraith_layer"), "Main");
	private final ModelPart Main;
	private final ModelPart FullMovement;
	private final ModelPart Head;
	private final ModelPart Crown;
	private final ModelPart SnowFlake;
	private final ModelPart Bones;
	private final ModelPart Ribs;
	private final ModelPart ArmR;
	private final ModelPart ArmL;

	public WretchedWraithModel(ModelPart root) {
		this.Main = root.getChild("Main");
		this.FullMovement = this.Main.getChild("FullMovement");
		this.Head = this.FullMovement.getChild("Head");
		this.Crown = this.Head.getChild("Crown");
		this.SnowFlake = this.Head.getChild("SnowFlake");
		this.Bones = this.FullMovement.getChild("Bones");
		this.Ribs = this.Bones.getChild("Ribs");
		this.ArmR = this.FullMovement.getChild("ArmR");
		this.ArmL = this.FullMovement.getChild("ArmL");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Main = partdefinition.addOrReplaceChild("Main", CubeListBuilder.create(), PartPose.offset(-4.25F, 17.0F, -3.0F));

		PartDefinition FullMovement = Main.addOrReplaceChild("FullMovement", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Head = FullMovement.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(4, 31).addBox(-6.2572F, -16.3876F, -8.3666F, 14.0F, 17.0F, 13.0F, new CubeDeformation(0.0F))
				.texOffs(1, 77).addBox(-5.2572F, -14.3876F, -7.6166F, 12.0F, 14.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5072F, -37.6124F, 3.3666F));

		PartDefinition Crown = Head.addOrReplaceChild("Crown", CubeListBuilder.create().texOffs(6, 12).addBox(0.0F, -7.0F, -1.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(14, 20).addBox(1.0F, -7.0F, -15.0F, 4.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(10, 20).addBox(7.0F, -7.0F, -13.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(8, 20).addBox(-2.0F, -7.0F, -9.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(10, 20).addBox(-3.5F, -7.0F, -13.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(13, 1).addBox(-3.0F, -14.0F, -6.0F, 1.0F, 13.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(13, 1).addBox(8.0F, -14.0F, -11.0F, 1.0F, 13.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(5, 20).addBox(7.0F, -9.0F, -4.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(6, 12).addBox(-5.0F, -7.0F, -4.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(6, 12).addBox(10.0F, -6.0F, -15.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(4, 20).addBox(2.0F, -9.0F, -2.0F, 3.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.2572F, -14.3876F, 5.6334F));

		PartDefinition SnowFlake = Head.addOrReplaceChild("SnowFlake", CubeListBuilder.create().texOffs(105, 2).addBox(-3.029F, -3.0502F, -3.2165F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.7718F, -26.3373F, -2.1501F));

		PartDefinition cube_r1 = SnowFlake.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(94, 8).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.029F, 0.9498F, -0.2165F, -1.0472F, -1.0472F, 1.0472F));

		PartDefinition Bones = FullMovement.addOrReplaceChild("Bones", CubeListBuilder.create().texOffs(62, 13).addBox(-7.0F, -1.0F, -3.5F, 12.0F, 29.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(111, 28).addBox(-2.0F, 0.0F, 1.5F, 2.0F, 22.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.25F, -37.0F, 0.0F, 0.3054F, 0.0F, 0.0F));

		PartDefinition Ribs = Bones.addOrReplaceChild("Ribs", CubeListBuilder.create().texOffs(-3, 105).addBox(-2.0F, -2.0F, -1.0F, 0.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
				.texOffs(-3, 105).mirror().addBox(-12.0F, -9.0F, -1.0F, 0.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(-3, 105).mirror().addBox(-12.0F, -2.0F, -1.0F, 0.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(-3, 105).addBox(-2.0F, -9.0F, -1.0F, 0.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
				.texOffs(65, 96).addBox(-10.0F, -10.0F, 0.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, 13.0F, -2.5F));

		PartDefinition cube_r2 = Ribs.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 108).addBox(-1.0F, -2.0F, -1.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 108).addBox(-1.0F, -9.0F, -1.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 0.0F, 7.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r3 = Ribs.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(9, 108).mirror().addBox(-1.0F, -2.0F, -1.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(9, 108).mirror().addBox(-1.0F, -9.0F, -1.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-3.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r4 = Ribs.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(9, 108).addBox(1.0F, -2.0F, -1.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(9, 108).addBox(1.0F, -9.0F, -1.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r5 = Ribs.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 108).mirror().addBox(1.0F, -2.0F, -1.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(0, 108).mirror().addBox(1.0F, -9.0F, -1.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-11.0F, 0.0F, 7.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition ArmR = FullMovement.addOrReplaceChild("ArmR", CubeListBuilder.create().texOffs(60, 123).addBox(-3.0F, -1.0F, -34.25F, 7.0F, 11.0F, 7.0F, new CubeDeformation(1.0F))
				.texOffs(52, 95).addBox(-3.0F, -1.0F, -34.25F, 7.0F, 4.0F, 7.0F, new CubeDeformation(0.9F))
				.texOffs(56, 97).addBox(-2.0F, 0.25F, -25.25F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.9F))
				.texOffs(64, 144).addBox(-2.0F, 0.25F, -25.25F, 5.0F, 9.0F, 6.0F, new CubeDeformation(1.0F)), PartPose.offsetAndRotation(10.25F, -37.0F, 2.0F, 0.48F, 0.0F, 0.0F));

		PartDefinition cube_r6 = ArmR.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(111, 28).addBox(-1.0F, -21.0F, -1.0F, 2.0F, 22.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, 2.5F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r7 = ArmR.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(66, 61).addBox(-6.0F, 0.0F, -4.0F, 7.0F, 15.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 2.0F, 4.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition ArmL = FullMovement.addOrReplaceChild("ArmL", CubeListBuilder.create().texOffs(60, 123).mirror().addBox(-4.0F, -1.0F, -34.25F, 7.0F, 11.0F, 7.0F, new CubeDeformation(1.0F)).mirror(false)
				.texOffs(52, 95).mirror().addBox(-4.0F, -1.0F, -34.25F, 7.0F, 4.0F, 7.0F, new CubeDeformation(0.9F)).mirror(false)
				.texOffs(56, 97).mirror().addBox(-3.0F, 0.25F, -25.25F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.9F)).mirror(false)
				.texOffs(64, 144).mirror().addBox(-3.0F, 0.25F, -25.25F, 5.0F, 9.0F, 6.0F, new CubeDeformation(1.0F)).mirror(false), PartPose.offsetAndRotation(-8.75F, -37.0F, 2.0F, 0.48F, 0.0F, 0.0F));

		PartDefinition cube_r8 = ArmL.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(111, 28).mirror().addBox(-1.0F, -21.0F, -1.0F, 2.0F, 22.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 2.0F, 2.5F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r9 = ArmL.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(66, 61).mirror().addBox(-1.0F, 0.0F, -4.0F, 7.0F, 15.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-3.0F, 2.0F, 4.0F, -1.5708F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public ModelPart root() {
		return this.Main;
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		WretchedWraithUnit wretchedWraith = ((WretchedWraithUnit) entity);

		if (wretchedWraith.animateScale > 0 && wretchedWraith.animateScaleReducing) {
			wretchedWraith.animateScale -= 0.02f;
		}
		if (wretchedWraith.animateScale <= 0) {
			wretchedWraith.animateScale = 1.0f;
			wretchedWraith.activeAnimDef = null;
			wretchedWraith.activeAnimState = null;
			wretchedWraith.animateScaleReducing = false;
			wretchedWraith.stopAllAnimations();
		}

		AttributeInstance ms = wretchedWraith.getAttribute(Attributes.MOVEMENT_SPEED);
		if (ms == null)
			return;
		float speed = (float) ms.getValue() * 10;

		// any once-off animation like attack or cast spell
		if (wretchedWraith.activeAnimDef != null && wretchedWraith.activeAnimState != null && wretchedWraith.animateTicks > 0) {
			restartThenAnimate(wretchedWraith, wretchedWraith.activeAnimState, wretchedWraith.activeAnimDef, ageInTicks, wretchedWraith.animateScale, wretchedWraith.getAnimationSpeed());
		}
		// walk animation
		else if (!entity.isInWaterOrBubble() && limbSwingAmount > 0.001f) {
			restart(wretchedWraith, wretchedWraith.walkAnimState, ageInTicks);
			animateWalk(WretchedWraithAnimations.WALK, limbSwing, limbSwingAmount, speed, speed);
		}
		// idle animation
		else {
			restartThenAnimate(wretchedWraith, wretchedWraith.idleAnimState, WretchedWraithAnimations.IDLE, ageInTicks);
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}