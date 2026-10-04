package com.solegendary.reignofnether.unit.modelling.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class TotemOfShieldingModel<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "totem_of_shielding"), "main");
	private final ModelPart group;

	public TotemOfShieldingModel(ModelPart root) {
		this.group = root.getChild("group");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition group = partdefinition.addOrReplaceChild("group", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -12.0F, -2.0F, 10.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(23, 21).addBox(2.0F, -12.0F, -2.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(23, 21).addBox(-9.0F, -12.0F, -2.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(23, 17).addBox(-6.5F, -13.0F, -1.0F, 7.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 17).addBox(-6.5F, -21.0F, -2.0F, 7.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(29, 0).addBox(-8.5F, -17.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(29, 0).addBox(0.5F, -17.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 48, 48);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		group.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}