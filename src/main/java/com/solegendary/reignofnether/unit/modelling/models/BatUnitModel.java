//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.modelling.models;

import com.solegendary.reignofnether.unit.units.monsters.BatUnit;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.definitions.BatAnimation;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * The model the bat unit is drawn with: vanilla's own bat mesh (the renderer bakes
 * {@code ModelLayers.BAT}) animated with vanilla's flying keyframes.
 *
 * <p>It deliberately carries no mesh of its own. The 64x64 decompiled mesh that used to live here in
 * {@code createBodyLayer()} is the 1.20.1 bat layout, while 1.21.1 redrew the bat for a 32x32
 * {@code textures/entity/bat.png} - baking that mesh against the current texture garbles every face.
 *
 * <p>{@code BatUnit} is not a {@code Bat}, so it cannot carry vanilla's fly/rest animation states; the
 * flying keyframes are driven from the render tick instead.
 */
@OnlyIn(Dist.CLIENT)
public class BatUnitModel extends HierarchicalModel<BatUnit> {
    private static final Vector3f ANIMATION_CACHE = new Vector3f();

    private final ModelPart root;
    private final ModelPart head;

    public BatUnitModel(ModelPart pRoot) {
        this.root = pRoot;
        this.head = pRoot.getChild("head");
    }

    public ModelPart root() {
        return this.root;
    }

    /**
     * Replaces the 1.20.1-era hand-written animation, which moved the parts themselves
     * ({@code head.setPos(0,0,0)} and both wings into the body's centre) as 1.20.1's mesh expected.
     * On the 1.21.1 mesh those calls drop the head at the bat's feet and fold the wings together, so
     * the unit rendered mangled; 1.21.1 also animates the bat with keyframes, which is what replaces
     * those rotations. {@code resetPose()} first, so the result does not depend on the previous frame.
     */
    public void setupAnim(BatUnit pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        // Head yaw first, then the flight keyframes: ROTATION and POSITION channels are additive
        // (AnimationChannel.Targets -> ModelPart#offsetRotation/#offsetPos), so the head keeps looking
        // where the unit does while the flight bob is layered on top - vanilla's order in BatModel.
        this.head.yRot = pNetHeadYaw * (float) (Math.PI / 180.0);
        KeyframeAnimations.animate(this, BatAnimation.BAT_FLYING, (long) (pAgeInTicks * 50.0F), 1.0F, ANIMATION_CACHE);
    }
}
