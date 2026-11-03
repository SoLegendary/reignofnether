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
    private static final float WALK_SPEED = 0.6662f * 0.75f; // vanilla walk cycle (0.6662 rad per tick at full limb swing) - 25%
    private static final float WALK_AMPLITUDE = 0.4f;        // vanilla full-speed leg swing
    private static final float HIND_AMPLITUDE_MULT = 0.35f;  // hind legs move much less than front legs while digging
    private static final float HEAD_DIP = 0.4f;              // radians, head pitched down while digging
    private static final float HEAD_DIP_RAMP = 2.5f;         // higher = head reaches full dip sooner

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

        if (!(pEntity instanceof RavagerUnit unit) || !unit.isSiegeLocked()) {
            // the model is shared by every ravager and vanilla never resets zRot, so clear any splay left by a sieged one
            rightFrontLeg.zRot = 0;
            rightHindLeg.zRot = 0;
            leftFrontLeg.zRot = 0;
            leftHindLeg.zRot = 0;
            return;
        }

        float partialTick = pAgeInTicks - pEntity.tickCount;
        float progress = unit.getSiegeProgress(partialTick);   // 0 surfaced -> 1 sunk
        float dig = Mth.sin(progress * Mth.PI);                // 0 at both ends, peaks mid-transition
        float t = pAgeInTicks * WALK_SPEED;

        // same leg cycle as vanilla RavagerModel's walk, eased in/out via 'dig'
        float cycle = Mth.cos(t) * WALK_AMPLITUDE;
        float hindCycle = cycle * HIND_AMPLITUDE_MULT;
        rightHindLeg.xRot  = Mth.lerp(dig, rightHindLeg.xRot,   hindCycle);
        leftHindLeg.xRot   = Mth.lerp(dig, leftHindLeg.xRot,   -hindCycle);
        rightFrontLeg.xRot = Mth.lerp(dig, rightFrontLeg.xRot, -cycle);
        leftFrontLeg.xRot  = Mth.lerp(dig, leftFrontLeg.xRot,   cycle);

        // head dips toward the ground while digging: snaps down quickly (linear ramp, not eased), holds, then lifts
        head.xRot += HEAD_DIP * Math.min(1f, dig * HEAD_DIP_RAMP);

        // braced stance once down: legs splay outward and stay that way while sieged
        // (positive zRot moves the foot toward -x, so right = +, left = -)
        rightFrontLeg.zRot =  SPLAY * progress;
        rightHindLeg.zRot  =  SPLAY * progress;
        leftFrontLeg.zRot  = -SPLAY * progress;
        leftHindLeg.zRot   = -SPLAY * progress;
    }
}