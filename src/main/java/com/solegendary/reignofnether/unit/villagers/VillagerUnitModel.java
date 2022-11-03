package com.solegendary.reignofnether.unit.villagers;

import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.VillagerHeadModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// TODO: add arms from VindicatorModel

// based on IllagerModel
@OnlyIn(Dist.CLIENT)
public class VillagerUnitModel<T extends Entity> extends HierarchicalModel<T> implements HeadedModel, VillagerHeadModel {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart hat;
    private final ModelPart hatRim;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    protected final ModelPart nose;

    public VillagerUnitModel(ModelPart p_171051_) {
        this.root = p_171051_;
        this.head = p_171051_.getChild("head");
        this.hat = this.head.getChild("hat");
        this.hatRim = this.hat.getChild("hat_rim");
        this.nose = this.head.getChild("nose");
        this.rightLeg = p_171051_.getChild("right_leg");
        this.leftLeg = p_171051_.getChild("left_leg");
    }

    public static MeshDefinition createBodyModel() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        float f = 0.5F;
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.ZERO);
        PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.51F)), PartPose.ZERO);
        partdefinition2.addOrReplaceChild("hat_rim", CubeListBuilder.create().texOffs(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F), PartPose.rotation((-(float)Math.PI / 2F), 0.0F, 0.0F));
        partdefinition1.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition partdefinition3 = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F), PartPose.ZERO);
        partdefinition3.addOrReplaceChild("jacket", CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.ZERO);
        partdefinition.addOrReplaceChild("arms", CubeListBuilder.create().texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F).texOffs(44, 22).addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, true).texOffs(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.0F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.0F, 12.0F, 0.0F));
        return meshdefinition;
    }

    public ModelPart root() {
        return this.root;
    }

    public void setupAnim(T p_104053_, float p_104054_, float p_104055_, float p_104056_, float p_104057_, float p_104058_) {
        boolean flag = false;
        if (p_104053_ instanceof AbstractVillager) {
            flag = ((AbstractVillager)p_104053_).getUnhappyCounter() > 0;
        }

        this.head.yRot = p_104057_ * ((float)Math.PI / 180F);
        this.head.xRot = p_104058_ * ((float)Math.PI / 180F);
        if (flag) {
            this.head.zRot = 0.3F * Mth.sin(0.45F * p_104056_);
            this.head.xRot = 0.4F;
        } else {
            this.head.zRot = 0.0F;
        }

        this.rightLeg.xRot = Mth.cos(p_104054_ * 0.6662F) * 1.4F * p_104055_ * 0.5F;
        this.leftLeg.xRot = Mth.cos(p_104054_ * 0.6662F + (float)Math.PI) * 1.4F * p_104055_ * 0.5F;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
    }

    public ModelPart getHead() {
        return this.head;
    }

    public void hatVisible(boolean p_104060_) {
        this.head.visible = p_104060_;
        this.hat.visible = p_104060_;
        this.hatRim.visible = p_104060_;
    }
}