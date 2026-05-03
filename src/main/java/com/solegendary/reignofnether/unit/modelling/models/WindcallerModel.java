package com.solegendary.reignofnether.unit.modelling.models;// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.modelling.animations.WindcallerAnimations;
import com.solegendary.reignofnether.unit.units.villagers.WindcallerUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class WindcallerModel<T extends Entity> extends KeyframeHierarchicalModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ReignOfNether.MOD_ID, "windcaller_layer"), "main");
	private final ModelPart bone;
	private final ModelPart Body;
	private final ModelPart Cape;
	private final ModelPart Head;
	private final ModelPart Larm;
	private final ModelPart Rarm;
	private final ModelPart Staff;
	private final ModelPart RLeg;
	private final ModelPart Lleg;

	public WindcallerModel(ModelPart root) {
		this.bone = root.getChild("bone");
		this.Body = this.bone.getChild("Body");
		this.Cape = this.Body.getChild("Cape");
		this.Head = this.Body.getChild("Head");
		this.Larm = this.Body.getChild("Larm");
		this.Rarm = this.Body.getChild("Rarm");
		this.Staff = this.Rarm.getChild("Staff");
		this.RLeg = this.bone.getChild("RLeg");
		this.Lleg = this.bone.getChild("Lleg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(-1.0F, 12.0F, 1.0F));

		PartDefinition Body = bone.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, -12.0F, -3.0F, 8.0F, 18.0F, 6.0F, new CubeDeformation(0.25F))
				.texOffs(16, 20).addBox(-4.0F, -12.0F, -3.0F, 8.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -1.0F));

		PartDefinition Cape = Body.addOrReplaceChild("Cape", CubeListBuilder.create().texOffs(44, 18).addBox(-4.0F, -1.0F, -1.0F, 9.0F, 21.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -11.0F, 4.0F, 0.1745F, 0.0F, 0.0F));

		PartDefinition Head = Body.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -3.0F, -6.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(32, 2).addBox(-4.0F, -18.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition Larm = Body.addOrReplaceChild("Larm", CubeListBuilder.create().texOffs(46, 46).addBox(-1.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F))
				.texOffs(29, 46).addBox(-1.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -11.0F, 0.0F, -0.3563F, -0.0292F, -0.6346F));

		PartDefinition Rarm = Body.addOrReplaceChild("Rarm", CubeListBuilder.create().texOffs(46, 46).mirror().addBox(-4.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)).mirror(false)
				.texOffs(29, 46).mirror().addBox(-4.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-4.0F, -11.0F, 0.0F, -0.7414F, 0.4087F, 0.1086F));

		PartDefinition Staff = Rarm.addOrReplaceChild("Staff", CubeListBuilder.create().texOffs(99, 27).addBox(-0.5F, -11.0F, -1.0F, 1.0F, 26.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(93, 30).addBox(0.5F, -9.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(93, 30).addBox(-2.5F, -12.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(93, 27).addBox(0.5F, -18.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(93, 30).addBox(-1.5F, -19.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(92, 18).addBox(-1.5F, -20.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(107, 34).addBox(-1.5F, -17.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(112, 33).addBox(-2.5F, -19.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(112, 33).addBox(1.5F, -19.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(107, 23).addBox(0.5F, -16.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(93, 34).addBox(0.5F, 10.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(93, 30).addBox(-1.5F, 8.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(93, 30).addBox(1.5F, 11.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(107, 27).addBox(-1.5F, -14.0F, -1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(107, 33).addBox(-0.5F, -16.0F, -1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 9.5F, 0.0F, 1.4835F, 0.0F, 1.5708F));

		PartDefinition RLeg = bone.addOrReplaceChild("RLeg", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-1.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-3.0F, 1.0F, -1.0F));

		PartDefinition Lleg = bone.addOrReplaceChild("Lleg", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-1.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(1.0F, 1.0F, -1.0F));

		return LayerDefinition.create(meshdefinition, 128, 64);
	}

	@Override
	public ModelPart root() {
		return this.bone;
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		WindcallerUnit windcaller = ((WindcallerUnit) entity);

		if (windcaller.animateScale > 0 && windcaller.animateScaleReducing) {
			windcaller.animateScale -= 0.02f;
		}
		if (windcaller.animateScale <= 0) {
			windcaller.animateScale = 1.0f;
			windcaller.activeAnimDef = null;
			windcaller.activeAnimState = null;
			windcaller.animateScaleReducing = false;
			windcaller.stopAllAnimations();
		}

		AttributeInstance ms = windcaller.getAttribute(Attributes.MOVEMENT_SPEED);
		if (ms == null)
			return;
		float speed = (float) ms.getValue() * 10;

		// any once-off animation like attack or cast spell
		if (windcaller.activeAnimDef != null && windcaller.activeAnimState != null && windcaller.animateTicks > 0) {
			restartThenAnimate(windcaller, windcaller.activeAnimState, windcaller.activeAnimDef, ageInTicks, windcaller.animateScale, windcaller.animateSpeed);
		}
		// walk animation
		else if (!entity.isInWaterOrBubble() && limbSwingAmount > 0.001f) {
			restart(windcaller, windcaller.walkAnimState, ageInTicks);
			animateWalk(WindcallerAnimations.WALK, limbSwing, limbSwingAmount, speed, speed);
		}
		// idle animation
		else {
			restartThenAnimate(windcaller, windcaller.idleAnimState, WindcallerAnimations.IDLE, ageInTicks);
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}