package com.prova.provamod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
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
    public static final double BLOCK_PULL_RADIUS = 7.0;
    public static final int MAX_ORBITING_BLOCKS = 32;

    private static final Direction[] DIRECTIONS = Direction.values();

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.tickCount = input.getInt("Age").orElse(0);
        input.getString("Owner").ifPresent(uuidStr -> {
            try {
                this.ownerUUID = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed UUID string
            }
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Age", this.tickCount);
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
        RandomSource rand = this.getRandom();
        int age = this.tickCount;

        // 1. Singularity Event Horizon: dense black sphere
        for (int i = 0; i < 18; i++) {
            double theta = rand.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * rand.nextDouble() - 1.0);
            double r = 0.45 + rand.nextDouble() * 0.25;
            double px = center.x + r * Math.sin(phi) * Math.cos(theta);
            double py = center.y + r * Math.cos(phi);
            double pz = center.z + r * Math.sin(phi) * Math.sin(theta);

            if (rand.nextBoolean()) {
                level().addParticle(ParticleTypes.SQUID_INK, px, py, pz, 0, 0, 0);
            } else {
                level().addParticle(ParticleTypes.SMOKE, px, py, pz, 0, 0, 0);
            }
        }

        // 2. Accretion Disk: spinning galaxy disk around the singularity
        int diskCount = 22;
        double spinAngle = age * 0.22;
        for (int i = 0; i < diskCount; i++) {
            double radius = 0.75 + Math.pow(rand.nextDouble(), 1.4) * 2.8;
            double angle = spinAngle + (rand.nextDouble() * 2.0 * Math.PI);

            double diskX = radius * Math.cos(angle);
            double diskZ = radius * Math.sin(angle);
            double diskY = (rand.nextDouble() - 0.5) * 0.2 * (radius / 3.0);

            // Tangential orbital velocity plus inward gravitational drain
            double orbitalSpeed = 0.15 * Math.sqrt(1.6 / radius);
            double inwardSpeed = 0.05;
            double vx = -Math.sin(angle) * orbitalSpeed - Math.cos(angle) * inwardSpeed;
            double vz = Math.cos(angle) * orbitalSpeed - Math.sin(angle) * inwardSpeed;
            double vy = -diskY * 0.08;

            double roll = rand.nextDouble();
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
            double spreadX = (rand.nextDouble() - 0.5) * 0.05;
            double spreadZ = (rand.nextDouble() - 0.5) * 0.05;

            // North pole jet (Upwards)
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.35, center.z, spreadX, 0.24, spreadZ);
            level().addParticle(ParticleTypes.END_ROD, center.x, center.y + 0.45, center.z, spreadX * 0.4, 0.30, spreadZ * 0.4);

            // South pole jet (Downwards)
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y - 0.35, center.z, spreadX, -0.24, spreadZ);
            level().addParticle(ParticleTypes.END_ROD, center.x, center.y - 0.45, center.z, spreadX * 0.4, -0.30, spreadZ * 0.4);
        }

        // 4. Infalling Space Distortion: particles rushing into the vortex from afar
        for (int k = 0; k < 12; k++) {
            double dist = 4.0 + rand.nextDouble() * 4.5;
            double theta = rand.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * rand.nextDouble() - 1.0);
            double sx = center.x + dist * Math.sin(phi) * Math.cos(theta);
            double sy = center.y + dist * Math.cos(phi);
            double sz = center.z + dist * Math.sin(phi) * Math.sin(theta);

            Vec3 inwards = center.subtract(sx, sy, sz).normalize().scale(0.38 + rand.nextDouble() * 0.22);
            if (rand.nextBoolean()) {
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
     * Server physics: pulls entities, blocks & fluids (excluding owner) and deals continuous damage inside core.
     */
    private void tickGravitationalPullAndDamage() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        Vec3 center = this.position().add(0, 0.5, 0);
        AABB pullBox = getBoundingBox().inflate(GRAVITY_RADIUS);

        // Periodically tear nearby blocks and fluids from the world and turn them into orbiting falling blocks
        if (this.tickCount % 2 == 0) {
            List<FallingBlockEntity> orbitingBlocks = serverLevel.getEntitiesOfClass(
                    FallingBlockEntity.class,
                    pullBox
            );
            orbitingBlocks.removeIf(b -> b == null || !b.isAlive());

            if (orbitingBlocks.size() < MAX_ORBITING_BLOCKS) {
                int blocksToRip = (orbitingBlocks.size() < 8) ? 2 : 1;
                for (int i = 0; i < blocksToRip; i++) {
                    BlockPos candidate = findRipCandidate(serverLevel, center);
                    if (candidate != null) {
                        ripBlockIntoVortex(serverLevel, candidate, center);
                    }
                }
            }
        }

        final UUID currentOwner = this.ownerUUID;
        List<Entity> affectedEntities = serverLevel.getEntities(this, pullBox, e -> {
            if (e == null || e == this) return false;
            // The player who cast it is completely exempt from pull and damage
            if (currentOwner != null && e.getUUID().equals(currentOwner)) return false;
            if (e instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
            if (e instanceof LivingEntity living && !living.isAlive()) return false;
            return e instanceof LivingEntity || e instanceof ItemEntity || e instanceof FallingBlockEntity;
        });

        for (Entity target : affectedEntities) {
            Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);
            Vec3 toCenter = center.subtract(targetCenter);
            double dist = toCenter.length();

            if (dist > GRAVITY_RADIUS) continue;

            // Gravitational attraction force (stronger closer to singularity)
            double normalizedDist = Math.max(0.1, dist / GRAVITY_RADIUS);
            double pullSpeed = Mth.clamp(0.50 * (1.0 - normalizedDist) + 0.12, 0.08, 0.72);

            Vec3 pullDir = toCenter.normalize().scale(pullSpeed);
            // Tangential vortex swirl around vertical axis
            Vec3 swirl = new Vec3(-toCenter.z, 0, toCenter.x).normalize().scale(pullSpeed * 0.45);

            // Special handling for FallingBlockEntity:
            // Keep time at 1 so it never despawns/drops prematurely,
            // ease inward pull near core to form a magnificent debris belt,
            // and emit fluid particles for swirling liquids!
            if (target instanceof FallingBlockEntity fb) {
                fb.time = 1;
                if (dist < 2.2) {
                    pullDir = pullDir.scale(0.15); // soft core buffer to maintain orbit
                }

                // Swirling particles for fluids in the vortex
                if (fb.getBlockState().is(Blocks.WATER)) {
                    serverLevel.sendParticles(ParticleTypes.SPLASH, fb.getX(), fb.getY() + 0.3, fb.getZ(), 2, 0.15, 0.15, 0.15, 0.05);
                    serverLevel.sendParticles(ParticleTypes.FALLING_WATER, fb.getX(), fb.getY() + 0.1, fb.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
                } else if (fb.getBlockState().is(Blocks.LAVA)) {
                    serverLevel.sendParticles(ParticleTypes.LAVA, fb.getX(), fb.getY() + 0.3, fb.getZ(), 2, 0.15, 0.15, 0.15, 0.02);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, fb.getX(), fb.getY() + 0.3, fb.getZ(), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }

            Vec3 currentVelocity = target.getDeltaMovement();
            Vec3 newVelocity = currentVelocity.scale(0.78).add(pullDir).add(swirl);

            if (target instanceof FallingBlockEntity) {
                // Counteract vanilla gravity (-0.04) so blocks stay suspended and swirling
                newVelocity = newVelocity.add(0, 0.044, 0);
            }

            target.setDeltaMovement(newVelocity);
            target.hurtMarked = true;
            target.resetFallDistance();

            // Continuous damage inside the core
            if (dist <= CORE_RADIUS && target instanceof LivingEntity living) {
                if (this.tickCount % 4 == 0) {
                    living.invulnerableTime = 0;
                    Entity owner = getOwner();
                    DamageSource source = (owner != null)
                            ? serverLevel.damageSources().indirectMagic(this, owner)
                            : serverLevel.damageSources().magic();
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
     * Looks for valid surface blocks or fluids within pull radius to tear from the earth.
     */
    @Nullable
    private BlockPos findRipCandidate(ServerLevel level, Vec3 center) {
        RandomSource rand = this.getRandom();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = rand.nextDouble() * 2.0 * Math.PI;
            double r = 1.2 + rand.nextDouble() * (BLOCK_PULL_RADIUS - 1.2);
            double yOffset = (rand.nextDouble() * 2.0 - 1.0) * (BLOCK_PULL_RADIUS * 0.75);

            BlockPos targetPos = BlockPos.containing(
                    center.x + r * Math.cos(angle),
                    center.y + yOffset,
                    center.z + r * Math.sin(angle)
            );

            BlockState state = level.getBlockState(targetPos);
            if (canRipBlock(level, targetPos, state)) {
                return targetPos;
            }
        }
        return null;
    }

    /**
     * Determines whether a block or fluid can be pulled into the singularity.
     * Bedrock and indestructible blocks are strictly immune.
     */
    private boolean canRipBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        // Bedrock and unbreakable block protection: completely immune
        if (state.is(Blocks.BEDROCK) || state.is(BlockTags.WITHER_IMMUNE)) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        // Do not rip tile entities / containers to protect inventories
        if (state.hasBlockEntity()) return false;

        // Fluid blocks (Water, Lava): pulled if exposed to air or surface
        if (!state.getFluidState().isEmpty()) {
            for (Direction dir : DIRECTIONS) {
                BlockPos adjacent = pos.relative(dir);
                BlockState adjState = level.getBlockState(adjacent);
                if (adjState.isAir() || adjState.getFluidState().isEmpty() || !adjState.canOcclude()) {
                    return true;
                }
            }
            return false;
        }

        // Solid blocks: must have at least one exposed face (adjacent to air, fluid, or non-occluding block)
        for (Direction dir : DIRECTIONS) {
            BlockPos adjacent = pos.relative(dir);
            BlockState adjState = level.getBlockState(adjacent);
            if (adjState.isAir() || !adjState.getFluidState().isEmpty() || !adjState.canOcclude()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts a world block or fluid into a FallingBlockEntity propelled into the black hole.
     */
    private void ripBlockIntoVortex(ServerLevel level, BlockPos pos, Vec3 center) {
        BlockState state = level.getBlockState(pos);
        boolean isFluid = !state.getFluidState().isEmpty();

        if (isFluid) {
            boolean isWater = state.getFluidState().is(FluidTags.WATER);
            boolean isLava = state.getFluidState().is(FluidTags.LAVA);

            // Remove fluid block from world
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

            // Fluid-specific sound and particles
            if (isWater) {
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.2F);
                level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.1);
            } else if (isLava) {
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
            }

            BlockState fluidToFall = isWater ? Blocks.WATER.defaultBlockState() : (isLava ? Blocks.LAVA.defaultBlockState() : state);
            FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level, pos, fluidToFall);
            if (fallingBlock != null) {
                fallingBlock.time = 1;
                fallingBlock.dropItem = false;

                Vec3 blockCenter = Vec3.atCenterOf(pos);
                Vec3 toCenter = center.subtract(blockCenter).normalize().scale(0.42);
                fallingBlock.setDeltaMovement(toCenter.add(0, 0.32, 0));
                fallingBlock.hurtMarked = true;
            }

            // Ensure source position is cleared
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            // Sound & particles of the solid block being torn out of the earth
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));

            // Spawn falling block entity
            FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level, pos, state);
            if (fallingBlock != null) {
                fallingBlock.time = 1;
                fallingBlock.dropItem = true;

                Vec3 blockCenter = Vec3.atCenterOf(pos);
                Vec3 toCenter = center.subtract(blockCenter).normalize().scale(0.38);
                fallingBlock.setDeltaMovement(toCenter.add(0, 0.30, 0));
                fallingBlock.hurtMarked = true;
            }
        }
    }

    /**
     * Implosion / collapse when the black hole expires.
     * Launches all orbiting blocks and fluids and knocks back living entities.
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

            // 1. Fling all orbiting falling blocks and fluids outward and upward into the surroundings!
            List<FallingBlockEntity> orbitingBlocks = serverLevel.getEntitiesOfClass(
                    FallingBlockEntity.class,
                    getBoundingBox().inflate(GRAVITY_RADIUS)
            );

            RandomSource rand = this.getRandom();
            for (FallingBlockEntity fb : orbitingBlocks) {
                if (fb == null || !fb.isAlive()) continue;
                Vec3 outward = fb.position().subtract(center);
                Vec3 horiz;
                if (outward.x * outward.x + outward.z * outward.z < 0.04) {
                    double randAngle = rand.nextDouble() * 2.0 * Math.PI;
                    horiz = new Vec3(Math.cos(randAngle), 0, Math.sin(randAngle));
                } else {
                    horiz = new Vec3(outward.x, 0, outward.z).normalize();
                }

                // Explosive fling velocity
                double horizSpeed = 0.65 + rand.nextDouble() * 0.75; // 0.65 to 1.40 blocks/tick
                double upwardSpeed = 0.50 + rand.nextDouble() * 0.55; // 0.50 to 1.05 upward arc
                Vec3 flingMotion = horiz.scale(horizSpeed).add(0, upwardSpeed, 0);

                fb.setDeltaMovement(flingMotion);
                fb.hurtMarked = true;
                fb.time = 1; // Fresh timer so they stay intact while flying and landing

                boolean isFluidBlock = fb.getBlockState().is(Blocks.WATER) || fb.getBlockState().is(Blocks.LAVA);
                fb.dropItem = !isFluidBlock; // solid blocks drop as items if obstructed, fluids place or dissipate
            }

            // 2. Shockwave burst damage & knockback for living entities
            AABB finalArea = getBoundingBox().inflate(5.0);
            final UUID currentOwner = this.ownerUUID;
            List<LivingEntity> trapped = serverLevel.getEntitiesOfClass(LivingEntity.class, finalArea, e -> {
                if (e == null) return false;
                if (currentOwner != null && e.getUUID().equals(currentOwner)) return false;
                if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
                return e.isAlive();
            });

            Entity owner = getOwner();
            DamageSource damageSource = (owner != null)
                    ? serverLevel.damageSources().indirectMagic(this, owner)
                    : serverLevel.damageSources().magic();

            for (LivingEntity le : trapped) {
                le.invulnerableTime = 0;
                le.hurtServer(serverLevel, damageSource, 8.0F);
                Vec3 knockback = le.position().subtract(center).normalize().scale(1.3).add(0, 0.45, 0);
                le.setDeltaMovement(knockback);
                le.hurtMarked = true;
            }
        }

        this.discard();
    }
}
