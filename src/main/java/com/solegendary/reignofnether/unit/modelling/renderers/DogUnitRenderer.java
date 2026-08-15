//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.unit.units.monsters.SlimeUnit;
import com.solegendary.reignofnether.unit.units.villagers.ScoutDogUnit;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.WolfCollarLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DogUnitRenderer extends MobRenderer<Wolf, WolfModel<Wolf>> {

    private static final ResourceLocation WOLF_ASHEN_TAME_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/wolf/wolf_ashen_tame.png");
    private static final ResourceLocation WOLF_BLACK_TAME_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/wolf/wolf_black_tame.png");
    private static final ResourceLocation WOLF_CHESTNUT_TAME_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/wolf/wolf_chestnut_tame.png");
    private static final ResourceLocation WOLF_RUSTY_TAME_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/wolf/wolf_rusty_tame.png");
    private static final ResourceLocation WOLF_SNOWY_TAME_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/wolf/wolf_snowy_tame.png");
    private static final ResourceLocation WOLF_SPOTTED_TAME_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/wolf/wolf_spotted_tame.png");
    private static final ResourceLocation WOLF_STRIPED_TAME_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/wolf/wolf_striped_tame.png");
    private static final ResourceLocation WOLF_WOODS_TAME_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/wolf/wolf_woods_tame.png");

    public DogUnitRenderer(EntityRendererProvider.Context p_174452_) {
        super(p_174452_, new WolfModel<>(p_174452_.bakeLayer(ModelLayers.WOLF)), 0.5F);
        this.addLayer(new WolfCollarLayer(this));
    }

    protected float getBob(Wolf pLivingBase, float pPartialTicks) {
        return pLivingBase.getTailAngle();
    }

    public void render(Wolf pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        if (pEntity.isWet()) {
            float $$6 = pEntity.getWetShade(pPartialTicks);
            this.model.setColor($$6, $$6, $$6);
        }

        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
        if (pEntity.isWet()) {
            this.model.setColor(1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    protected void scale(Wolf wolf, PoseStack pPoseStack, float pPartialTickTime) {
        if (wolf instanceof ScoutDogUnit dogUnit) {
            pPoseStack.scale(0.999F, 0.999F, 0.999F);
            pPoseStack.translate(0.0F, 0.001F, 0.0F);
            float $$5 = Mth.lerp(pPartialTickTime, dogUnit.oSquish, dogUnit.squish) / (0.5F + 1.0F);
            float $$6 = 1.0F / ($$5 + 1.0F);
            pPoseStack.scale($$6, 1.0F / $$6, $$6);
        }
        else
            super.scale(wolf, pPoseStack, pPartialTickTime);
    }

    private ResourceLocation getDogTexture(ScoutDogUnit scoutDogUnit) {
        return WOLF_WOODS_TAME_LOCATION;
    }

    public ResourceLocation getTextureLocation(Wolf pEntity) {
        if (pEntity instanceof ScoutDogUnit scoutDogUnit) {
            return getDogTexture(scoutDogUnit);
        } else {
            return new ResourceLocation("textures/entity/wolf/wolf_tame.png");
        }
    }
}
