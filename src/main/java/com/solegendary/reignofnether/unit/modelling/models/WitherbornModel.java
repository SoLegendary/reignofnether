// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings

package com.solegendary.reignofnether.unit.modelling.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.animations.WitherbornAnimations;
import com.solegendary.reignofnether.unit.units.piglins.WitherbornUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class WitherbornModel<T extends Entity> extends KeyframeHierarchicalModel<T> {

	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "witherborn_layer"), "main");

	// Body, RLeg and LLeg are all top-level parts, so the baked root is used as the animation root
	private final ModelPart root;
	private final ModelPart Body;
	private final ModelPart Cape;
	private final ModelPart LHead;
	private final ModelPart RHead;
	private final ModelPart Head;
	private final ModelPart Eye;
	private final ModelPart Rarm;
	private final ModelPart Chain1;
	private final ModelPart Chain2;
	private final ModelPart Chain3;
	private final ModelPart Larm;
	private final ModelPart LifeEnder;
	private final ModelPart Swish;
	private final ModelPart RLeg;
	private final ModelPart LLeg;

	public WitherbornModel(ModelPart root) {
		this.root = root;
		this.Body = root.getChild("Body");
		this.Cape = this.Body.getChild("Cape");
		this.LHead = this.Body.getChild("LHead");
		this.RHead = this.Body.getChild("RHead");
		this.Head = this.Body.getChild("Head");
		this.Eye = this.Head.getChild("Eye");
		this.Rarm = this.Body.getChild("Rarm");
		this.Chain1 = this.Rarm.getChild("Chain1");
		this.Chain2 = this.Chain1.getChild("Chain2");
		this.Chain3 = this.Chain2.getChild("Chain3");
		this.Larm = this.Body.getChild("Larm");
		this.LifeEnder = this.Larm.getChild("LifeEnder");
		this.Swish = this.LifeEnder.getChild("Swish");
		this.RLeg = root.getChild("RLeg");
		this.LLeg = root.getChild("LLeg");
	}

	@Override
	public @NotNull ModelPart root() {
		return this.root;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(30, 72).addBox(-8.0F, -24.0F, -5.0F, 14.0F, 13.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(30, 93).addBox(-7.0F, -11.0F, -5.0F, 12.0F, 11.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(68, 34).addBox(-5.0F, 0.0F, -5.0F, 8.0F, 11.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -10.5F, 5.0F, 0.0F, 0.0F, 0.1309F));

		PartDefinition Cape = Body.addOrReplaceChild("Cape", CubeListBuilder.create().texOffs(0, 48).addBox(-10.0F, 0.0F, 0.0F, 14.0F, 55.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -24.0F, 3.0F, 0.5672F, 0.0F, 0.0F));

		PartDefinition LHead = Body.addOrReplaceChild("LHead", CubeListBuilder.create().texOffs(106, 88).addBox(-4.1F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.0F, -24.0F, -1.0F, -0.6939F, 0.084F, 0.1005F));

		PartDefinition RHead = Body.addOrReplaceChild("RHead", CubeListBuilder.create().texOffs(106, 72).addBox(-3.9F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(10.0F, -23.0F, -1.0F));

		PartDefinition Head = Body.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(30, 48).addBox(-7.0F, -8.9556F, -7.3889F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(0, 142).addBox(-7.0F, -8.9556F, -7.3889F, 14.0F, 10.0F, 14.0F, new CubeDeformation(-0.5F))
				.texOffs(68, 0).addBox(-8.0F, -8.0556F, -7.3889F, 16.0F, 4.0F, 14.0F, new CubeDeformation(1.0F))
				.texOffs(68, 18).addBox(-8.0F, -2.0556F, -7.3889F, 16.0F, 2.0F, 14.0F, new CubeDeformation(1.5F))
				.texOffs(0, 26).addBox(-8.5F, -14.1556F, -8.8889F, 17.0F, 5.0F, 17.0F, new CubeDeformation(0.0F))
				.texOffs(2, 35).addBox(-1.95F, -9.1556F, -8.8889F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-8.5F, -23.1556F, -8.8889F, 17.0F, 9.0F, 17.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -24.9444F, -0.6111F, 0.0F, 0.0F, -0.1309F));

		PartDefinition Eye = Head.addOrReplaceChild("Eye", CubeListBuilder.create().texOffs(37, 58).addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -3.5556F, -6.5889F));

		PartDefinition Rarm = Body.addOrReplaceChild("Rarm", CubeListBuilder.create().texOffs(0, 104).addBox(-1.0F, -1.0F, -2.0F, 4.0F, 34.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(40, 112).addBox(-2.0F, 28.0F, -3.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, -23.0F, -1.0F, 0.0F, 0.0F, -0.2182F));

		PartDefinition Chain1 = Rarm.addOrReplaceChild("Chain1", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0F, 30.5F, 1.0F, 0.0133F, 0.002F, 0.1288F));

		PartDefinition Chain1_r1 = Chain1.addOrReplaceChild("Chain1_r1", CubeListBuilder.create().texOffs(64, 112).addBox(-2.5F, 0.5F, 0.0F, 4.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition Chain2 = Chain1.addOrReplaceChild("Chain2", CubeListBuilder.create().texOffs(64, 112).addBox(-2.0F, -0.5F, -0.5F, 4.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, -0.0436F));

		PartDefinition Chain3 = Chain2.addOrReplaceChild("Chain3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0873F));

		PartDefinition Chain3_r1 = Chain3.addOrReplaceChild("Chain3_r1", CubeListBuilder.create().texOffs(64, 117).addBox(-2.5F, -0.5F, 0.0F, 4.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition Larm = Body.addOrReplaceChild("Larm", CubeListBuilder.create().texOffs(106, 34).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 34.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.0F, -23.0F, -1.0F, -1.3963F, 0.7854F, 0.0F));

		PartDefinition LifeEnder = Larm.addOrReplaceChild("LifeEnder", CubeListBuilder.create().texOffs(163, 88).addBox(-0.75F, 54.0F, -2.0F, 1.0F, 4.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(170, 36).addBox(-0.75F, 6.0F, -2.0F, 1.0F, 48.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(164, 23).addBox(-1.6F, 3.0F, -5.0F, 3.0F, 3.0F, 9.0F, new CubeDeformation(0.0F))
				.texOffs(170, 12).addBox(-1.6F, -3.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(-0.1F))
				.texOffs(168, 1).addBox(-1.6F, -6.0F, -2.5F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(205, 27).addBox(-1.6F, 8.0F, 4.0F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(134, 27).addBox(-1.6F, -2.0F, -10.0F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(191, 29).addBox(-1.6F, 3.0F, 4.0F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(151, 27).addBox(-1.6F, 1.0F, -8.0F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6F, 30.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition Swish = LifeEnder.addOrReplaceChild("Swish", CubeListBuilder.create().texOffs(232, 25).addBox(0.0F, -27.5F, 0.0F, 0.0F, 55.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.2102F, 30.2169F, -1.2026F));

		PartDefinition RLeg = partdefinition.addOrReplaceChild("RLeg", CubeListBuilder.create().texOffs(86, 34).addBox(-2.0F, 0.0F, -3.0F, 5.0F, 35.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, -11.0F, 5.0F, 0.0F, 0.0F, -0.0436F));

		PartDefinition LLeg = partdefinition.addOrReplaceChild("LLeg", CubeListBuilder.create().texOffs(86, 74).addBox(-1.0F, 0.0F, -3.0F, 5.0F, 35.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -11.0F, 5.0F, 0.0F, 0.0F, 0.0436F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		WitherbornUnit witherborn = ((WitherbornUnit) entity);

		if (witherborn.animateScale > 0 && witherborn.animateScaleReducing) {
			witherborn.animateScale -= 0.02f;
		}
		if (witherborn.animateScale <= 0) {
			witherborn.animateScale = 1.0f;
			witherborn.activeAnimDef = null;
			witherborn.activeAnimState = null;
			witherborn.animateScaleReducing = false;
			witherborn.stopAllAnimations();
		}

		AttributeInstance ms = witherborn.getAttribute(Attributes.MOVEMENT_SPEED);
		if (ms == null)
			return;
		float speed = (float) ms.getValue() * 7; // slower than usual

		// any once-off animation like attack or cast spell
		if (witherborn.activeAnimDef != null && witherborn.activeAnimState != null && witherborn.animateTicks > 0) {
			restartThenAnimate(witherborn, witherborn.activeAnimState, witherborn.activeAnimDef, ageInTicks, witherborn.animateScale, witherborn.animateSpeed);
		}
		// walk animation
		else if (!entity.isInWaterOrBubble() && limbSwingAmount > 0.001f) {
			restart(witherborn, witherborn.walkAnimState, ageInTicks);
			animateWalk(WitherbornAnimations.WALK, limbSwing, limbSwingAmount, speed, speed);
		}
		// idle animation
		else {
			restartThenAnimate(witherborn, witherborn.idleAnimState, WitherbornAnimations.IDLE, ageInTicks);
		}
		finishPose(entity, ageInTicks);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		RLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		LLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
