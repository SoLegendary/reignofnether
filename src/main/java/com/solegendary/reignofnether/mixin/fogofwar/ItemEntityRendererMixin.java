package com.solegendary.reignofnether.mixin.fogofwar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.resources.ResourceSources;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// brightness shading for blocks excluding liquids and flat flace blocks (like tall grass)

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    @Final @Shadow private ItemRenderer itemRenderer;
    @Final @Shadow private RandomSource random = RandomSource.create();
    @Shadow protected int getRenderAmount(ItemStack pStack) { return 0; }
    
    private static final List<Item> enlargedItems = List.of(
        Items.GOLDEN_CHESTPLATE,
        Items.GOLDEN_LEGGINGS,
        Items.GOLDEN_BOOTS,
        Items.GOLDEN_HELMET,
        Items.NETHERITE_CHESTPLATE,
        Items.NETHERITE_LEGGINGS,
        Items.NETHERITE_BOOTS,
        Items.NETHERITE_HELMET,
        Items.NETHERITE_SWORD,
        Items.TRIDENT
    );

    @Unique
    private boolean reignofnether$shouldRenderLargeItemEntity(ItemEntity itemEntity) {
        Item item = itemEntity.getItem().getItem();
        if (!OrthoviewClientEvents.isEnabled())
            return false;
        return ResourceSources.isPreparedFood(item) ||
                ResourceSources.getFromItem(item) != null ||
                enlargedItems.contains(item);
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void render(ItemEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, CallbackInfo ci) {
        boolean shouldRender = false;
        BlockPos bp = pEntity.getOnPos();

        if (FogOfWarClientEvents.isInBrightChunk(pEntity) &&
            pEntity.level().getWorldBorder().isWithinBounds(bp))
            shouldRender = true;

        if (!shouldRender) {
            ci.cancel();
            return;
        }

        // enlarge and brighten items that units can use
        if (reignofnether$shouldRenderLargeItemEntity(pEntity)) {
            ci.cancel();

            pPoseStack.pushPose();
            pPoseStack.scale(2,2,2);
            ItemStack itemstack = pEntity.getItem();
            int i = itemstack.isEmpty() ? 187 : Item.getId(itemstack.getItem()) + itemstack.getDamageValue();
            this.random.setSeed(i);
            BakedModel bakedmodel = this.itemRenderer.getModel(itemstack, pEntity.level(), null, pEntity.getId());
            boolean flag = bakedmodel.isGui3d();
            int j = this.getRenderAmount(itemstack);
            float f = 0.25F;
            float f1 = Mth.sin(((float)pEntity.getAge() + pPartialTicks) / 10.0F + pEntity.bobOffs) * 0.1F + 0.1F;
            float f2 = bakedmodel.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
            pPoseStack.translate(0.0F, f1 + 0.25F * f2, 0.0F);
            float f3 = pEntity.getSpin(pPartialTicks);
            pPoseStack.mulPose(Axis.YP.rotation(f3));
            float f11;
            float f13;
            if (!flag) {
                float f7 = -0.0F * (float)(j - 1) * 0.5F;
                f11 = -0.0F * (float)(j - 1) * 0.5F;
                f13 = -0.09375F * (float)(j - 1) * 0.5F;
                pPoseStack.translate(f7, f11, f13);
            }

            for(int k = 0; k < j; ++k) {
                pPoseStack.pushPose();
                if (k > 0) {
                    if (flag) {
                        f11 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                        f13 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                        float f10 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                        pPoseStack.translate(f11, f13, f10);
                    } else {
                        f11 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                        f13 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                        pPoseStack.translate(f11, f13, 0.0);
                    }
                }
                this.itemRenderer.render(itemstack, ItemDisplayContext.GROUND, false, pPoseStack,
                        pBuffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, bakedmodel);
                pPoseStack.popPose();
                if (!flag) {
                    pPoseStack.translate(0.0, 0.0, 0.09375);
                }
            }
            pPoseStack.popPose();
        }
    }
}
