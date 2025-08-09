//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Slime;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MagmaCubeUnitRenderer extends MobRenderer<Slime, LavaSlimeModel<Slime>> {
    private static final ResourceLocation MAGMACUBE_LOCATION = ResourceLocation.parse("textures/entity/slime/magmacube.png");

    public MagmaCubeUnitRenderer(EntityRendererProvider.Context p_174391_) {
        super(p_174391_, new LavaSlimeModel<>(p_174391_.bakeLayer(ModelLayers.MAGMA_CUBE)), 0.25F);
    }

    public void render(Slime pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        this.shadowRadius = 0.25F * (float)pEntity.getSize();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    protected void scale(Slime pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
        float $$3 = 0.999F;
        pMatrixStack.scale(0.999F, 0.999F, 0.999F);
        pMatrixStack.translate(0.0, 0.0010000000474974513, 0.0);
        float $$4 = (float)pLivingEntity.getSize();
        float $$5 = Mth.lerp(pPartialTickTime, pLivingEntity.oSquish, pLivingEntity.squish) / ($$4 * 0.5F + 1.0F);
        float $$6 = 1.0F / ($$5 + 1.0F);
        pMatrixStack.scale($$6 * $$4, 1.0F / $$6 * $$4, $$6 * $$4);
    }

    public ResourceLocation getTextureLocation(Slime pEntity) {
        return MAGMACUBE_LOCATION;
    }
}
