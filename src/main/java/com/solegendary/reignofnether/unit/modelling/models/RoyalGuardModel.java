// Made with Blockbench 4.11.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

package com.solegendary.reignofnether.unit.modelling.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.animations.RoyalGuardAnimations;
import com.solegendary.reignofnether.unit.units.villagers.RoyalGuardUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class RoyalGuardModel<T extends Entity> extends KeyframeHierarchicalModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "royal_guard_layer"), "main");

	private final ModelPart main;
	private final ModelPart head;
	private final ModelPart helm;
	private final ModelPart body;
	private final ModelPart armR;
	private final ModelPart shield;
	private final ModelPart armL;
	private final ModelPart mace;
	private final ModelPart head2;
	private final ModelPart legR;
	private final ModelPart legL;

	public RoyalGuardModel(ModelPart root) {
		this.main = root.getChild("main");
		this.head = this.main.getChild("head");
		this.helm = this.head.getChild("helm");
		this.body = this.main.getChild("body");
		this.armR = this.main.getChild("armR");
		this.shield = this.armR.getChild("shield");
		this.armL = this.main.getChild("armL");
		this.mace = this.armL.getChild("mace");
		this.head2 = this.mace.getChild("head2");
		this.legR = this.main.getChild("legR");
		this.legL = this.main.getChild("legL");
	}

	@Override
	public @NotNull ModelPart root() {
		return this.main;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition head = main.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.5F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(24, 0).addBox(-1.0F, -3.0F, -6.5F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, -24.6F, -0.5F));

		PartDefinition helm = head.addOrReplaceChild("helm", CubeListBuilder.create().texOffs(34, 0).addBox(-8.0F, -11.0F, -1.5F, 10.0F, 10.0F, 9.0F, new CubeDeformation(0.0F))
				.texOffs(74, 9).addBox(-5.0F, -14.0F, -1.5F, 4.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(33, 0).addBox(2.0F, -9.0F, 1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(33, 3).addBox(3.0F, -11.0F, 1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(33, 3).mirror().addBox(-10.9F, -11.0F, 1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(33, 0).mirror().addBox(-9.0F, -9.0F, 1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(3.0F, 0.0F, -3.5F));

		PartDefinition body = main.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 20).addBox(-3.96F, -12.9152F, -3.8888F, 8.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(33, 39).addBox(-3.96F, -12.9152F, -3.8888F, 8.0F, 12.0F, 6.0F, new CubeDeformation(0.5F))
				.texOffs(66, 29).addBox(-3.96F, -12.9152F, -3.8888F, 8.0F, 12.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.offsetAndRotation(-1.04F, -11.0848F, -0.1112F, 0.0F, 0.1309F, 0.0F));

		PartDefinition cube2_r1 = body.addOrReplaceChild("cube2_r1", CubeListBuilder.create().texOffs(67, 51).mirror().addBox(-0.5F, -4.0F, -1.0F, 5.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(67, 51).addBox(-5.7F, -4.0F, -1.0F, 5.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.54F, -7.4152F, -4.5888F, 0.6109F, 0.0F, 0.0F));

		PartDefinition armR = main.addOrReplaceChild("armR", CubeListBuilder.create().texOffs(0, 40).addBox(1.0F, -1.0F, -1.4F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(99, 37).addBox(5.5F, -0.5F, -1.4F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(100, 28).addBox(1.5F, -1.5F, -1.4F, 4.0F, 7.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(91, 5).addBox(4.0F, 8.0F, -1.4F, 2.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(91, 14).addBox(3.0F, 11.0F, -1.4F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -23.0F, -2.0F, -0.7156F, 0.0F, 0.0F));

		PartDefinition shield = armR.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(3, 64).addBox(-5.0F, -11.0F, -1.9F, 14.0F, 22.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 12.0F, 0.5F, 0.6981F, 0.0F, 0.0F));

		PartDefinition armL = main.addOrReplaceChild("armL", CubeListBuilder.create().texOffs(16, 40).mirror().addBox(-3.5F, -0.7F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(99, 37).mirror().addBox(-6.0F, -0.2F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(100, 28).mirror().addBox(-4.0F, -1.2F, -2.5F, 4.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(110, 5).mirror().addBox(-4.5F, 8.3F, -3.0F, 2.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(110, 14).mirror().addBox(-4.5F, 11.3F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-5.5F, -23.3F, -1.0F, 0.5061F, -0.2705F, 0.2356F));

		PartDefinition mace = armL.addOrReplaceChild("mace", CubeListBuilder.create().texOffs(135, 36).addBox(0.0F, -6.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 10.3F, -1.5F, 2.3562F, 0.0F, 0.0F));

		PartDefinition head2 = mace.addOrReplaceChild("head2", CubeListBuilder.create().texOffs(124, 24).addBox(-4.0F, -5.0F, -1.0F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(124, 35).addBox(-2.0F, -7.0F, 1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -6.0F, 0.7F, 0.6981F, 0.0F, 0.0F));

		PartDefinition spike_r1 = head2.addOrReplaceChild("spike_r1", CubeListBuilder.create().texOffs(124, 35).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, -3.0F, 1.0F, 0.0F, -1.5708F, 1.5708F));

		PartDefinition spike_r2 = head2.addOrReplaceChild("spike_r2", CubeListBuilder.create().texOffs(124, 35).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -3.0F, 2.0F, 0.0F, 1.5708F, -1.5708F));

		PartDefinition spike_r3 = head2.addOrReplaceChild("spike_r3", CubeListBuilder.create().texOffs(124, 35).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -3.0F, 4.0F, -1.5708F, 0.0F, -3.1416F));

		PartDefinition spike_r4 = head2.addOrReplaceChild("spike_r4", CubeListBuilder.create().texOffs(124, 35).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -3.0F, -1.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition spike_r5 = head2.addOrReplaceChild("spike_r5", CubeListBuilder.create().texOffs(124, 35).addBox(0.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 2.0F, 2.0F, 0.0F, 0.0F, -3.1416F));

		PartDefinition legR = main.addOrReplaceChild("legR", CubeListBuilder.create().texOffs(0, 22).addBox(0.0F, 0.0F, -5.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(93, 48).addBox(0.0F, 0.0F, -5.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.5F))
				.texOffs(97, 42).addBox(0.0F, 5.0F, -6.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.0F, -1.0F, -0.0436F, 0.0F, 0.0F));

		PartDefinition legL = main.addOrReplaceChild("legL", CubeListBuilder.create().texOffs(44, 22).mirror().addBox(-3.0F, -5.0F, 0.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(110, 48).mirror().addBox(-3.0F, -5.0F, 0.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.5F)).mirror(false)
				.texOffs(113, 42).mirror().addBox(-3.0F, 0.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-3.0F, -7.0F, 0.0F, 0.1309F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 144, 96);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		RoyalGuardUnit royalGuard = ((RoyalGuardUnit) entity);

		if (royalGuard.animateScale > 0 && royalGuard.animateScaleReducing) {
			royalGuard.animateScale -= 0.02f;
		}
		if (royalGuard.animateScale <= 0) {
			royalGuard.animateScale = 1.0f;
			royalGuard.activeAnimDef = null;
			royalGuard.activeAnimState = null;
			royalGuard.animateScaleReducing = false;
			royalGuard.stopAllAnimations();
		}

		AttributeInstance ms = royalGuard.getAttribute(Attributes.MOVEMENT_SPEED);
		if (ms == null)
			return;
		float speed = (float) ms.getValue() * 10;

		// any once-off animation like attack or cast spell
		if (royalGuard.activeAnimDef != null && royalGuard.activeAnimState != null && royalGuard.animateTicks > 0) {
			restartThenAnimate(royalGuard, royalGuard.activeAnimState, royalGuard.activeAnimDef, ageInTicks, royalGuard.animateScale, royalGuard.animateSpeed);
		}
		// walk animation
		else if (!entity.isInWaterOrBubble() && limbSwingAmount > 0.001f) {
			restart(royalGuard, royalGuard.walkAnimState, ageInTicks);
			animateWalk(RoyalGuardAnimations.WALK, limbSwing, limbSwingAmount, speed, speed);
		}
		// idle animation
		else {
			restartThenAnimate(royalGuard, royalGuard.idleAnimState, RoyalGuardAnimations.IDLE, ageInTicks);
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}