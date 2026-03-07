package com.solegendary.reignofnether.unit.modelling.models;

// Made with Blockbench 5.0.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.animations.EnchanterAnimations;
import com.solegendary.reignofnether.unit.units.villagers.EnchanterUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class EnchanterModel<T extends Entity> extends KeyframeHierarchicalModel<T> {

	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "enchanter_layer"), "main");
	private final ModelPart main;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart hat;
	private final ModelPart cape;
	private final ModelPart armsTogether;
	private final ModelPart rArm;
	private final ModelPart lArm;
	private final ModelPart lLeg;
	private final ModelPart rLeg;
	private final ModelPart spine;
	private final ModelPart bookCoverR;
	private final ModelPart page2;
	private final ModelPart bookCoverL;
	private final ModelPart page;

	public EnchanterModel(ModelPart root) {
		this.main = root.getChild("Main");
		this.body = this.main.getChild("Body");
		this.head = this.body.getChild("Head");
		this.hat = this.head.getChild("Hat");
		this.cape = this.body.getChild("Cape");
		this.armsTogether = this.body.getChild("ArmsTogether");
		this.rArm = this.body.getChild("Rarm");
		this.lArm = this.body.getChild("Larm");
		this.lLeg = this.main.getChild("Lleg");
		this.rLeg = this.main.getChild("Rleg");
		this.spine = this.main.getChild("Spine");
		this.bookCoverR = this.spine.getChild("BookCoverR");
		this.page2 = this.bookCoverR.getChild("Page2");
		this.bookCoverL = this.spine.getChild("BookCoverL");
		this.page = this.bookCoverL.getChild("Page");
	}

	@Override
	public @NotNull ModelPart root() {
		return this.main;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Main = partdefinition.addOrReplaceChild("Main", CubeListBuilder.create(), PartPose.offset(1.0F, 25.0F, 3.0F));

		PartDefinition Body = Main.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(18, 22).addBox(-4.5556F, -7.0556F, -2.1111F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(2, 40).addBox(-4.5556F, -7.0556F, -2.1111F, 8.0F, 19.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offset(-0.4444F, -17.9444F, 0.1111F));

		PartDefinition Head = Body.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(24, 0).addBox(-1.0F, -3.0F, -6.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.5556F, -7.0556F, -0.1111F));

		PartDefinition Hat = Head.addOrReplaceChild("Hat", CubeListBuilder.create(), PartPose.offset(0.0F, -6.5F, 0.0F));

		PartDefinition Hat_r1 = Hat.addOrReplaceChild("Hat_r1", CubeListBuilder.create().texOffs(47, 0).addBox(-5.0F, -4.5F, -5.0F, 10.0F, 8.0F, 10.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, -3.75F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition Cape = Body.addOrReplaceChild("Cape", CubeListBuilder.create(), PartPose.offset(-0.5556F, -7.0556F, 1.8889F));

		PartDefinition Cape_r1 = Cape.addOrReplaceChild("Cape_r1", CubeListBuilder.create().texOffs(71, 40).mirror().addBox(-6.0F, 0.0F, -1.0F, 12.0F, 23.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, -0.75F, 0.0F, 3.1416F, 0.0F));

		PartDefinition ArmsTogether = Body.addOrReplaceChild("ArmsTogether", CubeListBuilder.create(), PartPose.offset(-1.5556F, -5.0556F, 0.8889F));

		PartDefinition cube_r1 = ArmsTogether.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(40, 30).addBox(-4.0F, -2.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 3.75F, -3.5F, -0.6109F, 0.0F, 0.0F));

		PartDefinition cube_r2 = ArmsTogether.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(44, 18).addBox(-3.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(44, 18).mirror().addBox(9.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-4.0F, -1.0F, 0.0F, -0.6109F, 0.0F, 0.0F));

		PartDefinition Rarm = Body.addOrReplaceChild("Rarm", CubeListBuilder.create().texOffs(48, 38).addBox(0.0F, -0.5F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(3.4444F, -6.5556F, -0.1111F));

		PartDefinition Larm = Body.addOrReplaceChild("Larm", CubeListBuilder.create().texOffs(48, 38).mirror().addBox(-4.0F, -0.5F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.5556F, -6.5556F, -0.1111F));

		PartDefinition Lleg = Main.addOrReplaceChild("Lleg", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, -13.0F, 0.0F));

		PartDefinition Rleg = Main.addOrReplaceChild("Rleg", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-1.0F, 0.0F, -1.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, -13.0F, -1.0F));

		PartDefinition Spine = Main.addOrReplaceChild("Spine", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.6667F, -20.6667F, -5.5F, 1.6144F, -0.5672F, -1.5708F));

		PartDefinition cube_r3 = Spine.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(61, 86).addBox(-5.5F, -2.0F, -0.5F, 9.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -1.3333F, 0.3333F, 0.0F, 0.0F, -1.5708F));

		PartDefinition BookCoverR = Spine.addOrReplaceChild("BookCoverR", CubeListBuilder.create().texOffs(36, 68).mirror().addBox(0.0F, -5.0F, -8.0F, 1.0F, 9.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(60, 66).addBox(-0.5F, -4.5F, -7.0F, 1.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 0.1667F, -0.1667F));

		PartDefinition Page2 = BookCoverR.addOrReplaceChild("Page2", CubeListBuilder.create().texOffs(61, 66).mirror().addBox(0.0F, -5.0F, -7.0F, 0.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-0.5F, 0.5F, 0.0F));

		PartDefinition BookCoverL = Spine.addOrReplaceChild("BookCoverL", CubeListBuilder.create().texOffs(36, 68).addBox(-1.0F, -5.0F, -8.0F, 1.0F, 9.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(60, 66).mirror().addBox(-0.5F, -4.5F, -7.0F, 1.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-0.5F, 0.1667F, -0.1667F));

		PartDefinition Page = BookCoverL.addOrReplaceChild("Page", CubeListBuilder.create().texOffs(61, 66).mirror().addBox(0.0F, -5.0F, -7.0F, 0.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.5F, 0.5F, 0.0F));

		return LayerDefinition.create(meshdefinition, 97, 96);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		EnchanterUnit enchanter = ((EnchanterUnit) entity);

		this.rArm.visible = false;
		this.lArm.visible = false;

		if (enchanter.animateScale > 0 && enchanter.animateScaleReducing) {
			enchanter.animateScale -= 0.02f;
		}
		if (enchanter.animateScale <= 0) {
			enchanter.animateScale = 1.0f;
			enchanter.activeAnimDef = null;
			enchanter.activeAnimState = null;
			enchanter.animateScaleReducing = false;
			enchanter.stopAllAnimations();
		}

		AttributeInstance ms = enchanter.getAttribute(Attributes.MOVEMENT_SPEED);
		if (ms == null)
			return;
		float speed = (float) ms.getValue() * 10;

		// any once-off animation like attack or cast spell
		if (enchanter.activeAnimDef != null && enchanter.activeAnimState != null && enchanter.animateTicks > 0) {
			restartThenAnimate(enchanter, enchanter.activeAnimState, enchanter.activeAnimDef, ageInTicks, enchanter.animateScale, enchanter.animateSpeed);
			this.rArm.visible = true;
			this.lArm.visible = true;
		}
		// walk animation
		else if (!entity.isInWaterOrBubble() && limbSwingAmount > 0.001f) {
			restart(enchanter, enchanter.walkAnimState, ageInTicks);
			animateWalk(EnchanterAnimations.WALK, limbSwing, limbSwingAmount, speed, speed);
		}
		// idle animation
		else {
			restartThenAnimate(enchanter, enchanter.idleAnimState, EnchanterAnimations.IDLE, ageInTicks);
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}