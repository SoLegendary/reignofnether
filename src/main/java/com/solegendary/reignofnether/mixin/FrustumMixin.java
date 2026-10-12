package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class FrustumMixin {

    // I have no idea why this is needed but without it the game freezes and gets stuck inside
    // this function forever a few seconds after activating orthoView
    @Inject(
            method = "offsetToFullyIncludeCameraCube",
            at = @At("HEAD"),
            cancellable = true
    )
    private void offsetToFullyIncludeCameraCube(int p_194442_, CallbackInfoReturnable<Frustum> cir) {
        if (OrthoviewClientEvents.isEnabled()) {
            cir.setReturnValue((Frustum) (Object) this);
        }
    }

    /*
     * The beacon / end-portal exemption upstream carried in this class is not a loss in 1.21.1 -
     * NeoForge already implements it, so re-adding the injection here would be dead code:
     *
     * - Upstream needed it because in 1.20.1 those block entities answered
     *   IForgeBlockEntity.INFINITE_EXTENT_AABB with a world-sized box, and Forge's frustum test
     *   could reject an infinite box at some camera angles (GH-9321). That fix now lives inside
     *   NeoForge's own Frustum#isVisible ("exit early for infinite bounds, these would otherwise
     *   fail in the intersection test at certain camera angles"), and AABB.INFINITE still exists -
     *   it is still what StructureBlockRenderer#getRenderBoundingBox answers.
     * - The render box itself moved from IForgeBlockEntity#getRenderBoundingBox to
     *   BlockEntityRenderer#getRenderBoundingBox, which LevelRenderer reaches through
     *   ClientHooks#isBlockEntityRendererVisible(dispatcher, blockEntity, frustum).
     * - Nothing left needs an exemption: BeaconRenderer answers a box that runs from the beacon to
     *   the top of the world (y up to MAX_RENDER_Y) and TheEndPortalRenderer answers the renderer
     *   default of one block. Both are finite and culled correctly, so the ortho camera already
     *   shows every beacon beam and end portal it should.
     *
     * If one of them is ever seen culled from the ortho camera, the place to fix it is that
     * renderer's getRenderBoundingBox, not this class: by the time a box gets here it is finite and
     * vanilla's test is right.
     */
}
