package com.solegendary.reignofnether.entities.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class ThrownMaceModel<T extends Entity> extends EntityModel<T> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("reignofnether", "mace"), "main");
    private final ModelPart main;
    private final ModelPart mace;
    private final ModelPart head2;

    public ThrownMaceModel(ModelPart root) {
        this.main = root.getChild("main");
        this.mace = this.main.getChild("mace");
        this.head2 = this.mace.getChild("head2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(8.0F, 41.0F, 2.0F));

        PartDefinition mace = main.addOrReplaceChild("mace", CubeListBuilder.create().texOffs(135, 36).addBox(0.0F, -6.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-8.0F, -13.0F, -2.5F));

        PartDefinition head2 = mace.addOrReplaceChild("head2", CubeListBuilder.create().texOffs(124, 24).addBox(-4.0F, -5.0F, -1.0F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(124, 35).addBox(-2.0F, -7.0F, 1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -6.0F, 0.7F, 0.6981F, 0.0F, 0.0F));

        PartDefinition spike_r1 = head2.addOrReplaceChild("spike_r1", CubeListBuilder.create().texOffs(124, 35).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, -3.0F, 1.0F, 0.0F, -1.5708F, 1.5708F));

        PartDefinition spike_r2 = head2.addOrReplaceChild("spike_r2", CubeListBuilder.create().texOffs(124, 35).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -3.0F, 2.0F, 0.0F, 1.5708F, -1.5708F));

        PartDefinition spike_r3 = head2.addOrReplaceChild("spike_r3", CubeListBuilder.create().texOffs(124, 35).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -3.0F, 4.0F, -1.5708F, 0.0F, -3.1416F));

        PartDefinition spike_r4 = head2.addOrReplaceChild("spike_r4", CubeListBuilder.create().texOffs(124, 35).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -3.0F, -1.0F, 1.5708F, 0.0F, 0.0F));

        PartDefinition spike_r5 = head2.addOrReplaceChild("spike_r5", CubeListBuilder.create().texOffs(124, 35).addBox(0.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 2.0F, 2.0F, 0.0F, 0.0F, -3.1416F));

        return LayerDefinition.create(meshdefinition, 144, 96);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}