package com.effecoria.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Client particles with school-specific motion — not generic orbs. */
public final class SchoolParticles {
    private SchoolParticles() {}

    /** Soft expanding fog — mental veil, green mist, black necromantic haze. */
    public static class FogParticle extends TextureSheetParticle {
        private final float growRate;

        protected FogParticle(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites,
                float baseSize,
                float growRate) {
            this(level, x, y, z, xd, yd, zd, sprites, baseSize, growRate, 18, 16, 0.3F, 0.25F);
        }

        protected FogParticle(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites,
                float baseSize,
                float growRate,
                int lifeBase,
                int lifeVariance,
                float alphaBase,
                float alphaVariance) {
            super(level, x, y, z, xd, yd, zd);
            this.friction = 0.96F;
            this.hasPhysics = false;
            this.quadSize = baseSize;
            this.growRate = growRate;
            this.lifetime = lifeBase + this.random.nextInt(Math.max(1, lifeVariance));
            this.alpha = alphaBase + this.random.nextFloat() * alphaVariance;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.alpha *= 0.93F;
            this.quadSize += this.growRate;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;
            private final float baseSize;
            private final float growRate;
            private final int lifeBase;
            private final int lifeVariance;
            private final float alphaBase;
            private final float alphaVariance;

            public Provider(SpriteSet sprites, float baseSize, float growRate) {
                this(sprites, baseSize, growRate, 18, 16, 0.3F, 0.25F);
            }

            public Provider(
                    SpriteSet sprites,
                    float baseSize,
                    float growRate,
                    int lifeBase,
                    int lifeVariance,
                    float alphaBase,
                    float alphaVariance) {
                this.sprites = sprites;
                this.baseSize = baseSize;
                this.growRate = growRate;
                this.lifeBase = lifeBase;
                this.lifeVariance = lifeVariance;
                this.alphaBase = alphaBase;
                this.alphaVariance = alphaVariance;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new FogParticle(
                        level,
                        x,
                        y,
                        z,
                        xd,
                        yd,
                        zd,
                        this.sprites,
                        this.baseSize,
                        this.growRate,
                        this.lifeBase,
                        this.lifeVariance,
                        this.alphaBase,
                        this.alphaVariance);
            }
        }
    }

