package com.solegendary.reignofnether.unit.modelling.layers;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.item.*;
import net.minecraftforge.client.ForgeHooksClient;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

// based on HumanoidArmorLayer
public class VillagerUnitArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {

    private static final Map<String, ResourceLocation> ARMOR_LOCATION_CACHE = Maps.newHashMap();
    private final A innerModel;
    private final A outerModel;

    public VillagerUnitArmorLayer(RenderLayerParent<T, M> pRenderer, A pInnerModel, A pOuterModel) {
        super(pRenderer);
        this.innerModel = pInnerModel;
        this.outerModel = pOuterModel;
    }

    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.CHEST, packedLight, this.getArmorModel(EquipmentSlot.CHEST));
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.LEGS, packedLight, this.getArmorModel(EquipmentSlot.LEGS));
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.FEET, packedLight, this.getArmorModel(EquipmentSlot.FEET));
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.HEAD, packedLight, this.getArmorModel(EquipmentSlot.HEAD));
    }

    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource buffer, T entity, EquipmentSlot slot, int packedLight, A model) {
        ItemStack itemstack = entity.getItemBySlot(slot);
        Item $$9 = itemstack.getItem();
        if ($$9 instanceof ArmorItem armoritem) {
            if (armoritem.getEquipmentSlot() == slot) {
                this.getParentModel().copyPropertiesTo(model);
                this.setPartVisibility(model, slot);
                Model armorModel = this.getArmorModelHook(entity, itemstack, slot, model);
                boolean flag = this.usesInnerModel(slot);
                if (armoritem instanceof DyeableLeatherItem) {
                    int i = ((DyeableLeatherItem)armoritem).getColor(itemstack);
                    float f = (float)(i >> 16 & 255) / 255.0F;
                    float f1 = (float)(i >> 8 & 255) / 255.0F;
                    float f2 = (float)(i & 255) / 255.0F;
                    this.renderModel(poseStack, buffer, packedLight, armoritem, armorModel, flag, f, f1, f2, this.getArmorResource(entity, itemstack, slot, (String)null));
                    this.renderModel(poseStack, buffer, packedLight, armoritem, armorModel, flag, 1.0F, 1.0F, 1.0F, this.getArmorResource(entity, itemstack, slot, "overlay"));
                } else {
                    this.renderModel(poseStack, buffer, packedLight, armoritem, armorModel, flag, 1.0F, 1.0F, 1.0F, this.getArmorResource(entity, itemstack, slot, (String)null));
                }
                //ArmorTrim.getTrim(entity.level().registryAccess(), itemstack).ifPresent((p_289638_) -> {
                //    this.renderTrim(armoritem.getMaterial(), poseStack, buffer, packedLight, p_289638_, armorModel, flag);
                //});
                if (itemstack.hasFoil()) {
                    this.renderGlint(poseStack, buffer, packedLight, armorModel);
                }
            }
        }
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

    private void renderModel(PoseStack p_289664_, MultiBufferSource p_289689_, int p_289681_, ArmorItem p_289650_, Model p_289658_, boolean p_289668_, float p_289678_, float p_289674_, float p_289693_, ResourceLocation armorResource) {
        VertexConsumer vertexconsumer = p_289689_.getBuffer(RenderType.armorCutoutNoCull(armorResource));
        p_289658_.renderToBuffer(p_289664_, vertexconsumer, p_289681_, OverlayTexture.NO_OVERLAY, p_289678_, p_289674_, p_289693_, 1.0F);
    }

    /*
    private void renderTrim(ArmorMaterial pArmorMaterial, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, ArmorTrim pTrim, A pModel, boolean pInnerTexture) {
        this.renderTrim(pArmorMaterial, pPoseStack, pBuffer, pPackedLight, pTrim, pModel, pInnerTexture);
    }

    private void renderTrim(ArmorMaterial p_289690_, PoseStack p_289687_, MultiBufferSource p_289643_, int p_289683_, ArmorTrim p_289692_, Model p_289663_, boolean p_289651_) {
        TextureAtlasSprite textureatlassprite = this.armorTrimAtlas.getSprite(p_289651_ ? p_289692_.innerTexture(p_289690_) : p_289692_.outerTexture(p_289690_));
        VertexConsumer vertexconsumer = textureatlassprite.wrap(p_289643_.getBuffer(Sheets.armorTrimsSheet()));
        p_289663_.renderToBuffer(p_289687_, vertexconsumer, p_289683_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
     */

    private void renderGlint(PoseStack p_289673_, MultiBufferSource p_289654_, int p_289649_, Model p_289659_) {
        p_289659_.renderToBuffer(p_289673_, p_289654_.getBuffer(RenderType.armorEntityGlint()), p_289649_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private A getArmorModel(EquipmentSlot pSlot) {
        return this.usesInnerModel(pSlot) ? this.innerModel : this.outerModel;
    }

    private boolean usesInnerModel(EquipmentSlot pSlot) {
        return pSlot == EquipmentSlot.LEGS;
    }

    protected Model getArmorModelHook(T entity, ItemStack itemStack, EquipmentSlot slot, A model) {
        return ForgeHooksClient.getArmorModel(entity, itemStack, slot, model);
    }

    public ResourceLocation getArmorResource(Entity entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
        ArmorItem item = (ArmorItem)stack.getItem();
        String texture = item.getMaterial().getName();
        String domain = "minecraft";
        int idx = texture.indexOf(58);
        if (idx != -1) {
            domain = texture.substring(0, idx);
            texture = texture.substring(idx + 1);
        }
        String s1 = String.format(Locale.ROOT, "%s:textures/models/armor/%s_layer_%d%s.png", domain, texture,
                this.usesInnerModel(slot) ? 2 : 1, type == null ? "" : String.format(Locale.ROOT, "_%s", type));
        s1 = ForgeHooksClient.getArmorTexture(entity, stack, s1, slot, type);
        ResourceLocation resourcelocation = ARMOR_LOCATION_CACHE.get(s1);
        if (resourcelocation == null) {
            resourcelocation = ResourceLocation.tryParse(s1);
            ARMOR_LOCATION_CACHE.put(s1, resourcelocation);
        }
        return resourcelocation;
    }
}