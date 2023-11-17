package com.solegendary.reignofnether.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;


// prevent syncing time from serverside under some conditions

@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    // Vec3s representing the points of a 3d rectangle - used to hide leaves around the cursor
    private static final int UNIT_WINDOW_RADIUS = 5; // size of area to hide leaves

    @Inject(
            method = "renderBatched*",
            at = @At("HEAD"),
            cancellable = true
    )
    private void renderBatched(BlockState bs, BlockPos bp, BlockAndTintGetter batg, PoseStack poseStack,
                 VertexConsumer vertexConsumer, boolean unknownBool, RandomSource randomSource,
                 ModelData modelData, RenderType renderType, boolean queryModelSpecificData, CallbackInfo ci) {

        Minecraft MC = Minecraft.getInstance();
        if (OrthoviewClientEvents.isEnabled() &&
            OrthoviewClientEvents.hideLeaves &&
            bs.getBlock() instanceof LeavesBlock &&
            MC.player != null &&
            bp.distSqr(MC.player.getOnPos()) < 1600) {

            Vec3 bpvec3 = new Vec3(bp.getX() + 0.5f, bp.getY() + 0.5f, bp.getZ() + 0.5f);

            for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                ArrayList<Vec3> vecs = MyMath.prepIsPointInsideRect3d(Minecraft.getInstance(),
                        new Vector3d(entity.getX() - UNIT_WINDOW_RADIUS, entity.getY(), entity.getZ() - UNIT_WINDOW_RADIUS), // tl
                        new Vector3d(entity.getX() - UNIT_WINDOW_RADIUS, entity.getY(), entity.getZ() + UNIT_WINDOW_RADIUS), // bl
                        new Vector3d(entity.getX() + UNIT_WINDOW_RADIUS, entity.getY(), entity.getZ() + UNIT_WINDOW_RADIUS)  // br
                );
                if (MyMath.isPointInsideRect3d(vecs, bpvec3)) {
                    ci.cancel();
                    return;
                }
            }
        }

    }
}