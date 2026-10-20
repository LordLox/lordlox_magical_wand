package com.prova.provamod.entity;

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
    }

    public void setSelectedMobId(String mobId) {
        this.entityData.set(SELECTED_MOB_DATA, mobId);
    }

    public String getSelectedMobId() {
        return this.entityData.get(SELECTED_MOB_DATA);
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
            if (this.tickCount >= INITIAL_DELAY && (this.tickCount - INITIAL_DELAY) % SPAWN_INTERVAL == 0) {
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

    @SuppressWarnings("deprecation")
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

        // Position mob just beneath the portal ring
        double spawnX = getX() + (random.nextDouble() - 0.5) * 0.4;
        double spawnY = getY() - 0.3;
        double spawnZ = getZ() + (random.nextDouble() - 0.5) * 0.4;

        spawnedEntity.setPos(spawnX, spawnY, spawnZ);
        spawnedEntity.setYRot(random.nextFloat() * 360.0F);

        if (spawnedEntity instanceof Mob mob) {
            mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()), EntitySpawnReason.SPAWNER, null);
            mob.setDeltaMovement(0, -0.2, 0);
        }

        serverLevel.addFreshEntity(spawnedEntity);

        // Sound and burst effects
        serverLevel.playSound(null, getX(), getY(), getZ(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0F, 1.4F);
        serverLevel.playSound(null, getX(), getY(), getZ(),
                SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.8F, 1.3F);

        // Particle burst
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 20, 0.2, 0.2, 0.2, 0.1);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 30, 0.3, 0.3, 0.3, 0.15);
        serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() - 0.2, getZ(), 15, 0.2, 0.1, 0.2, 0.05);
    }

    private void spawnClientVortexParticles() {
        double radius = 1.25;
        int particleCount = 8;
        float progress = (float) this.tickCount * 0.12F;

        for (int i = 0; i < particleCount; i++) {
            double angle = progress + (i * 2.0 * Math.PI / particleCount);
            double px = getX() + Math.cos(angle) * radius;
            double pz = getZ() + Math.sin(angle) * radius;
            double py = getY() + Math.sin(progress * 1.5 + i) * 0.15;

            // Swirling motion directed inward and upward
            double vx = -Math.sin(angle) * 0.08 + (getX() - px) * 0.05;
            double vz = Math.cos(angle) * 0.08 + (getZ() - pz) * 0.05;
            double vy = 0.02;

            level().addParticle(ParticleTypes.PORTAL, px, py, pz, vx, vy, vz);
            if (random.nextFloat() < 0.35F) {
                level().addParticle(ParticleTypes.END_ROD, px, py, pz, 0, 0.01, 0);
            }
            if (random.nextFloat() < 0.25F) {
                level().addParticle(ParticleTypes.WITCH, px, py, pz, 0, -0.02, 0);
            }
        }

        // Center beacon / singularity energy
        if (random.nextFloat() < 0.6F) {
            double coreOffset = (random.nextDouble() - 0.5) * 0.3;
            level().addParticle(ParticleTypes.ENCHANT, getX() + coreOffset, getY() + (random.nextDouble() - 0.5) * 0.3, getZ() + coreOffset,
                    (random.nextDouble() - 0.5) * 0.2, 0.1, (random.nextDouble() - 0.5) * 0.2);
        }
    }

    private void collapseAndDie() {
        if (!level().isClientSide() && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.PORTAL, getX(), getY(), getZ(), 60, 0.5, 0.5, 0.5, 0.3);
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.2F, 0.8F);
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        discard();
    }
}
