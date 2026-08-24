//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.entities.models;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MagicProjectileModel<T extends Entity> extends HierarchicalModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "magic_projectile_layer"), "main");

    private final ModelPart root;
    private final ModelPart main;

    public MagicProjectileModel(ModelPart pRoot) {
        this.root = pRoot;
        this.main = pRoot.getChild("main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition $$0 = new MeshDefinition();
        PartDefinition $$1 = $$0.getRoot();
        $$1.addOrReplaceChild("main",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -1.0F, 8.0F, 8.0F, 2.0F)
                        .texOffs(0, 10)
                        .addBox(-1.0F, -4.0F, -4.0F, 2.0F, 8.0F, 8.0F)
                        .texOffs(20, 0)
                        .addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F),
                PartPose.ZERO);
        return LayerDefinition.create($$0, 64, 32);
    }

    public ModelPart root() {
        return this.root;
    }

    public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        this.main.yRot = pNetHeadYaw * 0.017453292F;
        this.main.xRot = pHeadPitch * 0.017453292F;
    }
}
