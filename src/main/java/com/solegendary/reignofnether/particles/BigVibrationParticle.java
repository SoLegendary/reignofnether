//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class BigVibrationParticle extends TextureSheetParticle {
    private final PositionSource target;
    private float yaw;
    private float yawO;

    BigVibrationParticle(ClientLevel pLevel, double pX, double pY, double pZ, PositionSource pTarget, int pLifetime) {
        super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
        this.quadSize = 0.3F;
        this.target = pTarget;
        this.lifetime = pLifetime;

        // Face the initial direction of travel (toward the target) before the first tick runs.
        Optional<Vec3> optional = pTarget.getPosition(pLevel);
        if (optional.isPresent()) {
            Vec3 vec3 = (Vec3)optional.get();
            double d0 = vec3.x() - pX;
            double d2 = vec3.z() - pZ;
            this.yawO = this.yaw = (float)Mth.atan2(d0, d2);
        }
    }

    // Base size scaled up 4x, then shrunk linearly to zero as the particle approaches
    // the end of its lifetime.
    public float getQuadSize(float pScaleFactor) {
        float progress = Mth.clamp(((float) this.age + pScaleFactor) / (float) this.lifetime, 0.0F, 1.0F);
        float shrink = 1.0F - progress;
        return super.getQuadSize(pScaleFactor) * 5.0F * shrink;
    }

    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        // Smoothly interpolated heading, aligned with the particle's direction of motion.
        float f = Mth.lerp(pPartialTicks, this.yawO, this.yaw);

        // Lay the quad flat on the horizontal (XZ) plane instead of billboarding it toward the
        // camera/target, so it always reads as fully visible from directly overhead, while
        // rotateY(f) after the flattening turns it in-plane to face its direction of travel.
        // Two passes with opposite normals make it double-sided (visible from above and below).
        this.renderSignal(pBuffer, pRenderInfo, pPartialTicks, (p_253355_) -> {
            p_253355_.rotateX(1.5707964F).rotateY(f);
        });
        this.renderSignal(pBuffer, pRenderInfo, pPartialTicks, (p_253351_) -> {
            p_253351_.rotateX(-1.5707964F).rotateY(f);
        });
    }

    private void renderSignal(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks, Consumer<Quaternionf> pQuaternionConsumer) {
        Vec3 vec3 = pRenderInfo.getPosition();
        float f = (float)(Mth.lerp((double)pPartialTicks, this.xo, this.x) - vec3.x());
        float f1 = (float)(Mth.lerp((double)pPartialTicks, this.yo, this.y) - vec3.y());
        float f2 = (float)(Mth.lerp((double)pPartialTicks, this.zo, this.z) - vec3.z());
        Vector3f vector3f = (new Vector3f(0.5F, 0.5F, 0.5F)).normalize();
        Quaternionf quaternionf = (new Quaternionf()).setAngleAxis(0.0F, vector3f.x(), vector3f.y(), vector3f.z());
        pQuaternionConsumer.accept(quaternionf);
        Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        float f3 = this.getQuadSize(pPartialTicks);

        for(int i = 0; i < 4; ++i) {
            Vector3f vector3f1 = avector3f[i];
            vector3f1.rotate(quaternionf);
            vector3f1.mul(f3);
            vector3f1.add(f, f1, f2);
        }

        float f6 = this.getU0();
        float f7 = this.getU1();
        float f4 = this.getV0();
        float f5 = this.getV1();
        int j = this.getLightColor(pPartialTicks);
        pBuffer.vertex((double)avector3f[0].x(), (double)avector3f[0].y(), (double)avector3f[0].z()).uv(f7, f5).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        pBuffer.vertex((double)avector3f[1].x(), (double)avector3f[1].y(), (double)avector3f[1].z()).uv(f7, f4).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        pBuffer.vertex((double)avector3f[2].x(), (double)avector3f[2].y(), (double)avector3f[2].z()).uv(f6, f4).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        pBuffer.vertex((double)avector3f[3].x(), (double)avector3f[3].y(), (double)avector3f[3].z()).uv(f6, f5).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
    }

    public int getLightColor(float pPartialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            Optional<Vec3> optional = this.target.getPosition(this.level);
            if (optional.isEmpty()) {
                this.remove();
            } else {
                int i = this.lifetime - this.age;
                double d0 = 1.0 / (double)i;
                Vec3 vec3 = (Vec3)optional.get();
                this.x = Mth.lerp(d0, this.x, vec3.x());
                this.y = Mth.lerp(d0, this.y, vec3.y());
                this.z = Mth.lerp(d0, this.z, vec3.z());
                this.setPos(this.x, this.y, this.z);

                // Face the direction actually just moved in, so the flat quad tracks its motion.
                double d1 = this.x - this.xo;
                double d2 = this.z - this.zo;
                if (d1 * d1 + d2 * d2 > 1.0E-9) {
                    this.yawO = this.yaw;
                    this.yaw = (float)Mth.atan2(d1, d2);
                }
            }
        }

    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<BigVibrationParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(BigVibrationParticleOption pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            BigVibrationParticle vibrationParticle = new BigVibrationParticle(pLevel, pX, pY, pZ, pType.getDestination(), pType.getArrivalInTicks());
            vibrationParticle.pickSprite(this.sprite);
            vibrationParticle.setAlpha(1.0F);
            return vibrationParticle;
        }
    }
}