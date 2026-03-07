package com.solegendary.reignofnether.unit.modelling.renderers;


import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.goals.GenericTargetedSpellGoal;
import com.solegendary.reignofnether.unit.modelling.models.WildfireModel;
import com.solegendary.reignofnether.unit.units.piglins.WildfireUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.*;

@OnlyIn(Dist.CLIENT)
public class WildfireRenderer extends MobRenderer<WildfireUnit, WildfireModel<WildfireUnit>> {

    public static final float SCALE_MULT = 1.0f;

    private static final ResourceLocation WILDFIRE_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/wildfire_unit.png");
    private static final ResourceLocation SOUL_WILDFIRE_LOCATION = ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/soulfire_wildfire_unit.png");

    public WildfireRenderer(EntityRendererProvider.Context context) {
        super(context, new WildfireModel<>(context.bakeLayer(WildfireModel.LAYER_LOCATION)), 0.5F);
    }

    protected int getBlockLightLevel(WildfireUnit pEntity, BlockPos pPos) {
        return 15;
    }

    public ResourceLocation getTextureLocation(WildfireUnit wildfireUnit) {
        if (wildfireUnit.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get()))
            return SOUL_WILDFIRE_LOCATION;
        else
            return WILDFIRE_LOCATION;
    }

    private static final Map<ResourceLocation, RenderType> EYES_ALPHA_CACHE = new ConcurrentHashMap<>();

    public static RenderType eyesWithAlpha(ResourceLocation texture) {
        return EYES_ALPHA_CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEyesShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                    .setOverlayState(new RenderStateShard.OverlayStateShard(true))
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(true);

            return RenderType.create(
                    "ron_eyes_alpha",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    state
            );
        });
    }

    @Override
    public void render(WildfireUnit wildfire, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight) {

        super.render(wildfire, yaw, partialTicks, poseStack, buffer, packedLight);

        GenericTargetedSpellGoal scorchingGazeGoal = wildfire.getCastScorchingGazeGoal();
        LivingEntity target = scorchingGazeGoal.getTargetEntity();
        if (scorchingGazeGoal.isCasting() && target != null) {
            float alpha = Math.min(1.0f, scorchingGazeGoal.getChannelTicks() / 20f);
            renderFireBeam(
                    wildfire,
                    target,
                    partialTicks,
                    poseStack,
                    buffer,
                    alpha,
                    this.getModel().getHead()
            );
        }
    }

    private static final ResourceLocation FIRE_BEAM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/misc/wildfire_laser.png");

    public static void renderFireBeam(
            WildfireUnit source,
            Entity target,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            float alpha,
            ModelPart headModel
    ) {
        float time = source.tickCount + partialTick;

        // World positions
        Vec3 entityPos = source.getPosition(partialTick);
        Vec3 sourceEyeWorld = source.getEyePosition(partialTick);
        Vec3 targetEyeWorld = target.getEyePosition(partialTick);

        // Compute animated offset in ENTITY-LOCAL model space using the head part transform
        Vec3 headAnimatedLocal = getAnimatedPointFromPart(headModel, 0, 0, -4f / 16f);

        // Rotate that local offset into world-space using the entity's yaw (body yaw)
        float bodyYawDeg = Mth.rotLerp(partialTick, source.yRotO, source.getYRot());
        float bodyYawRad = bodyYawDeg * Mth.DEG_TO_RAD;

        float cos = Mth.cos(bodyYawRad);
        float sin = Mth.sin(bodyYawRad);

        double offX = headAnimatedLocal.x * cos - headAnimatedLocal.z * sin;
        double offZ = headAnimatedLocal.x * sin + headAnimatedLocal.z * cos;
        double offY = headAnimatedLocal.y;

        Vec3 beamOriginWorld = sourceEyeWorld.subtract(offX, offY, offZ);

        // Direction/length from new origin
        Vec3 dir = targetEyeWorld.subtract(beamOriginWorld);
        float dx = (float) dir.x;
        float dy = (float) dir.y;
        float dz = (float) dir.z;

        float dist = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.0001f) return;

        // Convert world origin into renderer-local space (poseStack is already at entity origin)
        Vec3 originLocal = beamOriginWorld.subtract(entityPos);

        poseStack.pushPose();
        poseStack.translate(originLocal.x, originLocal.y - 1.0f, originLocal.z);

        float yaw = (float) Math.atan2(dx, dz);
        float pitch = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.XP.rotationDegrees((-pitch * Mth.RAD_TO_DEG) + 90));

        VertexConsumer vertexConsumer = buffer.getBuffer(eyesWithAlpha(FIRE_BEAM_TEXTURE));

        float scroll = time * 0.02F;
        float vStart = -1.0F + scroll;
        float vEnd = dist * 0.5F + vStart;

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        float pulse = MiscUtil.getOscillatingFloat(0, 1.0f);
        pulse = (float) Math.pow(pulse, 0.4f);

        float r1 = 0.6F, g1 = 0.1F, b1 = 0.0F;
        float r2 = 1.0F, g2 = 0.9F, b2 = 0.1F;
        float r = r1 + (r2 - r1) * pulse;
        float g = g1 + (g2 - g1) * pulse;
        float b = b1 + (b2 - b1) * pulse;

        float radius = 0.2F;
        int sides = 4;

        for (int i = 0; i < sides; ++i) {
            float angle1 = (float) (Math.PI * 2 * i / sides);
            float angle2 = (float) (Math.PI * 2 * (i + 1) / sides);

            float x1 = Mth.cos(angle1) * radius;
            float z1 = Mth.sin(angle1) * radius;
            float x2 = Mth.cos(angle2) * radius;
            float z2 = Mth.sin(angle2) * radius;

            vertexConsumer.vertex(matrix, x1, 0.0F, z1)
                    .color(r, g, b, alpha)
                    .uv(0.0F, vEnd)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.pack(15, 15))
                    .normal(normal, 0, 1, 0)
                    .endVertex();

            vertexConsumer.vertex(matrix, x1, dist, z1)
                    .color(r, g, b, alpha)
                    .uv(0.0F, vStart)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.pack(15, 15))
                    .normal(normal, 0, 1, 0)
                    .endVertex();

            vertexConsumer.vertex(matrix, x2, dist, z2)
                    .color(r, g, b, alpha)
                    .uv(1.0F, vStart)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.pack(15, 15))
                    .normal(normal, 0, 1, 0)
                    .endVertex();

            vertexConsumer.vertex(matrix, x2, 0.0F, z2)
                    .color(r, g, b, alpha)
                    .uv(1.0F, vEnd)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.pack(15, 15))
                    .normal(normal, 0, 1, 0)
                    .endVertex();
        }

        poseStack.popPose();
    }

    /**
     * Returns the position of a point (x,y,z) after applying ModelPart's translate+rotate.
     * Coordinates are in model units (i.e., pixels / 16).
     */
    private static Vec3 getAnimatedPointFromPart(ModelPart part, float x, float y, float z) {
        PoseStack ps = new PoseStack();
        ps.pushPose();
        part.translateAndRotate(ps);

        Matrix4f m = ps.last().pose();
        Vector4f v = new Vector4f(x, y, z, 1.0f).mul(m);

        ps.popPose();
        return new Vec3(v.x(), v.y(), v.z());
    }
}
