// Made with Blockbench 4.11.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

package com.solegendary.reignofnether.unit.modelling.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.animations.PiglinMerchantAnimations;
import com.solegendary.reignofnether.unit.units.piglins.PiglinMerchantUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class PiglinMerchantModel<T extends Entity> extends KeyframeHierarchicalModel<T> {

	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "piglin_merchant_layer"), "main");

	private final ModelPart main;
	private final ModelPart head;
	private final ModelPart earL;
	private final ModelPart earR;
	private final ModelPart pack;
	private final ModelPart bone;
	private final ModelPart chainR;
	private final ModelPart chainL;
	private final ModelPart wart;
	private final ModelPart wart2;
	private final ModelPart wart3;
	private final ModelPart body;
	private final ModelPart armR;
	private final ModelPart armL;
	private final ModelPart legL;
	private final ModelPart legR;

	public PiglinMerchantModel(ModelPart root) {
		this.main = root.getChild("main");
		this.head = this.main.getChild("head");
		this.earL = this.head.getChild("earL");
		this.earR = this.head.getChild("earR");
		this.pack = this.main.getChild("pack");
		this.bone = this.pack.getChild("bone");
		this.chainR = this.bone.getChild("chainR");
		this.chainL = this.bone.getChild("chainL");
		this.wart = this.bone.getChild("wart");
		this.wart2 = this.bone.getChild("wart2");
		this.wart3 = this.bone.getChild("wart3");
		this.body = this.main.getChild("body");
		this.armR = this.main.getChild("armR");
		this.armL = this.main.getChild("armL");
		this.legL = this.main.getChild("legL");
		this.legR = this.main.getChild("legR");
	}

	@Override
	public @NotNull ModelPart root() {
		return this.main;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(0.0F, 5.0F, -2.0F));

		PartDefinition head = main.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 0).addBox(-5.0F, -7.0F, -8.0F, 10.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(61, 2).addBox(-2.0F, -3.0F, -9.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(72, 0).addBox(-6.0F, -8.0F, -9.0F, 12.0F, 9.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(32, 5).addBox(-3.0F, -1.0F, -9.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(34, 2).addBox(2.0F, -1.0F, -9.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0436F, 0.0F, 0.0F));

		PartDefinition earL = head.addOrReplaceChild("earL", CubeListBuilder.create().texOffs(72, 0).mirror().addBox(-1.9239F, -0.6173F, -1.0F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-3.2F, -4.0F, -6.0F, 0.0F, 0.0F, 0.3927F));

		PartDefinition earR = head.addOrReplaceChild("earR", CubeListBuilder.create().texOffs(72, 0).addBox(0.2F, -1.0F, -1.0F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(60, 18).addBox(-0.8F, 2.0F, 1.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.9F, -4.0F, -6.0F, 0.0F, 0.0F, -0.3927F));

		PartDefinition pack = main.addOrReplaceChild("pack", CubeListBuilder.create().texOffs(76, 82).mirror().addBox(-10.3333F, -7.3333F, -3.8333F, 21.0F, 15.0F, 13.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(73, 110).addBox(-8.3333F, 7.6667F, -3.8333F, 16.0F, 9.0F, 9.0F, new CubeDeformation(0.0F))
				.texOffs(81, 65).addBox(-11.8333F, -14.3333F, -3.8333F, 24.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.3333F, -4.1667F, 10.8333F, 0.2182F, 0.0F, 0.0F));

		PartDefinition bone = pack.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(149, 47).mirror().addBox(14.0F, -1.5F, 0.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(82, 53).addBox(-16.0F, -0.5F, 0.0F, 30.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(149, 47).addBox(-18.0F, -1.5F, 0.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6667F, -10.8333F, -5.8333F, -0.0873F, 0.0F, 0.0F));

		PartDefinition chainR = bone.addOrReplaceChild("chainR", CubeListBuilder.create(), PartPose.offset(5.5F, 1.0F, -1.0F));

		PartDefinition chain_r1 = chainR.addOrReplaceChild("chain_r1", CubeListBuilder.create().texOffs(136, 6).addBox(0.0F, -0.5F, 0.0F, 3.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, 0.0F, 1.0F, -0.48F, 0.0F, 0.0F));

		PartDefinition chainL = bone.addOrReplaceChild("chainL", CubeListBuilder.create(), PartPose.offset(-7.5F, 1.0F, -1.0F));

		PartDefinition chain_r2 = chainL.addOrReplaceChild("chain_r2", CubeListBuilder.create().texOffs(136, 6).addBox(0.0F, -0.5F, 0.0F, 3.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, 0.0F, 1.0F, -0.4363F, 0.0F, 0.0F));

		PartDefinition wart = bone.addOrReplaceChild("wart", CubeListBuilder.create().texOffs(104, 0).addBox(-3.0F, 0.5F, 0.0F, 6.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 1.0F, 0.0F, -0.1745F, 0.0F, 0.0F));

		PartDefinition wart_r1 = wart.addOrReplaceChild("wart_r1", CubeListBuilder.create().texOffs(104, 0).addBox(-5.0F, -5.5F, 0.0F, 6.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 6.0F, 1.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition wart2 = bone.addOrReplaceChild("wart2", CubeListBuilder.create().texOffs(122, 0).addBox(-3.0F, 0.5F, 0.0F, 6.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.0F, 1.0F, 0.0F, -0.1745F, 0.0F, 0.0F));

		PartDefinition wart_r2 = wart2.addOrReplaceChild("wart_r2", CubeListBuilder.create().texOffs(122, 0).addBox(-5.0F, -5.5F, 0.0F, 6.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 6.0F, 1.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition wart3 = bone.addOrReplaceChild("wart3", CubeListBuilder.create().texOffs(116, 0).addBox(-3.0F, 0.5F, 0.0F, 6.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.0F, 1.0F, 0.0F, -0.1745F, 0.0F, 0.0F));

		PartDefinition wart_r3 = wart3.addOrReplaceChild("wart_r3", CubeListBuilder.create().texOffs(116, 0).addBox(-5.0F, -5.5F, 0.0F, 6.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 6.0F, 1.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition body = main.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 24).addBox(-8.0F, -16.0F, -4.0F, 16.0F, 19.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(12, 76).addBox(-8.0F, -3.0F, -4.0F, 16.0F, 0.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 10.0F, 3.0F, 0.0785F, 0.0F, 0.0F));

		PartDefinition armR = main.addOrReplaceChild("armR", CubeListBuilder.create().texOffs(89, 27).addBox(-2.0F, 0.0F, -1.5F, 5.0F, 14.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.0F, -5.0F, 1.5F, -0.0873F, 0.2443F, -0.3578F));

		PartDefinition armL = main.addOrReplaceChild("armL", CubeListBuilder.create().texOffs(89, 27).mirror().addBox(-3.0F, -1.0F, -2.0F, 5.0F, 14.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-9.0F, -4.0F, 2.0F, -0.0873F, -0.2443F, 0.3578F));

		PartDefinition legL = main.addOrReplaceChild("legL", CubeListBuilder.create().texOffs(111, 30).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 7.0F, 3.0F, 0.0F, 0.48F, 0.0F));

		PartDefinition legR = main.addOrReplaceChild("legR", CubeListBuilder.create().texOffs(111, 30).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 10.0F, 3.0F, 0.0F, -0.3927F, 0.0F));

		return LayerDefinition.create(meshdefinition, 160, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		PiglinMerchantUnit merchant = ((PiglinMerchantUnit) entity);

		if (merchant.animateScale > 0 && merchant.animateScaleReducing) {
			merchant.animateScale -= 0.02f;
		}
		if (merchant.animateScale <= 0) {
			merchant.animateScale = 1.0f;
			merchant.activeAnimDef = null;
			merchant.activeAnimState = null;
			merchant.animateScaleReducing = false;
			merchant.stopAllAnimations();
		}

		AttributeInstance ms = merchant.getAttribute(Attributes.MOVEMENT_SPEED);
		if (ms == null)
			return;
		float speed = (float) ms.getValue() * 10;

		// any once-off animation like attack or cast spell
		if (merchant.activeAnimDef != null && merchant.activeAnimState != null && merchant.animateTicks > 0) {
			restartThenAnimate(merchant, merchant.activeAnimState, merchant.activeAnimDef, ageInTicks, merchant.animateScale, merchant.animateSpeed);
		}
		// walk animation
		else if (!entity.isInWaterOrBubble() && limbSwingAmount > 0.001f) {
			restart(merchant, merchant.walkAnimState, ageInTicks);
			animateWalk(PiglinMerchantAnimations.WALK, limbSwing, limbSwingAmount, speed, speed);
		}
		// idle animation
		else {
			restartThenAnimate(merchant, merchant.idleAnimState, PiglinMerchantAnimations.IDLE, ageInTicks);
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}