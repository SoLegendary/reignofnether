package com.solegendary.reignofnether.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import javax.annotation.Nullable;

// projects world positions onto the GUI, using the matrices of the last rendered frame
public class WorldToScreen {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Matrix4f projection = new Matrix4f();
    private static final Matrix4f view = new Matrix4f();
    private static Vec3 camera = Vec3.ZERO;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        projection.set(evt.getProjectionMatrix());
        view.set(evt.getPoseStack().last().pose());
        camera = evt.getCamera().getPosition();
    }

    // GUI-scaled screen position, or null when behind the camera
    @Nullable
    public static Vec2 project(Vec3 world) {
        Vector4f v = new Vector4f((float) (world.x - camera.x), (float) (world.y - camera.y), (float) (world.z - camera.z), 1);
        view.transform(v);
        projection.transform(v);
        if (v.w <= 0.01f)
            return null;
        float x = (v.x / v.w * 0.5f + 0.5f) * MC.getWindow().getGuiScaledWidth();
        float y = (1 - (v.y / v.w * 0.5f + 0.5f)) * MC.getWindow().getGuiScaledHeight();
        return new Vec2(x, y);
    }

    public static double distance(Vec3 world) {
        return camera.distanceTo(world);
    }
}
