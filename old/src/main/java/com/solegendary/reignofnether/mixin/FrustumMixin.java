package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.building.buildings.placements.EndPortalPlacement;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
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

    // see IForgeBlockEntity.getRenderBoundingBox()
    @Inject(
            method = "isVisible(Lnet/minecraft/world/phys/AABB;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    public void isVisible(AABB aabb, CallbackInfoReturnable<Boolean> cir) {
        // aabb is infinite only for some block entities: structure, beacon, end portal
        boolean infAABB = aabb.equals(IForgeBlockEntity.INFINITE_EXTENT_AABB);

        Player player = Minecraft.getInstance().player;
        float zoom = Math.max(30, OrthoviewClientEvents.getZoom()) * 2;

        if (player != null && OrthoviewClientEvents.isEnabled() && infAABB) {
            for (BuildingPlacement building : BuildingClientEvents.getBuildings()) {
                if (building instanceof BeaconPlacement ||
                    building instanceof EndPortalPlacement) {
                    BlockPos equalYBp = new BlockPos(player.getOnPos().getX(), building.centrePos.getY(), player.getOnPos().getZ());
                    if (building.centrePos.distSqr(equalYBp) < (zoom * zoom))
                        cir.setReturnValue(true);
                }
            }
        }
    }
}