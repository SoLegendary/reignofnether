// Follows net.minecraft.client.renderer.entity.layers.CustomHeadLayer, with the mod's own
// CARVED_PUMPKIN head offset kept from 1.20.1 (vanilla dropped that special case, but RoN's
// Christmas-hat items rely on it).

package com.solegendary.reignofnether.unit.modelling.layers;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomUnitHeadLayer<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {
    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;
    private final Map<SkullBlock.Type, SkullModelBase> skullModels;
    private final ItemInHandRenderer itemInHandRenderer;

    public CustomUnitHeadLayer(RenderLayerParent<T, M> parent, EntityModelSet modelSet, ItemInHandRenderer itemInHandRenderer) {
        this(parent, modelSet, 1.0F, 1.0F, 1.0F, itemInHandRenderer);
    }

    public CustomUnitHeadLayer(RenderLayerParent<T, M> parent, EntityModelSet modelSet,
                               float scaleX, float scaleY, float scaleZ,
                               ItemInHandRenderer itemInHandRenderer) {
        super(parent);
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        this.skullModels = SkullBlockRenderer.createSkullRenderers(modelSet);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (stack.isEmpty()) return;

        Item item = stack.getItem();
        poseStack.pushPose();
        poseStack.scale(this.scaleX, this.scaleY, this.scaleZ);
        boolean isVillager = entity instanceof Villager || entity instanceof ZombieVillager;
        if (entity.isBaby() && !(entity instanceof Villager)) {
            poseStack.translate(0.0F, 0.03125F, 0.0F);
            poseStack.scale(0.7F, 0.7F, 0.7F);
            poseStack.translate(0.0F, 1.0F, 0.0F);
        }

        this.getParentModel().getHead().translateAndRotate(poseStack);

        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock) {
            poseStack.scale(1.1875F, -1.1875F, -1.1875F);
            if (isVillager) {
                poseStack.translate(0.0F, 0.0625F, 0.0F);
            }

            // 1.21.1 moved the skull owner from the stack's "SkullOwner" NBT to a ResolvableProfile
            // data component.
            ResolvableProfile profile = stack.get(DataComponents.PROFILE);
            poseStack.translate(-0.5, 0.0, -0.5);
            SkullBlock.Type type = ((AbstractSkullBlock) blockItem.getBlock()).getType();
            SkullModelBase model = this.skullModels.get(type);
            RenderType renderType = SkullBlockRenderer.getRenderType(type, profile);
            WalkAnimationState walkAnimation = entity.getVehicle() instanceof LivingEntity vehicle
                    ? vehicle.walkAnimation
                    : entity.walkAnimation;
            float bob = walkAnimation.position(partialTick);
            SkullBlockRenderer.renderSkull(null, 180.0F, bob, poseStack, buffer, packedLight, model, renderType);
        } else if (!(item instanceof ArmorItem armor) || armor.getEquipmentSlot() != EquipmentSlot.HEAD) {
            translateToHead(poseStack, isVillager, item);
            this.itemInHandRenderer.renderItem(entity, stack, ItemDisplayContext.HEAD, false, poseStack, buffer, packedLight);
        }

        poseStack.popPose();
    }

    public static void translateToHead(PoseStack poseStack, boolean isVillager, Item item) {
        poseStack.translate(0.0F, item == Items.CARVED_PUMPKIN ? -0.375F : -0.25F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.625F, -0.625F, -0.625F);
        if (isVillager) {
            poseStack.translate(0.0F, 0.1875F, 0.0F);
        }
    }
}
