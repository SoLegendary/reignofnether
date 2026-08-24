//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Iterator;
import java.util.List;

import com.solegendary.reignofnether.unit.units.villagers.ScoutCatUnit;
import com.solegendary.reignofnether.unit.units.villagers.ScoutDogUnit;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CatCollarLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CatUnitRenderer extends MobRenderer<Cat, CatModel<Cat>> {
    public CatUnitRenderer(EntityRendererProvider.Context p_173943_) {
        super(p_173943_, new CatModel(p_173943_.bakeLayer(ModelLayers.CAT)), 0.4F);
        this.addLayer(new CatCollarLayer(this, p_173943_.getModelSet()));
    }

    public ResourceLocation getTextureLocation(Cat pEntity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/jellie.png");
    }

    @Override
    protected void scale(Cat cat, PoseStack pPoseStack, float pPartialTickTime) {
        if (cat instanceof ScoutCatUnit catUnit) {
            pPoseStack.scale(0.999F, 0.999F, 0.999F);
            pPoseStack.translate(0.0F, 0.001F, 0.0F);
            float $$5 = Mth.lerp(pPartialTickTime, catUnit.oSquish, catUnit.squish) / (0.5F + 1.0F);
            float $$6 = 1.0F / ($$5 + 1.0F);
            pPoseStack.scale($$6, 1.0F / $$6, $$6);
        }
    }

    protected void setupRotations(Cat pEntityLiving, PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        super.setupRotations(pEntityLiving, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
        float $$5 = pEntityLiving.getLieDownAmount(pPartialTicks);
        if ($$5 > 0.0F) {
            pPoseStack.translate(0.4F * $$5, 0.15F * $$5, 0.1F * $$5);
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(Mth.rotLerp($$5, 0.0F, 90.0F)));
            BlockPos $$6 = pEntityLiving.blockPosition();
            List<Player> $$7 = pEntityLiving.level().getEntitiesOfClass(Player.class, (new AABB($$6)).inflate(2.0, 2.0, 2.0));
            Iterator var9 = $$7.iterator();

            while(var9.hasNext()) {
                Player $$8 = (Player)var9.next();
                if ($$8.isSleeping()) {
                    pPoseStack.translate(0.15F * $$5, 0.0F, 0.0F);
                    break;
                }
            }
        }
    }
}
