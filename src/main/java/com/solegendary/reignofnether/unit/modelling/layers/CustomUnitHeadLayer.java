//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.modelling.layers;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// based on CustomHeadLayer

@OnlyIn(Dist.CLIENT)
public class CustomUnitHeadLayer<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {
    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;
    private final Map<SkullBlock.Type, SkullModelBase> skullModels;
    private final ItemInHandRenderer itemInHandRenderer;

    public CustomUnitHeadLayer(RenderLayerParent<T, M> pRenderer, EntityModelSet pModelSet, ItemInHandRenderer pItemInHandRenderer) {
        this(pRenderer, pModelSet, 1.0F, 1.0F, 1.0F, pItemInHandRenderer);
    }

    public CustomUnitHeadLayer(RenderLayerParent<T, M> pRenderer, EntityModelSet pModelSet, float pScaleX, float pScaleY, float pScaleZ, ItemInHandRenderer pItemInHandRenderer) {
        super(pRenderer);
        this.scaleX = pScaleX;
        this.scaleY = pScaleY;
        this.scaleZ = pScaleZ;
        this.skullModels = SkullBlockRenderer.createSkullRenderers(pModelSet);
        this.itemInHandRenderer = pItemInHandRenderer;
    }

    public void render(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, T pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        ItemStack $$10 = pLivingEntity.getItemBySlot(EquipmentSlot.HEAD);
        if (!$$10.isEmpty()) {
            Item $$11 = $$10.getItem();
            pPoseStack.pushPose();
            pPoseStack.scale(this.scaleX, this.scaleY, this.scaleZ);
            boolean $$12 = pLivingEntity instanceof Villager || pLivingEntity instanceof ZombieVillager;
            if (pLivingEntity.isBaby() && !(pLivingEntity instanceof Villager)) {
                pPoseStack.translate(0.0F, 0.03125F, 0.0F);
                pPoseStack.scale(0.7F, 0.7F, 0.7F);
                pPoseStack.translate(0.0F, 1.0F, 0.0F);
            }

            ((HeadedModel)this.getParentModel()).getHead().translateAndRotate(pPoseStack);
            if ($$11 instanceof BlockItem && ((BlockItem)$$11).getBlock() instanceof AbstractSkullBlock) {
                pPoseStack.scale(1.1875F, -1.1875F, -1.1875F);
                if ($$12) {
                    pPoseStack.translate(0.0F, 0.0625F, 0.0F);
                }

                GameProfile $$16 = null;
                if ($$10.hasTag()) {
                    CompoundTag $$17 = $$10.getTag();
                    if ($$17.contains("SkullOwner", 10)) {
                        $$16 = NbtUtils.readGameProfile($$17.getCompound("SkullOwner"));
                    }
                }

                pPoseStack.translate(-0.5, 0.0, -0.5);
                SkullBlock.Type $$18 = ((AbstractSkullBlock)((BlockItem)$$11).getBlock()).getType();
                SkullModelBase $$19 = this.skullModels.get($$18);
                RenderType $$20 = SkullBlockRenderer.getRenderType($$18, $$16);
                Entity var22 = pLivingEntity.getVehicle();
                WalkAnimationState $$23;
                if (var22 instanceof LivingEntity) {
                    LivingEntity $$21 = (LivingEntity)var22;
                    $$23 = $$21.walkAnimation;
                } else {
                    $$23 = pLivingEntity.walkAnimation;
                }

                float $$24 = $$23.position(pPartialTicks);
                SkullBlockRenderer.renderSkull(null, 180.0F, $$24, pPoseStack, pBuffer, pPackedLight, $$19, $$20);
            } else {
                label60: {
                    if ($$11 instanceof ArmorItem) {
                        ArmorItem $$25 = (ArmorItem)$$11;
                        if ($$25.getEquipmentSlot() == EquipmentSlot.HEAD) {
                            break label60;
                        }
                    }
                    translateToHead(pPoseStack, $$12, $$11);
                    this.itemInHandRenderer.renderItem(pLivingEntity, $$10, ItemDisplayContext.HEAD, false, pPoseStack, pBuffer, pPackedLight);
                }
            }

            pPoseStack.popPose();
        }
    }

    public static void translateToHead(PoseStack pPoseStack, boolean pIsVillager, Item item) {
        pPoseStack.translate(0.0F, item == Items.CARVED_PUMPKIN ? -0.375F : -0.25F, 0.0F);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        pPoseStack.scale(0.625F, -0.625F, -0.625F);
        if (pIsVillager) {
            pPoseStack.translate(0.0F, 0.1875F, 0.0F);
        }

    }
}
