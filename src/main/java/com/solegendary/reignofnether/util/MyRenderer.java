package com.solegendary.reignofnether.util;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.RectZone;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;
import java.util.OptionalDouble;

import static net.minecraft.client.renderer.RenderStateShard.COLOR_DEPTH_WRITE;
import static net.minecraft.client.renderer.RenderStateShard.ITEM_ENTITY_TARGET;
import static net.minecraft.client.renderer.RenderStateShard.NO_CULL;
import static net.minecraft.client.renderer.RenderStateShard.NO_DEPTH_TEST;
import static net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_LINES_SHADER;
import static net.minecraft.client.renderer.RenderStateShard.TRANSLUCENT_TRANSPARENCY;
import static net.minecraft.client.renderer.RenderStateShard.VIEW_OFFSET_Z_LAYERING;

public class MyRenderer {

    private static final Minecraft MC = Minecraft.getInstance();

    public static final RenderType LINES_NO_DEPTH_TEST = RenderType.create(
            "lines", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false,
            RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(false)
    );


    public static final Style iconStyle = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "resource_icons"));

    public static void drawBlockOutline(PoseStack matrixStack, BlockPos blockpos, float a) {
        AABB aabb = new AABB(blockpos).move(0, 0.01, 0);
        drawLineBox(matrixStack, aabb, 1.0f, 1.0f, 1.0f, a);
    }

    public static void drawEntityBox(PoseStack matrixStack, Entity entity, float a) {
        drawLineBox(matrixStack, entity.getBoundingBox(), 1.0f, 1.0f, 1.0f, a);
    }

    public static void drawEntityBox(PoseStack matrixStack, Entity entity, float r, float g, float b, float a) {
        drawLineBox(matrixStack, entity.getBoundingBox(), r, g, b, a);
    }

    // like drawEntityOutline but only the bottom square
    public static void drawBoxBottom(PoseStack matrixStack, AABB aabb, float r, float g, float b, float a) {
        aabb = aabb.setMaxY(aabb.minY);
        drawLineBox(matrixStack, aabb, r, g, b, a);
    }

    public static void drawLineBox(PoseStack matrixStack, AABB aabb, float r, float g, float b, float a) {
        Entity camEntity = MC.getCameraEntity();
        double d0 = camEntity.getX();
        double d1 = camEntity.getY() + camEntity.getEyeHeight();
        double d2 = camEntity.getZ();

        VertexConsumer vertexConsumer = MC.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        matrixStack.pushPose();
        matrixStack.translate(-d0, -d1, -d2); // because we start at 0,0,0 relative to camera
        LevelRenderer.renderLineBox(matrixStack, vertexConsumer, aabb, r, g, b, a);
        matrixStack.popPose();
    }

    // draws an AABB but only the lines required to outline an entity from the perspective of the player in orthoview
    public static void drawLineBoxOutlineOnly(
            PoseStack matrixStack,
            VertexConsumer vertexConsumer,
            AABB aabb,
            float r,
            float g,
            float b,
            float a,
            boolean excludeMaxY
    ) {
        Entity camEntity = MC.getCameraEntity();
        double d0 = camEntity.getX();
        double d1 = camEntity.getY() + camEntity.getEyeHeight();
        double d2 = camEntity.getZ();

        matrixStack.pushPose();
        matrixStack.translate(-d0, -d1, -d2); // because we start at 0,0,0 relative to camera

        Matrix4f matrix4f = matrixStack.last().pose();
        Matrix3f matrix3f = matrixStack.last().normal();
        float minX = (float) aabb.minX;
        float minY = (float) aabb.minY;
        float minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX;
        float maxY = (float) aabb.maxY;
        float maxZ = (float) aabb.maxZ;

        float rotX = OrthoviewClientEvents.getCamRotX();
        // convert angle to +-180deg (orthoView uses +- 360)
        if (rotX <= -180)
            rotX += 360;
        if (rotX > 180)
            rotX -= 360;

        // hide the lines that meet at the two points furthest and closest from the player

        if (rotX > -180 && rotX <= -90) {
            // closest: minX, maxY, maxZ
            // furthest: maxX, minY, minZ
            vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
            if (!excludeMaxY) {
                vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
                vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
            }
            vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            if (!excludeMaxY) {
                vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
                vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
            }
        } else if (rotX > 90 && rotX <= 180) {
            // closest: maxX, maxY, maxZ
            // furthest: minX, minY, minZ
            vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            if (!excludeMaxY) {
                vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
                vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
                vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
                vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
            }
            vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
        } else if (rotX > 0 && rotX <= 90) {
            // closest: maxX, maxY, minZ
            // furthest: minX, minY, maxZ
            vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            if (!excludeMaxY) {
                vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
                vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
            }
            vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
            if (!excludeMaxY) {
                vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
                vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            }
            vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
        } else if (rotX > -90 && rotX <= 0) {
            // closest: minX, maxY, minZ
            // furthest: maxX, minY, maxZ
            vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
            if (!excludeMaxY) {
                vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
                vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
                vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
                vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
            }
        }
        matrixStack.popPose();
    }

    // remember white.png can still be used with RGBA values
    public static void drawBox(
            PoseStack matrixStack,
            VertexConsumer vertexConsumer,
            BlockPos bp,
            float r,
            float g,
            float b,
            float a
    ) {
        AABB aabb = new AABB(bp);
        aabb = aabb.setMaxY(aabb.maxY + 0.01f);
        drawSolidBox(matrixStack, vertexConsumer, aabb, null, r, g, b, a, ResourceLocation.parse("forge:textures/white.png"));
    }

    public static void drawBlockFace(
            PoseStack matrixStack,
            VertexConsumer vertexConsumer,
            Direction dir,
            BlockPos bp,
            float r,
            float g,
            float b,
            float a
    ) {
        AABB aabb = new AABB(bp);
        aabb = aabb.setMaxY(aabb.maxY + 0.01f);
        drawSolidBox(matrixStack, vertexConsumer, aabb, dir, r, g, b, a, ResourceLocation.parse("forge:textures/white.png"));
    }

    public static void drawBlockFace(
            PoseStack matrixStack,
            VertexConsumer vertexConsumer,
            Direction dir,
            float yOffset,
            BlockPos bp,
            float r,
            float g,
            float b,
            float a
    ) {
        AABB aabb = new AABB(bp);
        aabb = aabb.setMaxY(aabb.maxY + 0.01f);
        drawSolidBox(matrixStack, vertexConsumer, aabb, dir, yOffset, r, g, b, a, ResourceLocation.parse("forge:textures/white.png"));
    }

    // might be null RL for black.png as of 1.19?
    public static void drawSolidBox(
            PoseStack matrixStack,
            VertexConsumer vertexConsumer0,
            AABB aabb,
            Direction dir,
            float r,
            float g,
            float b,
            float a,
            ResourceLocation rl
    ) {
        drawSolidBox(matrixStack, vertexConsumer0, aabb, dir, 0, r, g, b, a, rl);
    }

    public static void drawSolidBox(
            PoseStack matrixStack,
            VertexConsumer vertexConsumer0, //Generally, a VertexConsumer should be passed in, but this is currently not possible due to PoseStack conversions within this function. This may be fixed in the future.
            AABB aabb,
            Direction dir,
            float yOffset,
            float r,
            float g,
            float b,
            float a,
            ResourceLocation rl
    ) {
        Entity camEntity = MC.getCameraEntity();
        double d0 = camEntity.getX();
        double d1 = camEntity.getY() + camEntity.getEyeHeight();
        double d2 = camEntity.getZ();

        matrixStack.pushPose();
        matrixStack.translate(-d0, -d1, -d2); // because we start at 0,0,0 relative to camera
        Matrix4f matrix4f = matrixStack.last().pose();
        Matrix3f matrix3f = matrixStack.last().normal();

        float minX = (float) aabb.minX;
        float minY = (float) aabb.minY;
        float minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX;
        float maxY = (float) aabb.maxY + yOffset;
        float maxZ = (float) aabb.maxZ;

        // Note that error: 'not filled all elements of vertex' means the vertex needs more elements,
        // eg. ENTITY_TRANSLUCENT needs vertex(x,y,z).color(rgba).uv(0,0).overlayCoords(0,0).uv2(light).normal(x,y,z)
        // you can trace this all the way back to the DefaultVertexFormat class where these vertex elements are defined
        // normal is the vector perpendicular to the plane, if not used all quads will always be flat facing

        // uv() are the texture coordinates, if you dont use a texture, they can be (0, 0).
        // uv2() are the block and skylight (packed with LightTexture.pack() to one integer).
        // overlayCoords() refers to overlay effects:
        //      (0,10) is no overlay
        //      (0,0) is 'entity hurt', ie. the red overlaid when entities take damage

        // all vertices are in order: BR, TR, TL, BL

        int light = 255;

        VertexConsumer vertexConsumer = MC.renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(rl));

        // +y top face
        if (dir == null || dir == Direction.UP) {
            vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
        }
        // +x side face
        if (dir == null || dir == Direction.EAST) {
            vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
        }
        // +z side face
        if (dir == null || dir == Direction.SOUTH) {
            vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
        }
        // -x side face
        if (dir == null || dir == Direction.WEST) {
            vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
        }
        // -z side face
        if (dir == null || dir == Direction.NORTH) {
            vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
        }
        // -y bottom face
        if (dir == null || dir == Direction.DOWN) {
            vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
            vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
        }
        matrixStack.popPose();
    }

    public static void drawLine(
            PoseStack matrixStack,
            VertexConsumer vertexConsumer,
            BlockPos startPos,
            BlockPos endPos,
            float r,
            float g,
            float b,
            float a
    ) {
        drawLine(matrixStack,
                vertexConsumer,
                new Vec3(startPos.getX() + 0.5f,
                        startPos.getY() + 0.5f,
                        startPos.getZ() + 0.5f),
                new Vec3(endPos.getX() + 0.5f,
                        endPos.getY() + 1.0f,
                        endPos.getZ() + 0.5f),
                r, g, b, a);
    }

    // draws a coloured line from centre-top of startPos to centre-top of endPos
    public static void drawLine(
            PoseStack matrixStack,
            VertexConsumer vertexConsumer_,
            Vec3 startPos,
            Vec3 endPos,
            float r,
            float g,
            float b,
            float a
    ) {
        Entity camEntity = MC.getCameraEntity();
        if (camEntity == null)
            return;

        double d0 = camEntity.getX();
        double d1 = camEntity.getY() + camEntity.getEyeHeight();
        double d2 = camEntity.getZ();

        matrixStack.pushPose();
        matrixStack.translate(-d0, -d1, -d2); // because we start at 0,0,0 relative to camera
        Matrix4f matrix4f = matrixStack.last().pose();
        Matrix3f matrix3f = matrixStack.last().normal();

        VertexConsumer vertexConsumer = MC.renderBuffers().bufferSource().getBuffer(RenderType.LINES);

        // draw two lines on inverse normals so they're visible from any angle
        vertexConsumer.vertex(matrix4f, (float) startPos.x(), (float) startPos.y(), (float) startPos.z()).color(r, g, b, a).normal(matrix3f, 1.0f, 0, 0).endVertex();
        vertexConsumer.vertex(matrix4f, (float) endPos.x(), (float) endPos.y(), (float) endPos.z()).color(r, g, b, a).normal(matrix3f, 1.0f, 0, 0).endVertex();
        vertexConsumer.vertex(matrix4f, (float) startPos.x(), (float) startPos.y(), (float) startPos.z()).color(r, g, b, a).normal(matrix3f, 0, 0, 1.0f).endVertex();
        vertexConsumer.vertex(matrix4f, (float) endPos.x(), (float) endPos.y(), (float) endPos.z()).color(r, g, b, a).normal(matrix3f, 0, 0, 1.0f).endVertex();

        matrixStack.popPose();
    }

    // render a HUD frame and return the RectZone that it represents
    public static RectZone renderFrameWithBg(GuiGraphics guiGraphics, int x, int y, int width, int height, int bgCol) {
        // draw icon frame with dark transparent bg
        guiGraphics.fill(// x1,y1, x2,y2,
                x + 2, y + 2,
                x + width - 2,
                y + height - 2,
                bgCol); //ARGB(hex); note that alpha ranges between ~0-16 in RenderOverlayEvent, not 0-255

        // draw edges first so they aren't stretched on large dimensions
        int thickness = 4;

        ResourceLocation iconFrameResource;
        iconFrameResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/unit_frame_left" + (height < 50 ? "_small" : "") + ".png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        guiGraphics.blit(iconFrameResource,
                x, y, 0,
                0, 0, // where on texture to start drawing from
                thickness, height, // dimensions of blit texture
                thickness, height // size of texture itself (if < dimensions, texture is repeated)
        );
        iconFrameResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/unit_frame_right" + (height < 50 ? "_small" : "") + ".png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        guiGraphics.blit(iconFrameResource,
                x + width - thickness, y, 0,
                0, 0, // where on texture to start drawing from
                thickness, height, // dimensions of blit texture
                thickness, height // size of texture itself (if < dimensions, texture is repeated)
        );
        iconFrameResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/unit_frame_top.png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        guiGraphics.blit(iconFrameResource,
                x + thickness, y, 0,
                0, 0, // where on texture to start drawing from
                width - thickness * 2, thickness, // dimensions of blit texture
                width, thickness // size of texture itself (if < dimensions, texture is repeated)
        );
        iconFrameResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/unit_frame_bottom.png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        guiGraphics.blit(iconFrameResource,
                x + thickness, y + height - thickness, 0,
                0, 0, // where on texture to start drawing from
                width - thickness * 2, thickness, // dimensions of blit texture
                width, thickness // size of texture itself (if < dimensions, texture is repeated)
        );
        return RectZone.getZoneByLW(x, y, width, height);
    }

    public static RectZone renderIconFrameWithBg(GuiGraphics guiGraphics, ResourceLocation frameRl, int x, int y, int size, int bg) {
        //transparent background
        guiGraphics.fill(// x1,y1, x2,y2,
                x, y,
                x + size,
                y + size,
                bg); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255

        // icon frame
        RenderSystem.setShaderTexture(0, frameRl);
        guiGraphics.blit(frameRl,
                x, y, 0,
                0, 0, // where on texture to start drawing from
                size, size, // dimensions of blit texture
                size, size // size of texture itself (if < dimensions, texture is repeated)
        );
        return RectZone.getZoneByLW(x, y, size, size);
    }

    public static void renderIcon(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int x, int y, int size) {
        RenderSystem.setShaderTexture(0, resourceLocation);
        guiGraphics.blit(resourceLocation,
                x, y, 0,
                0, 0, // where on texture to start drawing from
                size, size, // dimensions of blit texture
                size, size // size of texture itself (if < dimensions, texture is repeated)
        );
    }

    public static void renderTooltip(GuiGraphics guiGraphics, List<FormattedCharSequence> tooltipLines, int mouseX, int mouseY) {
        if (!OrthoviewClientEvents.isEnabled())
            return;
        if (MC.screen != null && tooltipLines != null && tooltipLines.size() > 0) {
            if (mouseY < MC.screen.height / 2)
                mouseY += (tooltipLines.size() * 10);
            guiGraphics.pose().translate(0, 0, 3000);
            guiGraphics.renderTooltip(MC.font, tooltipLines, mouseX, mouseY - (9 * (tooltipLines.size() - 1)));
            guiGraphics.pose().translate(0, 0, -3000);
        }
    }

    public static void renderItemInFrontOfEntityFace(PoseStack poseStack, LivingEntity entity, float partialTicks, ItemStack itemStack) {
        ItemRenderer itemRenderer = MC.getItemRenderer();
        poseStack.pushPose();

        // Get current rendering position
        Camera camera = MC.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vec3 eyePos = entity.getEyePosition();

        float f = entity.getXRot() * 0.017453292F;
        float f1 = -entity.getYHeadRot() * 0.017453292F;
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        Vec3 look = new Vec3(f3 * f4, -f5, f2 * f4).normalize();

        Vec3 itemPos = eyePos.add(look.scale(0.5f));

        poseStack.translate(itemPos.x - camPos.x, itemPos.y - camPos.y - 0.25f, itemPos.z - camPos.z);

        float yaw = Mth.lerp(partialTicks, entity.yHeadRotO, entity.getYHeadRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        itemRenderer.renderStatic(
                itemStack,
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                MC.renderBuffers().bufferSource(),
                MC.level,
                0 // random seed
        );
        poseStack.popPose();

        RandomSource random = MC.level.getRandom();
        double dx = (random.nextDouble() - 0.5) * 0.1;
        double dy = random.nextDouble() * 0.1 + 0.05;
        double dz = (random.nextDouble() - 0.5) * 0.1;

        MC.particleEngine.createParticle(
                new ItemParticleOption(ParticleTypes.ITEM, itemStack),
                itemPos.x, itemPos.y - 0.1f, itemPos.z, dx, dy, dz
        );
    }

    public static void renderItem(GuiGraphics guiGraphics, ItemStack pStack, int pX, int pY, float scale) {
        if (!pStack.isEmpty()) {
            BakedModel bakedmodel = MC.getItemRenderer().getModel(pStack, null, null, 0);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(pX + (9 * scale), pY + (9 * scale), (float)(150));
            try {
                guiGraphics.pose().mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
                guiGraphics.pose().scale(16.0F * scale, 16.0F * scale, 16.0F * scale);
                boolean flag = !bakedmodel.usesBlockLight();
                if (flag) {
                    Lighting.setupForFlatItems();
                }
                MC.getItemRenderer().render(pStack, ItemDisplayContext.GUI, false, guiGraphics.pose(), guiGraphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedmodel);
                guiGraphics.flush();
                if (flag) {
                    Lighting.setupFor3DItems();
                }
            } catch (Throwable var12) {
                CrashReport crashreport = CrashReport.forThrowable(var12, "Rendering item");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Item being rendered");
                crashreportcategory.setDetail("Item Type", () -> String.valueOf(pStack.getItem()));
                crashreportcategory.setDetail("Registry Name", () -> String.valueOf(ForgeRegistries.ITEMS.getKey(pStack.getItem())));
                crashreportcategory.setDetail("Item Damage", () -> String.valueOf(pStack.getDamageValue()));
                crashreportcategory.setDetail("Item NBT", () -> String.valueOf(pStack.getTag()));
                crashreportcategory.setDetail("Item Foil", () -> String.valueOf(pStack.hasFoil()));
                throw new ReportedException(crashreport);
            }
            guiGraphics.pose().popPose();
        }
    }
}
