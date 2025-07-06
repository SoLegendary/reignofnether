package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.modelling.models.VillagerUnitModel;
import com.solegendary.reignofnether.unit.modelling.models.VillagerUnitProfessionLayer;
import com.solegendary.reignofnether.unit.units.villagers.MilitiaUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// based on VindicatorRenderer
@OnlyIn(Dist.CLIENT)
public class VillagerUnitRenderer extends AbstractVillagerUnitRenderer<AbstractIllager> {
    private static final ResourceLocation VILLAGER_UNIT = new ResourceLocation("reignofnether", "textures/entities/villager_unit.png");

    public VillagerUnitRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerUnitModel<>(context.bakeLayer(VillagerUnitModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new ItemInHandLayer<AbstractIllager, VillagerUnitModel<AbstractIllager>>(this, context.getItemInHandRenderer()) {
            public void render(PoseStack pose, MultiBufferSource mbs, int p_116354_, AbstractIllager unit, float p_116356_, float p_116357_, float p_116358_, float p_116359_, float p_116360_, float p_116361_) {
                if (unit instanceof VillagerUnit vUnit &&
                        ((vUnit.getBuildRepairGoal() != null && vUnit.getBuildRepairGoal().isBuilding()) ||
                        (vUnit.getGatherResourceGoal() != null && vUnit.getGatherResourceGoal().isGathering()) ||
                        (vUnit.getTargetGoal() != null && vUnit.getTargetGoal().getTarget() != null))) {
                    super.render(pose, mbs, p_116354_, unit, p_116356_, p_116357_, p_116358_, p_116359_, p_116360_, p_116361_);
                } else if (unit instanceof MilitiaUnit mUnit &&
                        ((mUnit.getTargetGoal() != null && mUnit.getTargetGoal().getTarget() != null) ||
                        (mUnit.getAttackBuildingGoal() instanceof MeleeAttackBuildingGoal mabg &&
                        mabg.getBuildingTarget() != null))) {
                    super.render(pose, mbs, p_116354_, unit, p_116356_, p_116357_, p_116358_, p_116359_, p_116360_, p_116361_);
                }
            }
        });
        this.addLayer(new VillagerUnitProfessionLayer(this, context.getResourceManager(), "villager"));
    }

    public ResourceLocation getTextureLocation(AbstractIllager p_116324_) {
        return VILLAGER_UNIT;
    }
}