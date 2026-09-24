package com.solegendary.reignofnether.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AbstractFloatingParticle extends TextureSheetParticle {

    public boolean darkenOverTime = true;
    public boolean slowOverTime = false;
    public float quadSizeMultiplier = 1.0f;

    AbstractFloatingParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        this.xd *= 0.6;
        this.yd *= 0.6;
        this.zd *= 0.6;
        this.xd += pXSpeed * 0.2;
        this.yd += pYSpeed * 0.2;
        this.zd += pZSpeed * 0.2;
        float f = (float)(Math.random() * 0.4 + 0.6);
        this.rCol = f;
        this.gCol = f;
        this.bCol = f;
        this.quadSize *= 0.75F;
        this.lifetime = (Math.max((int)(15.0 / (Math.random() * 0.8 + 0.6)), 7));
        this.hasPhysics = false;
        this.tick();
    }

    public float getQuadSize(float pScaleFactor) {
        float $$1 = ((this.age * 1.5f) + pScaleFactor) / (float)this.lifetime;
        return Math.max(0, this.quadSize * (1.0F - $$1 * $$1 * 0.5F) * 2.0f * quadSizeMultiplier);
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public void tick() {
        super.tick();
        if (darkenOverTime) {
            this.gCol *= 0.96F;
            this.bCol *= 0.9F;
        }
        if (slowOverTime) {
            this.xd *= 0.95;
            this.yd *= 0.95;
            this.zd *= 0.95;
        }
    }

    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            AbstractFloatingParticle particle = new AbstractFloatingParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            particle.pickSprite(this.sprite);
            return particle;
        }
    }
}