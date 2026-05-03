package com.solegendary.reignofnether.unit.modelling.models;// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.animations.WraithAnimations;
import com.solegendary.reignofnether.unit.units.monsters.WraithUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class WraithModel<T extends Entity> extends KeyframeHierarchicalModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ReignOfNether.MOD_ID, "wraith_layer"), "main");
	private final ModelPart bodyrotation;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart right_arm;
	private final ModelPart left_arm;

	public WraithModel(ModelPart root) {
		this.bodyrotation = root.getChild("bodyrotation");
		this.body = this.bodyrotation.getChild("body");
		this.head = this.body.getChild("head");
		this.right_arm = this.body.getChild("right_arm");
		this.left_arm = this.body.getChild("left_arm");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bodyrotation = partdefinition.addOrReplaceChild("bodyrotation", CubeListBuilder.create(), PartPose.offset(0.0F, 3.0F, 0.0F));

		PartDefinition body = bodyrotation.addOrReplaceChild("body", CubeListBuilder.create().texOffs(41, 2).addBox(-3.0F, 4.0F, -3.5F, 6.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(41, 2).addBox(-3.0F, 2.0F, -3.5F, 6.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(35, 3).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 12.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(17, 40).addBox(-3.5F, 0.0F, -1.5F, 7.0F, 18.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, -0.5F, 0.1745F, 0.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -7.0F, -2.0F, 8.0F, 12.0F, 8.0F, new CubeDeformation(0.3F))
				.texOffs(3, 20).addBox(-3.0F, -7.0F, -1.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, -0.1745F, 0.0F, 0.0F));

		PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(30, 18).mirror().addBox(-2.5F, -2.0F, -1.0F, 4.0F, 14.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(35, 3).mirror().addBox(-1.0F, -1.0F, 0.5F, 1.0F, 12.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-5.5F, 1.0F, -0.5F, -1.2654F, 0.0F, 0.0F));

		PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(35, 3).addBox(0.0F, -1.0F, 0.5F, 1.0F, 12.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(47, 18).addBox(-1.5F, -2.0F, -1.0F, 4.0F, 14.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.5F, 1.0F, -0.5F, -1.2217F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public ModelPart root() {
		return this.bodyrotation;
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		WraithUnit wraith = ((WraithUnit) entity);

		if (wraith.animateScale > 0 && wraith.animateScaleReducing) {
			wraith.animateScale -= 0.02f;
		}
		if (wraith.animateScale <= 0) {
			wraith.animateScale = 1.0f;
			wraith.activeAnimDef = null;
			wraith.activeAnimState = null;
			wraith.animateScaleReducing = false;
			wraith.stopAllAnimations();
		}

		AttributeInstance ms = wraith.getAttribute(Attributes.MOVEMENT_SPEED);
		if (ms == null)
			return;
		float speed = (float) ms.getValue() * 10;

		// any once-off animation like attack or cast spell
		if (wraith.activeAnimDef != null && wraith.activeAnimState != null && wraith.animateTicks > 0) {
			restartThenAnimate(wraith, wraith.activeAnimState, wraith.activeAnimDef, ageInTicks, wraith.animateScale, wraith.getAnimationSpeed());
		}
		// walk animation
		else if (!entity.isInWaterOrBubble() && limbSwingAmount > 0.001f) {
			restart(wraith, wraith.walkAnimState, ageInTicks);
			animateWalk(WraithAnimations.WALK, limbSwing, limbSwingAmount, speed, speed);
		}
		// idle animation
		else {
			restartThenAnimate(wraith, wraith.idleAnimState, WraithAnimations.IDLE, ageInTicks);
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bodyrotation.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}