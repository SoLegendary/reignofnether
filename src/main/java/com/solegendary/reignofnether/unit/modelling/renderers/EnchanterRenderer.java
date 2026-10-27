package com.solegendary.reignofnether.unit.modelling.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.unit.modelling.models.EnchanterModel;
import com.solegendary.reignofnether.unit.units.villagers.EnchanterUnit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class EnchanterRenderer extends MobRenderer<EnchanterUnit, EnchanterModel<EnchanterUnit>> {

    public static final float SCALE_MULT = 1.25f;

    private static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/enchanter_unit.png");

    // vanilla beacon beam texture (or point this at your own texture)
    private static final ResourceLocation BEAM_TEXTURE = BeaconRenderer.BEAM_LOCATION;

    // purple
    private static final float[] BEAM_COLOR = {0.725f, 0.300f, 1.00f};
    private static final float[] GLOW_COLOR = {0.90f, 0.600f, 1.00f};
    private static final float INNER_ALPHA = 0.75f;  // "slightly translucent" core
    private static final float GLOW_ALPHA = 0.25f;  // vanilla glow alpha
    // vanilla uses 0.2 / 0.25; these are thinner, which suits a unit-to-unit beam
    private static final float BEAM_RADIUS = 0.1f;
    private static final float GLOW_RADIUS = 0.15f;

    public EnchanterRenderer(EntityRendererProvider.Context context) {
        super(context, new EnchanterModel<>(context.bakeLayer(EnchanterModel.LAYER_LOCATION)), 0.5F);
    }

    public EnchanterRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation mll) {
        super(pContext, new EnchanterModel<>(pContext.bakeLayer(mll)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EnchanterUnit enchanterUnit) {
        return TEXTURE_LOCATION;
    }

    protected void scale(@NotNull EnchanterUnit pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
        float bonusScale = pLivingEntity.getScale();
        pMatrixStack.scale(SCALE_MULT * bonusScale, SCALE_MULT * bonusScale, SCALE_MULT * bonusScale);
    }

    @Override
    public void render(@NotNull EnchanterUnit enchanter, float yaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        super.render(enchanter, yaw, partialTicks, poseStack, buffer, packedLight);
        synchronized (enchanter.getTetheredEntities()) {
            for (LivingEntity entity : enchanter.getTetheredEntities()) {
                float alpha = enchanter.getBeamAlpha(entity);
                renderEnchantBeam(enchanter, entity, partialTicks, poseStack, buffer, alpha);
            }
        }
    }

    // Without this, the beam disappears when the enchanter's own bounding box leaves the screen
    @Override
    public boolean shouldRender(@NotNull EnchanterUnit enchanter, @NotNull Frustum frustum,
                                double camX, double camY, double camZ) {
        if (!enchanter.getTetheredEntities().isEmpty()) return true;
        return super.shouldRender(enchanter, frustum, camX, camY, camZ);
    }

    public static void renderEnchantBeam(EnchanterUnit source, LivingEntity target, float partialTick,
                                         PoseStack poseStack, MultiBufferSource buffer, float alpha) {
        if (!target.isAlive()) return;
        if (!source.renderTether) return;

        float time = source.tickCount + partialTick;

        Vec3 entityPos = source.getPosition(partialTick);
        // origin roughly at the enchanter's hands/chest; tweak the offset to taste
        Vec3 originWorld = source.getEyePosition(partialTick).subtract(0, source.getEyeHeight() * 0.3, 0);
        Vec3 targetWorld = target.getEyePosition(partialTick).subtract(0, source.getEyeHeight() * 0.3, 0);;

        Vec3 dir = targetWorld.subtract(originWorld);
        float dx = (float) dir.x, dy = (float) dir.y, dz = (float) dir.z;
        float dist = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.0001f) return;

        // poseStack is already at the entity's feet after super.render()
        Vec3 originLocal = originWorld.subtract(entityPos);

        poseStack.pushPose();
        poseStack.translate(originLocal.x, originLocal.y, originLocal.z);

        // Rotate so local +Y points at the target
        float yawRad = (float) Math.atan2(dx, dz);
        float pitchRad = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        poseStack.mulPose(Axis.YP.rotationDegrees(yawRad * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitchRad * Mth.RAD_TO_DEG + 90));

        // Same UV scrolling maths as vanilla's beacon
        float f = Math.floorMod((long) time, 40L) + (time - (long) time);
        float scroll = Mth.frac(-f * 0.2F - (float) Mth.floor(-f * 0.1F));
        // To make the texture flow toward the enchanter (draining the target), use instead:
        // float scroll = Mth.frac(f * 0.2F - (float) Mth.floor(f * 0.1F));
        float vBase = -1.0F + scroll;
        float vInner = dist * (0.5F / BEAM_RADIUS) + vBase;
        float vGlow = dist + vBase; // vanilla: height * textureScale (1.0), no radius scaling

        float r = BEAM_COLOR[0], g = BEAM_COLOR[1], b = BEAM_COLOR[2];
        float r2 = GLOW_COLOR[0], g2 = GLOW_COLOR[1], b2 = GLOW_COLOR[2];

        // Inner spinning beam (translucent=true so our alpha is respected)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(f * 2.25F - 45.0F));
        renderPart(poseStack, buffer.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true)),
                r, g, b, INNER_ALPHA * alpha, 0, dist,
                0.0F, BEAM_RADIUS, BEAM_RADIUS, 0.0F, -BEAM_RADIUS, 0.0F, 0.0F, -BEAM_RADIUS,
                0.0F, 1.0F, vInner, vBase);
        poseStack.popPose();

        // Outer glow
        renderPart(poseStack, buffer.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true)),
                r2, g2, b2, GLOW_ALPHA * alpha, 0, dist,
                -GLOW_RADIUS, -GLOW_RADIUS, GLOW_RADIUS, -GLOW_RADIUS,
                -GLOW_RADIUS, GLOW_RADIUS, GLOW_RADIUS, GLOW_RADIUS,
                0.0F, 1.0F, vGlow, vBase);

        poseStack.popPose();
    }

    private static void renderPart(PoseStack poseStack, VertexConsumer vc,
                                   float r, float g, float b, float a,
                                   float yMin, float yMax,
                                   float x1, float z1, float x2, float z2,
                                   float x3, float z3, float x4, float z4,
                                   float uMin, float uMax, float vMin, float vMax) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        renderQuad(m, n, vc, r, g, b, a, yMin, yMax, x1, z1, x2, z2, uMin, uMax, vMin, vMax);
        renderQuad(m, n, vc, r, g, b, a, yMin, yMax, x4, z4, x3, z3, uMin, uMax, vMin, vMax);
        renderQuad(m, n, vc, r, g, b, a, yMin, yMax, x2, z2, x4, z4, uMin, uMax, vMin, vMax);
        renderQuad(m, n, vc, r, g, b, a, yMin, yMax, x3, z3, x1, z1, uMin, uMax, vMin, vMax);
    }

    private static void renderQuad(Matrix4f m, Matrix3f n, VertexConsumer vc,
                                   float r, float g, float b, float a,
                                   float yMin, float yMax,
                                   float x1, float z1, float x2, float z2,
                                   float uMin, float uMax, float vMin, float vMax) {
        addVertex(m, n, vc, r, g, b, a, yMax, x1, z1, uMax, vMin);
        addVertex(m, n, vc, r, g, b, a, yMin, x1, z1, uMax, vMax);
        addVertex(m, n, vc, r, g, b, a, yMin, x2, z2, uMin, vMax);
        addVertex(m, n, vc, r, g, b, a, yMax, x2, z2, uMin, vMin);
    }

    private static void addVertex(Matrix4f m, Matrix3f n, VertexConsumer vc,
                                  float r, float g, float b, float a,
                                  float y, float x, float z, float u, float v) {
        vc.vertex(m, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.pack(15, 15))
                .normal(n, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}