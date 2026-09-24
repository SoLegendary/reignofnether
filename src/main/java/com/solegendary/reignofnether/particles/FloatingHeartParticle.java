package com.solegendary.reignofnether.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FloatingHeartParticle extends AbstractFloatingParticle {

    FloatingHeartParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        this.darkenOverTime = false;
        this.slowOverTime = true;
        this.lifetime *= 2.0f;
        this.quadSizeMultiplier = 2.0f;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            FloatingHeartParticle particle = new FloatingHeartParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            particle.pickSprite(this.sprite);
            return particle;
        }
    }
}