    /** Falling droplet — water stream, poison drip. */
    public static class DropParticle extends TextureSheetParticle {
        protected DropParticle(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.gravity = 0.1F;
            this.friction = 0.98F;
            this.quadSize = 0.07F + this.random.nextFloat() * 0.05F;
            this.lifetime = 14 + this.random.nextInt(10);
            this.sprite = sprites.get(this.random);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new DropParticle(
                        level,
                        x,
                        y,
                        z,
                        xd + (level.random.nextFloat() - 0.5F) * 0.04,
                        yd,
                        zd + (level.random.nextFloat() - 0.5F) * 0.04,
                        this.sprites);
            }
        }
    }

    /** Radial splash ring that expands and fades. */
    public static class SplashParticle extends TextureSheetParticle {
        protected SplashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, 0, 0, 0);
            this.hasPhysics = false;
            this.quadSize = 0.12F;
            this.lifetime = 10;
            this.alpha = 0.85F;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.quadSize += 0.06F;
            this.alpha *= 0.82F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new SplashParticle(level, x, y, z, this.sprites);
            }
        }
    }

    /** Horizontal streak — water wave crest or wind gust. */
    public static class StreakParticle extends TextureSheetParticle {
        protected StreakParticle(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites,
                float size) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.friction = 0.92F;
            this.quadSize = size;
            this.lifetime = 6 + this.random.nextInt(5);
            this.alpha = 0.75F;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.alpha *= 0.88F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;
            private final float size;

            public Provider(SpriteSet sprites, float size) {
                this.sprites = sprites;
                this.size = size;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new StreakParticle(level, x, y, z, xd, yd, zd, this.sprites, this.size);
            }
        }
    }

    /** Rising flame tongue with Φ rim. */
    public static class FlameParticle extends TextureSheetParticle {
        protected FlameParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = -0.03F;
            this.quadSize = 0.1F + this.random.nextFloat() * 0.08F;
            this.lifetime = 10 + this.random.nextInt(8);
            this.yd = 0.04 + this.random.nextFloat() * 0.04;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.alpha *= 0.92F;
            this.quadSize *= 0.97F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new FlameParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Rising then drifting ember ash. */
    public static class EmberParticle extends TextureSheetParticle {
        protected EmberParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = 0.02F;
            this.quadSize = 0.05F + this.random.nextFloat() * 0.04F;
            this.lifetime = 16 + this.random.nextInt(12);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.02;
            this.yd = 0.03 + this.random.nextFloat() * 0.04;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.02;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.xd += Mth.sin(this.age * 0.3F) * 0.0015;
            this.alpha = 1.0F - (float) this.age / (float) this.lifetime;
            this.quadSize *= 0.99F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new EmberParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Hot plasma core — fast bright flicker. */
    public static class PlasmaParticle extends TextureSheetParticle {
        protected PlasmaParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = -0.02F;
            this.quadSize = 0.09F + this.random.nextFloat() * 0.07F;
            this.lifetime = 8 + this.random.nextInt(6);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.04;
            this.yd = 0.02 + this.random.nextFloat() * 0.05;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.04;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            this.roll += 0.25F;
            this.alpha *= 0.88F;
            this.quadSize *= 0.96F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new PlasmaParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Jagged lightning spark. */
    public static class LightningSparkParticle extends TextureSheetParticle {
        protected LightningSparkParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.quadSize = 0.07F + this.random.nextFloat() * 0.06F;
            this.lifetime = 4 + this.random.nextInt(5);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.12;
            this.yd = yd + (this.random.nextFloat() - 0.5F) * 0.12;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.12;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.xd *= 0.85;
            this.yd *= 0.85;
            this.zd *= 0.85;
            this.alpha *= 0.8F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new LightningSparkParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Inward vacuum mote. */
    public static class VacuumParticle extends TextureSheetParticle {
        protected VacuumParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = 0.0F;
            this.quadSize = 0.1F + this.random.nextFloat() * 0.08F;
            this.lifetime = 14 + this.random.nextInt(10);
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            this.sprite = sprites.get(this.random);
            this.alpha = 0.7F;
        }

        @Override
        public void tick() {
            super.tick();
            this.xd *= 0.9;
            this.yd *= 0.9;
            this.zd *= 0.9;
            this.alpha = 0.7F * (1.0F - (float) this.age / (float) this.lifetime);
            this.quadSize *= 0.98F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new VacuumParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Sharp psychic shard — bolts / lances / stings. */
    public static class MentalShardParticle extends TextureSheetParticle {
        protected MentalShardParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.quadSize = 0.07F + this.random.nextFloat() * 0.05F;
            this.lifetime = 8 + this.random.nextInt(6);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.08;
            this.yd = yd + (this.random.nextFloat() - 0.5F) * 0.06;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.08;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.xd *= 0.88;
            this.yd *= 0.88;
            this.zd *= 0.88;
            this.alpha *= 0.85F;
            this.quadSize *= 0.97F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new MentalShardParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Telekinetic force streak. */
    public static class MentalForceParticle extends TextureSheetParticle {
        protected MentalForceParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.quadSize = 0.1F + this.random.nextFloat() * 0.08F;
            this.lifetime = 10 + this.random.nextInt(8);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.1;
            this.yd = 0.01 + this.random.nextFloat() * 0.03;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.1;
            this.sprite = sprites.get(this.random);
            this.alpha = 0.8F;
        }

        @Override
        public void tick() {
            super.tick();
            this.xd *= 0.92;
            this.zd *= 0.92;
            this.alpha = 0.8F * (1.0F - (float) this.age / (float) this.lifetime);
            this.quadSize *= 1.02F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new MentalForceParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Cyan-violet synapse flash. */
    public static class MentalSynapseParticle extends TextureSheetParticle {
        protected MentalSynapseParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.quadSize = 0.06F + this.random.nextFloat() * 0.05F;
            this.lifetime = 6 + this.random.nextInt(6);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.07;
            this.yd = 0.02 + this.random.nextFloat() * 0.05;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.07;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.xd *= 0.88;
            this.yd *= 0.88;
            this.zd *= 0.88;
            this.alpha = 1.0F - (float) this.age / (float) this.lifetime;
            this.quadSize *= 0.95F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new MentalSynapseParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Soft ward hex mote. */
    public static class MentalWardParticle extends TextureSheetParticle {
        protected MentalWardParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = -0.005F;
            this.quadSize = 0.09F + this.random.nextFloat() * 0.05F;
            this.lifetime = 18 + this.random.nextInt(10);
            this.xd = xd * 0.4 + (this.random.nextFloat() - 0.5F) * 0.02;
            this.yd = 0.01 + this.random.nextFloat() * 0.02;
            this.zd = zd * 0.4 + (this.random.nextFloat() - 0.5F) * 0.02;
            this.sprite = sprites.get(this.random);
            this.alpha = 0.75F;
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            this.roll += 0.08F;
            this.alpha = 0.75F * (1.0F - (float) this.age / (float) this.lifetime * 0.85F);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new MentalWardParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Dark compulsion / terror mote. */
    public static class MentalFearParticle extends TextureSheetParticle {
        protected MentalFearParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.quadSize = 0.08F + this.random.nextFloat() * 0.06F;
            this.lifetime = 14 + this.random.nextInt(10);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.04;
            this.yd = -0.01 + this.random.nextFloat() * 0.02;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.04;
            this.sprite = sprites.get(this.random);
            this.alpha = 0.85F;
        }

        @Override
        public void tick() {
            super.tick();
            this.xd += Mth.sin(this.age * 0.4F) * 0.002;
            this.alpha = 0.85F * (1.0F - (float) this.age / (float) this.lifetime);
            this.quadSize *= 0.98F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new MentalFearParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Iris / sense spark. */
    public static class MentalSenseParticle extends TextureSheetParticle {
        protected MentalSenseParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = -0.01F;
            this.quadSize = 0.07F + this.random.nextFloat() * 0.05F;
            this.lifetime = 12 + this.random.nextInt(8);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.03;
            this.yd = 0.02 + this.random.nextFloat() * 0.03;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.03;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            this.roll += 0.15F;
            this.alpha *= 0.9F;
            this.quadSize *= 0.98F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new MentalSenseParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Drifting leaf with gentle tumble. */
    public static class LeafParticle extends TextureSheetParticle {
        protected LeafParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.gravity = 0.015F;
            this.friction = 0.99F;
            this.quadSize = 0.1F + this.random.nextFloat() * 0.06F;
            this.lifetime = 22 + this.random.nextInt(14);
            this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            this.roll += 0.12F;
            this.xd += Mth.sin(this.age * 0.15F) * 0.002;
            this.zd += Mth.cos(this.age * 0.12F) * 0.002;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new LeafParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Root tendril erupting upward then fading. xd &gt;= 0.4 encodes size scale (root wrap). */
    public static class RootParticle extends TextureSheetParticle {
        private final float baseSize;

        protected RootParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, 0, yd > 0.001 ? yd : 0.08, 0);
            this.hasPhysics = false;
            float scale = xd >= 0.4 ? (float) Mth.clamp(xd, 0.4, 5.0) : 1.0F;
            this.baseSize = (0.12F + this.random.nextFloat() * 0.08F) * scale;
            this.quadSize = 0.02F * scale;
            this.lifetime = 16 + this.random.nextInt(8) + (scale > 1.5F ? 4 : 0);
            this.roll = (this.random.nextFloat() - 0.5F) * 0.4F;
            this.sprite = sprites.get(this.random);
            this.alpha = 0.95F;
        }

        @Override
        public void tick() {
            super.tick();
            float t = (float) this.age / (float) this.lifetime;
            // Grow in then fade — reads as a root pushing up from soil.
            float grow = t < 0.35F ? t / 0.35F : 1.0F;
            this.quadSize = this.baseSize * (0.35F + 0.65F * grow);
            this.yd *= 0.88;
            this.alpha = t < 0.55F ? 0.95F : 0.95F * (1.0F - (t - 0.55F) / 0.45F);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new RootParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Floating spore mote. */
    public static class SporeParticle extends TextureSheetParticle {
        protected SporeParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = -0.004F;
            this.quadSize = 0.06F + this.random.nextFloat() * 0.05F;
            this.lifetime = 24 + this.random.nextInt(16);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.02;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.02;
            this.yd = 0.01 + this.random.nextFloat() * 0.02;
            this.sprite = sprites.get(this.random);
            this.alpha = 0.85F;
        }

        @Override
        public void tick() {
            super.tick();
            this.xd += (this.random.nextFloat() - 0.5F) * 0.003;
            this.zd += (this.random.nextFloat() - 0.5F) * 0.003;
            this.alpha *= 0.97F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new SporeParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Sharp thorn shard streak. */
    public static class ThornParticle extends TextureSheetParticle {
        protected ThornParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.quadSize = 0.1F + this.random.nextFloat() * 0.05F;
            this.lifetime = 10 + this.random.nextInt(6);
            this.roll = (float) Math.atan2(xd, zd);
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.xd *= 0.92;
            this.yd *= 0.92;
            this.zd *= 0.92;
            this.alpha = 1.0F - ((float) this.age / (float) this.lifetime);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new ThornParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Amber sap droplet. */
    public static class SapParticle extends TextureSheetParticle {
        protected SapParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.gravity = 0.04F;
            this.friction = 0.96F;
            this.quadSize = 0.07F + this.random.nextFloat() * 0.04F;
            this.lifetime = 18 + this.random.nextInt(10);
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.alpha = 1.0F - ((float) this.age / (float) this.lifetime) * 0.85F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new SapParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Red blood cell — soft disc drifting in plasma. */
    public static class BloodCellParticle extends TextureSheetParticle {
        protected BloodCellParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = -0.002F;
            this.quadSize = 0.07F + this.random.nextFloat() * 0.05F;
            this.lifetime = 22 + this.random.nextInt(14);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.025;
            this.yd = 0.01 + this.random.nextFloat() * 0.02;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.025;
            this.roll = this.random.nextFloat() * (float) Math.PI;
            this.sprite = sprites.get(this.random);
            this.alpha = 0.9F;
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            this.roll += 0.08F;
            this.xd += Mth.sin(this.age * 0.2F) * 0.0015;
            this.zd += Mth.cos(this.age * 0.18F) * 0.0015;
            this.alpha = 0.9F * (1.0F - (float) this.age / (float) this.lifetime);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new BloodCellParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** White / stabilizer cell — larger, slower, pale. */
    public static class WhiteCellParticle extends TextureSheetParticle {
        protected WhiteCellParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = -0.001F;
            this.quadSize = 0.1F + this.random.nextFloat() * 0.06F;
            this.lifetime = 28 + this.random.nextInt(16);
            this.xd = xd * 0.4 + (this.random.nextFloat() - 0.5F) * 0.015;
            this.yd = 0.008 + this.random.nextFloat() * 0.012;
            this.zd = zd * 0.4 + (this.random.nextFloat() - 0.5F) * 0.015;
            this.sprite = sprites.get(this.random);
            this.alpha = 0.75F;
        }

        @Override
        public void tick() {
            super.tick();
            this.xd *= 0.98;
            this.zd *= 0.98;
            this.alpha = 0.75F * (1.0F - (float) this.age / (float) this.lifetime * 0.9F);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new WhiteCellParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Spiked virion — foreign agent / immune breach. */
    public static class VirusParticle extends TextureSheetParticle {
        protected VirusParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = -0.004F;
            this.quadSize = 0.09F + this.random.nextFloat() * 0.06F;
            this.lifetime = 24 + this.random.nextInt(14);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.03;
            this.yd = 0.012 + this.random.nextFloat() * 0.02;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.03;
            this.roll = this.random.nextFloat() * (float) Math.PI;
            this.sprite = sprites.get(this.random);
            this.alpha = 0.95F;
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            this.roll += 0.14F;
            this.xd += Mth.sin(this.age * 0.35F) * 0.002;
            this.zd += Mth.cos(this.age * 0.3F) * 0.002;
            this.alpha = 0.95F * (1.0F - (float) this.age / (float) this.lifetime);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new VirusParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Squirming parasite larva. */
    public static class ParasiteParticle extends TextureSheetParticle {
        protected ParasiteParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = 0.01F;
            this.quadSize = 0.1F + this.random.nextFloat() * 0.05F;
            this.lifetime = 20 + this.random.nextInt(12);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.04;
            this.yd = yd * 0.2 + (this.random.nextFloat() - 0.3F) * 0.02;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.04;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            this.roll = Mth.sin(this.age * 0.55F) * 0.6F;
            this.xd += Mth.sin(this.age * 0.45F) * 0.003;
            this.zd += Mth.cos(this.age * 0.4F) * 0.003;
            this.alpha = 1.0F - (float) this.age / (float) this.lifetime;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new ParasiteParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Bone shard burst. */
    public static class BoneShardParticle extends TextureSheetParticle {
        protected BoneShardParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = 0.05F;
            this.quadSize = 0.08F + this.random.nextFloat() * 0.05F;
            this.lifetime = 14 + this.random.nextInt(8);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.08;
            this.yd = 0.05 + this.random.nextFloat() * 0.08;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.08;
            this.roll = this.random.nextFloat() * (float) Math.PI;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            this.roll += 0.2F;
            this.alpha = 1.0F - (float) this.age / (float) this.lifetime;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new BoneShardParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Chitin plate flake settling onto armor. */
    public static class ChitinParticle extends TextureSheetParticle {
        protected ChitinParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = 0.02F;
            this.quadSize = 0.11F + this.random.nextFloat() * 0.06F;
            this.lifetime = 22 + this.random.nextInt(10);
            this.xd = xd * 0.3 + (this.random.nextFloat() - 0.5F) * 0.02;
            this.yd = 0.02 + this.random.nextFloat() * 0.03;
            this.zd = zd * 0.3 + (this.random.nextFloat() - 0.5F) * 0.02;
            this.sprite = sprites.get(this.random);
            this.alpha = 0.9F;
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            this.roll += 0.05F;
            this.alpha = 0.9F * (1.0F - (float) this.age / (float) this.lifetime * 0.85F);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new ChitinParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Twitching muscle fiber. */
    public static class MuscleParticle extends TextureSheetParticle {
        protected MuscleParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.quadSize = 0.1F + this.random.nextFloat() * 0.05F;
            this.lifetime = 12 + this.random.nextInt(8);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.05;
            this.yd = yd + (this.random.nextFloat() - 0.5F) * 0.04;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.05;
            this.roll = (float) Math.atan2(this.xd, this.zd);
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.xd *= 0.88;
            this.yd *= 0.88;
            this.zd *= 0.88;
            this.quadSize *= 0.97F;
            this.alpha = 1.0F - (float) this.age / (float) this.lifetime;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new MuscleParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Cyan synapse flash. */
    public static class NerveParticle extends TextureSheetParticle {
        protected NerveParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.quadSize = 0.06F + this.random.nextFloat() * 0.05F;
            this.lifetime = 8 + this.random.nextInt(8);
            this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.06;
            this.yd = 0.02 + this.random.nextFloat() * 0.05;
            this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.06;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.xd *= 0.9;
            this.yd *= 0.9;
            this.zd *= 0.9;
            this.alpha = 1.0F - (float) this.age / (float) this.lifetime;
            this.quadSize *= 0.96F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new NerveParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** DNA helix fragment — genetic rewrite. */
    public static class DnaParticle extends TextureSheetParticle {
        protected DnaParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = -0.003F;
            this.quadSize = 0.1F + this.random.nextFloat() * 0.06F;
            this.lifetime = 26 + this.random.nextInt(14);
            this.xd = xd * 0.35 + (this.random.nextFloat() - 0.5F) * 0.015;
            this.yd = 0.015 + this.random.nextFloat() * 0.02;
            this.zd = zd * 0.35 + (this.random.nextFloat() - 0.5F) * 0.015;
            this.roll = this.random.nextFloat() * (float) Math.PI;
            this.sprite = sprites.get(this.random);
            this.alpha = 0.85F;
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            this.roll += 0.12F;
            this.xd += Mth.sin(this.age * 0.25F + this.roll) * 0.0015;
            this.zd += Mth.cos(this.age * 0.25F + this.roll) * 0.0015;
            this.alpha = 0.85F * (1.0F - (float) this.age / (float) this.lifetime * 0.9F);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new DnaParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Dark shadow wisp drifting upward. */
    public static class ShadowParticle extends TextureSheetParticle {
        protected ShadowParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.gravity = -0.008F;
            this.quadSize = 0.12F + this.random.nextFloat() * 0.1F;
            this.lifetime = 20 + this.random.nextInt(12);
            this.yd = 0.015 + this.random.nextFloat() * 0.02;
            this.alpha = 0.65F;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.alpha *= 0.95F;
            this.quadSize += 0.008F;
            this.xd += (this.random.nextFloat() - 0.5F) * 0.002;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new ShadowParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /**
     * Spatial distortion — wobbling scale/roll approximates sprite warp without a post shader.
     */
    public static class WarpParticle extends TextureSheetParticle {
        private final float baseSize;
        private final float wobbleSpeed;
        private final float wobbleAmount;

        protected WarpParticle(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites,
                float baseSize,
                float wobbleSpeed,
                float wobbleAmount) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.baseSize = baseSize;
            this.wobbleSpeed = wobbleSpeed;
            this.wobbleAmount = wobbleAmount;
            this.quadSize = baseSize;
            this.lifetime = 14 + this.random.nextInt(10);
            this.alpha = 0.8F;
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            float wobble = Mth.sin(this.age * this.wobbleSpeed) * this.wobbleAmount;
            this.quadSize = this.baseSize + wobble;
            this.oRoll = this.roll;
            this.roll = this.age * 0.12F + wobble * 2.0F;
            this.alpha *= 0.94F;
            this.xd += (this.random.nextFloat() - 0.5F) * 0.004;
            this.zd += (this.random.nextFloat() - 0.5F) * 0.004;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;
            private final float baseSize;
            private final float wobbleSpeed;
            private final float wobbleAmount;

            public Provider(SpriteSet sprites, float baseSize, float wobbleSpeed, float wobbleAmount) {
                this.sprites = sprites;
                this.baseSize = baseSize;
                this.wobbleSpeed = wobbleSpeed;
                this.wobbleAmount = wobbleAmount;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new WarpParticle(
                        level, x, y, z, xd, yd, zd, this.sprites, this.baseSize, this.wobbleSpeed, this.wobbleAmount);
            }
        }
    }

    /** Rotating seal / corruption glyph. */
    public static class GlyphParticle extends TextureSheetParticle {
        protected GlyphParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.quadSize = 0.14F + this.random.nextFloat() * 0.06F;
            this.lifetime = 22 + this.random.nextInt(10);
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.oRoll = this.roll;
            this.roll += 0.06F;
            this.alpha = 1.0F - ((float) this.age / (float) this.lifetime) * 0.5F;
            this.quadSize *= 0.995F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new GlyphParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Brief bright spark — seal glow, Φ motes. */
    public static class SparkParticle extends TextureSheetParticle {
        protected SparkParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.hasPhysics = false;
            this.quadSize = 0.06F + this.random.nextFloat() * 0.04F;
            this.lifetime = 6 + this.random.nextInt(4);
            this.sprite = sprites.get(this.random);
        }

        @Override
        public void tick() {
            super.tick();
            this.alpha *= 0.85F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new SparkParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }

    /** Blood splatter burst with gravity. */
    public static class BloodParticle extends TextureSheetParticle {
        protected BloodParticle(
                ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd);
            this.gravity = 0.12F;
            this.friction = 0.96F;
            this.quadSize = 0.06F + this.random.nextFloat() * 0.04F;
            this.lifetime = 10 + this.random.nextInt(6);
            this.sprite = sprites.get(this.random);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }

        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;

            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }

            @Override
            public Particle createParticle(
                    SimpleParticleType type,
                    ClientLevel level,
                    double x,
                    double y,
                    double z,
                    double xd,
                    double yd,
                    double zd) {
                return new BloodParticle(level, x, y, z, xd, yd, zd, this.sprites);
            }
        }
    }
}
