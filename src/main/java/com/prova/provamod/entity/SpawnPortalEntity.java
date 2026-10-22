package com.prova.provamod.entity;

import com.prova.provamod.portal.PortalMobConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class SpawnPortalEntity extends Entity implements TraceableEntity {

    public static final int MAX_DURATION = 320;      // 16 seconds (320 ticks)
    public static final int SPAWN_INTERVAL = 50;    // Spawn a mob every 2.5 seconds (50 ticks)
    public static final int INITIAL_DELAY = 20;     // 1 second warmup before first spawn

    private static final EntityDataAccessor<String> SELECTED_MOB_DATA =
            SynchedEntityData.defineId(SpawnPortalEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> PORTAL_YAW_DATA =
            SynchedEntityData.defineId(SpawnPortalEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private Entity cachedOwner;

    public SpawnPortalEntity(EntityType<? extends SpawnPortalEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SELECTED_MOB_DATA, "minecraft:zombie");
        builder.define(PORTAL_YAW_DATA, 0.0F);
    }

    public void setSelectedMobId(String mobId) {
        this.entityData.set(SELECTED_MOB_DATA, mobId);
    }

    public String getSelectedMobId() {
        return this.entityData.get(SELECTED_MOB_DATA);
    }

    public void setPortalYaw(float yaw) {
        this.entityData.set(PORTAL_YAW_DATA, yaw);
        this.setYRot(yaw);
    }

    public float getPortalYaw() {
        return this.entityData.get(PORTAL_YAW_DATA);
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
        Entity owner = this.cachedOwner;
        if (owner != null && !owner.isRemoved()) {
            return owner;
        }
        UUID ownerId = this.ownerUUID;
        if (ownerId != null && level() instanceof ServerLevel serverLevel) {
            Entity found = serverLevel.getEntity(ownerId);
            this.cachedOwner = found;
            return found;
        }
        return null;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.tickCount = input.getInt("Age").orElse(0);
        input.getString("SelectedMob").ifPresent(this::setSelectedMobId);
        input.getString("PortalYaw").ifPresent(yawStr -> {
            try {
                setPortalYaw(Float.parseFloat(yawStr));
            } catch (NumberFormatException ignored) {}
        });
        input.getString("Owner").ifPresent(uuidStr -> {
            try {
                this.ownerUUID = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {}
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Age", this.tickCount);
        output.putString("SelectedMob", getSelectedMobId());
        output.putString("PortalYaw", String.valueOf(getPortalYaw()));
        UUID ownerId = this.ownerUUID;
        if (ownerId != null) {
            output.putString("Owner", ownerId.toString());
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

        // Periodic mob summoning on server
        if (!level().isClientSide()) {
            int interval = PortalMobConfig.getSpawnInterval(getSelectedMobId());
            if (this.tickCount >= INITIAL_DELAY && (this.tickCount - INITIAL_DELAY) % interval == 0) {
                spawnSelectedMob();
            }
        } else {
            // Visual particles on client
            spawnClientVortexParticles();
        }

        // Ambient humming sound every 35 ticks
        if (this.tickCount % 35 == 0) {
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, 1.2F + (float) Math.sin(tickCount * 0.1) * 0.2F);
        }
    }

    private void spawnSelectedMob() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        String mobId = getSelectedMobId();
        Optional<EntityType<?>> optionalType = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(mobId));
        if (optionalType.isEmpty()) {
            return;
        }

        EntityType<?> entityType = optionalType.get();
        Entity spawnedEntity = entityType.create(serverLevel, EntitySpawnReason.SPAWNER);
        if (spawnedEntity == null) {
            return;
        }

        float portalYaw = getPortalYaw();
        double yawRad = Math.toRadians(portalYaw);
        // Forward unit vector in Minecraft yaw coordinates (yaw 0 = +Z, 90 = -X)
        double Fx = -Math.sin(yawRad);
        double Fz = Math.cos(yawRad);
        // Horizontal tangent vector (right)
        double Rx = Math.cos(yawRad);
        double Rz = Math.sin(yawRad);

        // Position mob slightly in front of the vertical portal so it emerges cleanly
        double forwardOffset = 1.25;
        double lateralJitter = (random.nextDouble() - 0.5) * 0.25;

        double spawnX = getX() + Fx * forwardOffset + Rx * lateralJitter;
        double spawnZ = getZ() + Fz * forwardOffset + Rz * lateralJitter;

        // Find ground level at spawn position so the mob stands cleanly on the ground
        double spawnY = getY() - 1.35;
        BlockPos checkStart = BlockPos.containing(spawnX, getY() + 0.5, spawnZ);
        for (int dy = 0; dy <= 5; dy++) {
            BlockPos check = checkStart.below(dy);
            var blockState = serverLevel.getBlockState(check);
            if (!blockState.isAir() && (!blockState.getCollisionShape(serverLevel, check).isEmpty() || blockState.isFaceSturdy(serverLevel, check, Direction.UP))) {
                spawnY = check.getY() + 1.0;
                break;
            }
        }

        // For entities with no gravity, allow them to emerge near the portal center
        if (spawnedEntity.isNoGravity()) {
            spawnY = getY() + (random.nextDouble() - 0.5) * 0.4;
        }

        spawnedEntity.setPos(spawnX, spawnY, spawnZ);
        spawnedEntity.setYRot(portalYaw);
        spawnedEntity.setYHeadRot(portalYaw);
        spawnedEntity.setYBodyRot(portalYaw);

        if (spawnedEntity instanceof Mob mob) {
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, serverLevel, serverLevel.getCurrentDifficultyAt(BlockPos.containing(spawnX, spawnY, spawnZ)), EntitySpawnReason.SPAWNER, null);
            // Give mob forward impulse out of the portal
            double forwardSpeed = 0.28;
            mob.setDeltaMovement(Fx * forwardSpeed, 0.12, Fz * forwardSpeed);
        }

        serverLevel.addFreshEntity(spawnedEntity);

        // Sound and burst effects
        serverLevel.playSound(null, getX(), getY(), getZ(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0F, 1.4F);
        serverLevel.playSound(null, getX(), getY(), getZ(),
                SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.8F, 1.3F);

        // Particle burst directed forward out of the portal face towards the mob
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX() + Fx * 0.4, getY(), getZ() + Fz * 0.4, 25, 0.2, 0.4, 0.2, 0.15);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX() + Fx * 0.5, getY(), getZ() + Fz * 0.5, 30, 0.3, 0.4, 0.3, 0.12);
        serverLevel.sendParticles(ParticleTypes.POOF, spawnX, spawnY + 0.2, spawnZ, 15, 0.2, 0.2, 0.2, 0.05);
    }

    private void spawnClientVortexParticles() {
        float portalYaw = getPortalYaw();
        double yawRad = Math.toRadians(portalYaw);

        // Forward normal vector
        double Fx = -Math.sin(yawRad);
        double Fz = Math.cos(yawRad);

        // Right tangent vector
        double Rx = Math.cos(yawRad);
        double Rz = Math.sin(yawRad);

        double radius = 1.35;
        int particleCount = 12;
        float progress = (float) this.tickCount * 0.15F;

        // 1. Vertical swirling rim particles (spanning Rx/Rz horizontally and Y vertically)
        for (int i = 0; i < particleCount; i++) {
            double angle = progress + (i * 2.0 * Math.PI / particleCount);
            double cosA = Math.cos(angle);
            double sinA = Math.sin(angle);

            // In the vertical plane: horizontal axis is Rx, Rz; vertical axis is Y
            double px = getX() + Rx * (cosA * radius);
            double py = getY() + sinA * radius;
            double pz = getZ() + Rz * (cosA * radius);

            // Swirling motion tangential to vertical circle, plus slight inward draw and forward flow
            double vx = -Rx * (sinA * 0.08) + (getX() - px) * 0.03 + Fx * 0.01;
            double vy = cosA * 0.08 + (getY() - py) * 0.03;
            double vz = -Rz * (sinA * 0.08) + (getZ() - pz) * 0.03 + Fz * 0.01;

            level().addParticle(ParticleTypes.PORTAL, px, py, pz, vx, vy, vz);

            if (random.nextFloat() < 0.35F) {
                level().addParticle(ParticleTypes.END_ROD, px, py, pz, Fx * 0.01, 0.005, Fz * 0.01);
            }
            if (random.nextFloat() < 0.25F) {
                level().addParticle(ParticleTypes.WITCH, px, py, pz, 0, 0.01, 0);
            }
        }

        // 2. Inner vertical disk energy / cosmic vortex
        int diskParticles = 4;
        for (int j = 0; j < diskParticles; j++) {
            if (random.nextFloat() < 0.75F) {
                double r = Math.sqrt(random.nextDouble()) * (radius * 0.9);
                double phi = random.nextDouble() * 2.0 * Math.PI;
                double cosP = Math.cos(phi);
                double sinP = Math.sin(phi);

                double ix = getX() + Rx * (cosP * r) + Fx * ((random.nextDouble() - 0.5) * 0.15);
                double iy = getY() + sinP * r;
                double iz = getZ() + Rz * (cosP * r) + Fz * ((random.nextDouble() - 0.5) * 0.15);

                double ivx = (getX() - ix) * 0.04 + Fx * 0.02;
                double ivy = (getY() - iy) * 0.04;
                double ivz = (getZ() - iz) * 0.04 + Fz * 0.02;

                level().addParticle(ParticleTypes.REVERSE_PORTAL, ix, iy, iz, ivx, ivy, ivz);

                if (random.nextFloat() < 0.3F) {
                    level().addParticle(ParticleTypes.ENCHANT, ix, iy, iz,
                            (random.nextDouble() - 0.5) * 0.2, 0.05, (random.nextDouble() - 0.5) * 0.2);
                }
            }
        }

        // 3. Central singularity spark
        if (random.nextFloat() < 0.3F) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    getX() + Fx * 0.05, getY(), getZ() + Fz * 0.05,
                    (random.nextDouble() - 0.5) * 0.05,
                    (random.nextDouble() - 0.5) * 0.05,
                    (random.nextDouble() - 0.5) * 0.05);
        }
    }

    private void collapseAndDie() {
        if (!level().isClientSide() && level() instanceof ServerLevel serverLevel) {
            float portalYaw = getPortalYaw();
            double yawRad = Math.toRadians(portalYaw);
            double Fx = -Math.sin(yawRad);
            double Fz = Math.cos(yawRad);

            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.PORTAL, getX(), getY(), getZ(), 60, 0.4, 0.8, 0.4, 0.25);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX() + Fx * 0.3, getY(), getZ() + Fz * 0.3, 40, 0.4, 0.6, 0.4, 0.2);
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.2F, 0.8F);
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        discard();
    }
}
