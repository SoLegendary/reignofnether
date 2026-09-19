package com.solegendary.reignofnether.unit.modelling.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class TotemOfProtectionModel<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "totem_of_protection"), "main");
	private final ModelPart root;

	public TotemOfProtectionModel(ModelPart root) {
		this.root = root.getChild("root");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create().texOffs(1, 17).addBox(-7.0F, -11.0F, -2.0F, 9.0F, 11.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(29, 30).addBox(2.25F, -10.0F, -1.5F, 4.0F, 5.0F, 3.0F, new CubeDeformation(0.3F))
		.texOffs(0, 35).addBox(2.0F, -17.0F, -3.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(29, 30).mirror().addBox(-11.25F, -10.0F, -1.5F, 4.0F, 5.0F, 3.0F, new CubeDeformation(0.3F)).mirror(false)
		.texOffs(0, 35).mirror().addBox(-13.0F, -17.0F, -3.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(0, 0).addBox(-8.0F, -21.25F, -3.0F, 11.0F, 10.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.offset(2.5F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 48, 48);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}