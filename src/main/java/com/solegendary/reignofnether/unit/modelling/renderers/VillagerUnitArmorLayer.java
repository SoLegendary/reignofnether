package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.unit.modelling.models.VillagerUnitModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;

import java.util.Map;

public class VillagerUnitArmorLayer<T extends LivingEntity, M extends VillagerUnitModel<T>> extends RenderLayer<T, M> {
    private final HumanoidModel<T> innerModel;
    private final HumanoidModel<T> outerModel;

    public VillagerUnitArmorLayer(RenderLayerParent<T, M> parent, HumanoidModel<T> innerModel, HumanoidModel<T> outerModel) {
        super(parent);
        this.innerModel = innerModel;
        this.outerModel = outerModel;
    }

    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.CHEST, packedLight, this.getArmorModel(EquipmentSlot.CHEST));
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.LEGS, packedLight, this.getArmorModel(EquipmentSlot.LEGS));
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.FEET, packedLight, this.getArmorModel(EquipmentSlot.FEET));
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.HEAD, packedLight, this.getArmorModel(EquipmentSlot.HEAD));
    }

    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource buffer, T entity, EquipmentSlot slot, int packedLight, HumanoidModel<T> model) {
        ItemStack itemstack = entity.getItemBySlot(slot);
        if (itemstack.getItem() instanceof ArmorItem armorItem) {
            if (armorItem.getEquipmentSlot() == slot) {
                this.getParentModel().copyPropertiesTo(model);
                this.setPartVisibility(model, slot);
                this.copyModelProperties(this.getParentModel(), model);
                
                // Use default setupAnim to let it handle swaying/sneaking if compatible, 
                // but we rely on copyModelProperties for bone matching.
                // model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                
                boolean flag = this.usesInnerModel(slot);
                // boolean flag1 = itemstack.hasFoil(); // 1.20 method? Check mappings.
                
                ResourceLocation texture = getArmorResource(entity, itemstack, slot, null);
                
                VertexConsumer vertexconsumer = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(texture), false, itemstack.hasFoil());
                model.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private HumanoidModel<T> getArmorModel(EquipmentSlot slot) {
        return this.usesInnerModel(slot) ? this.innerModel : this.outerModel;
    }

    private boolean usesInnerModel(EquipmentSlot slot) {
        return slot == EquipmentSlot.LEGS;
    }

    private void setPartVisibility(HumanoidModel<T> model, EquipmentSlot slot) {
        model.setAllVisible(false);
        switch (slot) {
            case HEAD:
                model.head.visible = true;
                model.hat.visible = true;
                break;
            case CHEST:
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
                break;
            case LEGS:
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
                break;
            case FEET:
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
                break;
        }
    }
    
    private void copyModelProperties(M parent, HumanoidModel<T> armor) {
        // Map VillagerUnitModel parts to HumanoidModel parts
        armor.head.copyFrom(parent.getHead());
        
        // Villager model has "body" child in root
        armor.body.copyFrom(parent.root().getChild("body"));
        
        armor.leftArm.copyFrom(parent.root().getChild("left_arm"));
        armor.rightArm.copyFrom(parent.root().getChild("right_arm"));
        armor.leftLeg.copyFrom(parent.root().getChild("left_leg"));
        armor.rightLeg.copyFrom(parent.root().getChild("right_leg"));
        
        armor.hat.copyFrom(parent.getHat());
        
        // Sync visibility
        armor.leftArm.visible &= parent.root().getChild("left_arm").visible;
        armor.rightArm.visible &= parent.root().getChild("right_arm").visible;
    }
    
    // Helper for texture
    public ResourceLocation getArmorResource(net.minecraft.world.entity.Entity entity, ItemStack stack, EquipmentSlot slot, String type) {
        ArmorItem item = (ArmorItem)stack.getItem();
        String texture = item.getMaterial().getName();
        String domain = "minecraft";
        int idx = texture.indexOf(':');
        if (idx != -1) {
            domain = texture.substring(0, idx);
            texture = texture.substring(idx + 1);
        }
        String s1 = String.format("%s:textures/models/armor/%s_layer_%d%s.png", domain, texture, (usesInnerModel(slot) ? 2 : 1), type == null ? "" : String.format("_%s", type));
        
        s1 = ForgeHooksClient.getArmorTexture(entity, stack, s1, slot, type);
        return ResourceLocation.tryParse(s1);
    }
}

