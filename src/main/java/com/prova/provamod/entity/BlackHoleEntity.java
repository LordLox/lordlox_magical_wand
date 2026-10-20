package com.prova.provamod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class BlackHoleEntity extends Entity implements TraceableEntity {

    public static final int MAX_DURATION = 140; // 7 seconds (140 ticks)
    public static final double GRAVITY_RADIUS = 14.0;
    public static final double CORE_RADIUS = 2.2;

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private Entity cachedOwner;

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    public void setOwner(@Nullable Entity owner) {
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
            this.cachedOwner = owner;
        }
    }

    @Override
    @Nullable
    public Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        }
        if (this.ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            this.cachedOwner = serverLevel.getEntity(this.ownerUUID);
            return this.cachedOwner;
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.tickCount = input.getInt("Age").orElse(0);
        input.getString("Owner").ifPresent(uuidStr -> {
            try {
                this.ownerUUID = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {
            }
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Age", this.tickCount);
        if (this.ownerUUID != null) {
            output.putString("Owner", this.ownerUUID.toString());
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount >= MAX_DURATION) {
            collapseAndDie();
            return;
        }

        if (level().isClientSide()) {
            renderCosmicParticles();
        } else {
            tickGravitationalPullAndDamage();
        }
    }

    /**
     * Highly detailed, multi-layered cosmological particle system.
     */
    private void renderCosmicParticles() {
        Vec3 center = this.position().add(0, 0.5, 0);
        RandomSource random = this.random;
        int age = this.tickCount;

        // 1. Singularity Event Horizon: dense black sphere
        for (int i = 0; i < 18; i++) {
            double theta = random.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
            double r = 0.45 + random.nextDouble() * 0.25;
            double px = center.x + r * Math.sin(phi) * Math.cos(theta);
            double py = center.y + r * Math.cos(phi);
            double pz = center.z + r * Math.sin(phi) * Math.sin(theta);

            if (random.nextBoolean()) {
                level().addParticle(ParticleTypes.SQUID_INK, px, py, pz, 0, 0, 0);
            } else {
                level().addParticle(ParticleTypes.SMOKE, px, py, pz, 0, 0, 0);
            }
        }

        // 2. Accretion Disk: spinning galaxy disk around the singularity
        int diskCount = 22;
        double spinAngle = age * 0.22;
        for (int i = 0; i < diskCount; i++) {
            double radius = 0.75 + Math.pow(random.nextDouble(), 1.4) * 2.8;
            double angle = spinAngle + (random.nextDouble() * 2.0 * Math.PI);

            double diskX = radius * Math.cos(angle);
            double diskZ = radius * Math.sin(angle);
            double diskY = (random.nextDouble() - 0.5) * 0.2 * (radius / 3.0);

            // Tangential orbital velocity plus inward gravitational drain
            double orbitalSpeed = 0.15 * Math.sqrt(1.6 / radius);
            double inwardSpeed = 0.05;
            double vx = -Math.sin(angle) * orbitalSpeed - Math.cos(angle) * inwardSpeed;
            double vz = Math.cos(angle) * orbitalSpeed - Math.sin(angle) * inwardSpeed;
            double vy = -diskY * 0.08;

            double roll = random.nextDouble();
            if (roll < 0.40) {
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, center.x + diskX, center.y + diskY, center.z + diskZ, vx, vy, vz);
            } else if (roll < 0.70) {
                level().addParticle(ParticleTypes.PORTAL, center.x + diskX, center.y + diskY, center.z + diskZ, vx * 0.6, vy, vz * 0.6);
            } else if (roll < 0.88) {
                level().addParticle(ParticleTypes.WITCH, center.x + diskX, center.y + diskY, center.z + diskZ, vx * 0.7, vy, vz * 0.7);
            } else {
                level().addParticle(ParticleTypes.ENCHANT, center.x + diskX, center.y + diskY, center.z + diskZ, vx, vy, vz);
            }
        }

        // 3. Relativistic Astrophysical Jets (North & South poles)
        for (int j = 0; j < 2; j++) {
            double spreadX = (random.nextDouble() - 0.5) * 0.05;
            double spreadZ = (random.nextDouble() - 0.5) * 0.05;

            // North pole jet (Upwards)
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.35, center.z, spreadX, 0.24, spreadZ);
            level().addParticle(ParticleTypes.END_ROD, center.x, center.y + 0.45, center.z, spreadX * 0.4, 0.30, spreadZ * 0.4);

            // South pole jet (Downwards)
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y - 0.35, center.z, spreadX, -0.24, spreadZ);
            level().addParticle(ParticleTypes.END_ROD, center.x, center.y - 0.45, center.z, spreadX * 0.4, -0.30, spreadZ * 0.4);
        }

        // 4. Infalling Space Distortion: particles rushing into the vortex from afar
        for (int k = 0; k < 12; k++) {
            double dist = 4.0 + random.nextDouble() * 4.5;
            double theta = random.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
            double sx = center.x + dist * Math.sin(phi) * Math.cos(theta);
            double sy = center.y + dist * Math.cos(phi);
            double sz = center.z + dist * Math.sin(phi) * Math.sin(theta);

            Vec3 inwards = center.subtract(sx, sy, sz).normalize().scale(0.38 + random.nextDouble() * 0.22);
            if (random.nextBoolean()) {
                level().addParticle(ParticleTypes.PORTAL, sx, sy, sz, inwards.x, inwards.y, inwards.z);
            } else {
                level().addParticle(ParticleTypes.REVERSE_PORTAL, sx, sy, sz, inwards.x, inwards.y, inwards.z);
            }
        }

        // 5. Periodic Gravitational Pulse Ring
        if (age % 14 == 0) {
            double ringRadius = 3.6;
            for (int r = 0; r < 24; r++) {
                double a = (r / 24.0) * 2.0 * Math.PI;
                double rx = center.x + ringRadius * Math.cos(a);
                double rz = center.z + ringRadius * Math.sin(a);
                Vec3 pulse = center.subtract(rx, center.y, rz).normalize().scale(0.32);
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, rx, center.y, rz, pulse.x, 0, pulse.z);
            }
        }
    }

    /**
     * Server physics: pulls entities (excluding owner) and deals continuous damage inside core.
     */
    private void tickGravitationalPullAndDamage() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        Vec3 center = this.position().add(0, 0.5, 0);
        AABB pullBox = getBoundingBox().inflate(GRAVITY_RADIUS);

        List<Entity> affectedEntities = serverLevel.getEntities(this, pullBox, e -> {
            if (e == this) return false;
            // The player who cast it is completely exempt from pull and damage
            if (this.ownerUUID != null && e.getUUID().equals(this.ownerUUID)) return false;
            if (e instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
            if (e instanceof LivingEntity living && !living.isAlive()) return false;
            return e instanceof LivingEntity || e instanceof ItemEntity;
        });

        for (Entity target : affectedEntities) {
            Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);
            Vec3 toCenter = center.subtract(targetCenter);
            double dist = toCenter.length();

            if (dist > GRAVITY_RADIUS) continue;

            // Gravitational attraction force (stronger closer to singularity)
            double normalizedDist = Math.max(0.1, dist / GRAVITY_RADIUS);
            double pullSpeed = Math.clamp(0.50 * (1.0 - normalizedDist) + 0.12, 0.08, 0.72);

            Vec3 pullDir = toCenter.normalize().scale(pullSpeed);
            // Tangential vortex swirl around vertical axis
            Vec3 swirl = new Vec3(-toCenter.z, 0, toCenter.x).normalize().scale(pullSpeed * 0.42);

            Vec3 currentVelocity = target.getDeltaMovement();
            Vec3 newVelocity = currentVelocity.scale(0.78).add(pullDir).add(swirl);
            target.setDeltaMovement(newVelocity);
            target.hurtMarked = true;
            target.resetFallDistance();

            // Continuous damage inside the core
            if (dist <= CORE_RADIUS && target instanceof LivingEntity living) {
                if (this.tickCount % 4 == 0) {
                    living.invulnerableTime = 0;
                    DamageSource source = serverLevel.damageSources().indirectMagic(this, getOwner());
                    living.hurtServer(serverLevel, source, 3.0F);
                    living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, false));
                }
            }
        }

        // Ambient sound effects
        if (this.tickCount % 12 == 0) {
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 2.0F, 0.5F);
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 1.3F, 0.6F);
        }
        if (this.tickCount % 24 == 0) {
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.2F, 0.75F);
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.4F, 0.5F);
        }
    }

    /**
     * Implosion / collapse when the black hole expires.
     */
    private void collapseAndDie() {
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 center = this.position().add(0, 0.5, 0);

            // Collapse soundscape
            serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 1.1F);
            serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.7F);
            serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.6F, 1.3F);

            // Massive implosion particles
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 2, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 2, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 75, 1.5, 1.5, 1.5, 0.25);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 50, 1.2, 1.2, 1.2, 0.30);
            serverLevel.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z, 40, 1.0, 1.0, 1.0, 0.15);
            serverLevel.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 50, 1.0, 1.0, 1.0, 0.5);

            // Shockwave burst damage & knockback
            AABB finalArea = getBoundingBox().inflate(5.0);
            List<LivingEntity> trapped = serverLevel.getEntitiesOfClass(LivingEntity.class, finalArea, e -> {
                if (this.ownerUUID != null && e.getUUID().equals(this.ownerUUID)) return false;
                if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
                return e.isAlive();
            });

            for (LivingEntity le : trapped) {
                le.invulnerableTime = 0;
                le.hurtServer(serverLevel, serverLevel.damageSources().indirectMagic(this, getOwner()), 8.0F);
                Vec3 knockback = le.position().subtract(center).normalize().scale(1.3).add(0, 0.45, 0);
                le.setDeltaMovement(knockback);
                le.hurtMarked = true;
            }
        }

        this.discard();
    }
}
