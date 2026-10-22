package com.solegendary.reignofnether.unit.modelling.models;

import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import net.minecraft.client.model.RavagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RavagerUnitModel extends RavagerModel {
    private static final float SPLAY = 0.22f;       // braced stance, radians outward
    private static final float DIG_SPEED = 0.8f;    // radians per tick, ~5 scrapes over 40 ticks

    private final ModelPart head;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public RavagerUnitModel(ModelPart root) {
        super(root);
        this.head = root.getChild("neck").getChild("head");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    @Override
    public void setupAnim(Ravager pEntity, float pLimbSwing, float pLimbSwingAmount,
                          float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        super.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);

        if (!(pEntity instanceof RavagerUnit unit) || !unit.isSiegeLocked())
            return;

        float partialTick = pAgeInTicks - pEntity.tickCount;
        float progress = unit.getSiegeProgress(partialTick);   // 0 surfaced -> 1 sunk
        float dig = Mth.sin(progress * Mth.PI);                // 0 at both ends, peaks mid-transition
        float t = pAgeInTicks * DIG_SPEED;

        // front legs: alternate raising forward then scraping back (negative xRot = foot forward)
        float frontR = -0.4f + 0.7f * Mth.sin(t);
        float frontL = -0.4f + 0.7f * Mth.sin(t + Mth.PI);
        rightFrontLeg.xRot = Mth.lerp(dig, rightFrontLeg.xRot, frontR);
        leftFrontLeg.xRot  = Mth.lerp(dig, leftFrontLeg.xRot,  frontL);

        // hind legs: smaller, offset kicks pushing dirt back
        float hindR = 0.3f * Mth.sin(t + Mth.HALF_PI);
        float hindL = 0.3f * Mth.sin(t + Mth.HALF_PI + Mth.PI);
        rightHindLeg.xRot = Mth.lerp(dig, rightHindLeg.xRot, hindR);
        leftHindLeg.xRot  = Mth.lerp(dig, leftHindLeg.xRot,  hindL);

        // braced stance once down: legs splay outward and stay that way while sieged
        // (positive zRot moves the foot toward -x, so right = +, left = -)
        rightFrontLeg.zRot =  SPLAY * progress;
        rightHindLeg.zRot  =  SPLAY * progress;
        leftFrontLeg.zRot  = -SPLAY * progress;
        leftHindLeg.zRot   = -SPLAY * progress;

        // head nods into the ground while digging
        head.xRot += 0.35f * dig * Mth.sin(t);
    }
}