// Made with Blockbench 4.11.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

package com.solegendary.reignofnether.unit.modelling.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.animations.NecromancerAnimations;
import com.solegendary.reignofnether.unit.units.monsters.NecromancerUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class NecromancerModel<T extends Entity> extends KeyframeHierarchicalModel<T> {

	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "necromancer_layer"), "main");

	private final ModelPart main;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart legL;
	private final ModelPart legR;
	private final ModelPart armR;
	private final ModelPart armL;
	private final ModelPart pads;
	private final ModelPart cape;
	private final ModelPart staff;

	public NecromancerModel(ModelPart root) {
		this.main = root.getChild("main");
		this.head = this.main.getChild("head");
		this.body = this.main.getChild("body");
		this.legL = this.main.getChild("legL");
		this.legR = this.main.getChild("legR");
		this.armR = this.main.getChild("armR");
		this.armL = this.main.getChild("armL");
		this.pads = this.main.getChild("pads");
		this.cape = this.main.getChild("cape");
		this.staff = this.main.getChild("staff");
	}

	@Override
	public @NotNull ModelPart root() {
		return this.main;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition head = main.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 47).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, -24.0F, 1.0F));

		PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-7.0F, -8.0F, -1.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(3.0F, 0.0F, -3.0F, 0.0F, 0.0436F, 0.0F));

		PartDefinition body = main.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, -11.5F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(12, 32).addBox(-4.0F, 0.5F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.5F, 1.0F));

		PartDefinition legL = main.addOrReplaceChild("legL", CubeListBuilder.create().texOffs(0, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -12.0F, 1.0F));

		PartDefinition legR = main.addOrReplaceChild("legR", CubeListBuilder.create().texOffs(0, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -12.0F, 1.0F));

		PartDefinition armR = main.addOrReplaceChild("armR", CubeListBuilder.create(), PartPose.offset(5.0F, -23.0F, 1.0F));

		PartDefinition armR_r1 = armR.addOrReplaceChild("armR_r1", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396F));

		PartDefinition armL = main.addOrReplaceChild("armL", CubeListBuilder.create(), PartPose.offset(-5.0F, -22.0F, 1.0F));

		PartDefinition armL_r1 = armL.addOrReplaceChild("armL_r1", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, -1.0455F, 0.0756F, -0.0437F));

		PartDefinition pads = main.addOrReplaceChild("pads", CubeListBuilder.create().texOffs(42, 32).addBox(-9.0F, -3.0F, -3.0F, 5.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(42, 44).addBox(4.0F, -3.0F, -3.0F, 5.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -22.0F, 1.0F));

		PartDefinition cape = main.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(40, 3).addBox(-8.0F, -1.0F, -1.0F, 16.0F, 23.0F, 4.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, -23.0F, -0.4F));

		PartDefinition staff = main.addOrReplaceChild("staff", CubeListBuilder.create().texOffs(66, 41).addBox(-0.5F, -1.875F, -0.5F, 1.0F, 21.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(65, 32).addBox(-1.5F, -3.875F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(73, 47).addBox(-2.5F, -2.875F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(73, 47).addBox(1.5F, -2.875F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(73, 47).addBox(-0.5F, -2.875F, -2.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(73, 47).addBox(-0.5F, -2.875F, 1.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.5F, -19.125F, -6.5F));

		PartDefinition crep6_r1 = staff.addOrReplaceChild("crep6_r1", CubeListBuilder.create().texOffs(73, 47).addBox(0.0F, -3.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.875F, -1.5F, -1.5708F, 0.0F, 1.5708F));

		PartDefinition crep5_r1 = staff.addOrReplaceChild("crep5_r1", CubeListBuilder.create().texOffs(73, 47).addBox(0.0F, -3.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, -0.875F, 0.5F, 0.0F, 0.0F, 1.5708F));

		return LayerDefinition.create(meshdefinition, 80, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		NecromancerUnit necromancer = ((NecromancerUnit) entity);

		if (necromancer.animateScale > 0 && necromancer.animateScaleReducing) {
			necromancer.animateScale -= 0.02f;
		}
		if (necromancer.animateScale <= 0) {
			necromancer.animateScale = 1.0f;
			necromancer.activeAnimDef = null;
			necromancer.activeAnimState = null;
			necromancer.animateScaleReducing = false;
			necromancer.stopAllAnimations();
		}

		AttributeInstance ms = necromancer.getAttribute(Attributes.MOVEMENT_SPEED);
		if (ms == null)
			return;
		float speed = (float) ms.getValue() * 10;

		// any once-off animation like attack or cast spell
		if (necromancer.activeAnimDef != null && necromancer.activeAnimState != null && necromancer.animateTicks > 0) {
			restartThenAnimate(necromancer, necromancer.activeAnimState, necromancer.activeAnimDef, ageInTicks, necromancer.animateScale, necromancer.animateSpeed);
		}
		// walk animation
		else if (!entity.isInWaterOrBubble() && limbSwingAmount > 0.001f) {
			restart(necromancer, necromancer.walkAnimState, ageInTicks);
			animateWalk(NecromancerAnimations.WALK, limbSwing, limbSwingAmount, speed, speed);
		}
		// idle animation
		else {
			restartThenAnimate(necromancer, necromancer.idleAnimState, NecromancerAnimations.IDLE, ageInTicks);
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}