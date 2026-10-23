package com.lordlox.magicalwands.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class GravityWandItem extends Item {

    public static final double PULL_RANGE = 14.0;
    public static final int LIFT_COOLDOWN = 120; // 6 secondi: impedisce lo spam infinito della levitazione
    public static final int PULL_COOLDOWN = 20;  // 1 secondo: trazione gravitazionale rapida e reattiva

    public GravityWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            double range = PULL_RANGE;
            double rangeSq = range * range;
            AABB area = player.getBoundingBox().inflate(range);
            List<LivingEntity> targets = level.getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    e -> e != null && e != player && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand)
                            && (!(e instanceof Player p) || !p.isCreative())
                            && e.distanceToSqr(player) <= rangeSq
            );

            // Ordina i bersagli partendo da quelli più vicini al giocatore
            targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

            if (player.isShiftKeyDown()) {
                // Shift + Tasto Destro: Attira tutti i mob verso il giocatore con effetti gravitazionali
                if (level instanceof ServerLevel serverLevel) {
                    Vec3 playerCenter = player.position().add(0, player.getBbHeight() * 0.5, 0);

                    // Effetto vortice gravitazionale attorno al giocatore (centro di attrazione)
                    for (int i = 0; i < 24; i++) {
                        double angle = (i / 24.0) * (2.0 * Math.PI);
                        double rx = Math.cos(angle);
                        double rz = Math.sin(angle);
                        // Anello di particelle cosmiche che implodono verso il giocatore
                        serverLevel.sendParticles(ParticleTypes.PORTAL, player.getX() + rx * 2.2, player.getY() + 0.2, player.getZ() + rz * 2.2, 1, -rx * 0.6, 0.1, -rz * 0.6, 0.5);
                        serverLevel.sendParticles(ParticleTypes.WITCH, player.getX() + rx * 1.6, player.getY() + 0.3, player.getZ() + rz * 1.6, 1, 0, 0.04, 0, 0.02);
                    }
                    serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.8, player.getZ(), 20, 0.4, 0.4, 0.4, 0.08);
                    serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.6, 0.5, 0.6, 0.5);
                    serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.3, 0.4, 0.3, 0.04);

                    for (LivingEntity target : targets) {
                        Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
                        Vec3 toPlayer = playerCenter.subtract(targetCenter);
                        double dist = toPlayer.length();

                        if (dist > 0.01) {
                            Vec3 dir = toPlayer.normalize();

                            // Calcola la forza di attrazione proporzionata alla distanza
                            double speed;
                            if (dist < 2.0) {
                                speed = Math.clamp(dist * 0.25, 0.20, 0.45);
                            } else {
                                speed = Math.clamp(0.40 + dist * 0.075, 0.40, 1.30);
                            }

                            // Componente verticale per sollevare leggermente il mob ed evitare che l'attrito del terreno blocchi la trazione
                            double vy = dir.y * speed + 0.26;
                            if (player.getY() >= target.getY()) {
                                vy = Math.max(0.20, vy);
                            }
                            vy = Math.clamp(vy, -0.75, 0.60);

                            Vec3 newMotion = new Vec3(dir.x * speed, vy, dir.z * speed);
                            target.setDeltaMovement(newMotion);
                            target.hurtMarked = true;
                            target.resetFallDistance();

                            if (target instanceof ServerPlayer serverTarget) {
                                serverTarget.connection.send(new ClientboundSetEntityMotionPacket(serverTarget));
                            }

                            // Fascio di energia gravitazionale (stream di particelle) tra mob e giocatore
                            int steps = (int) Math.min(14, Math.max(5, dist * 1.4));
                            for (int step = 1; step < steps; step++) {
                                double t = (double) step / steps;
                                double lx = targetCenter.x + toPlayer.x * t;
                                double ly = targetCenter.y + toPlayer.y * t + Math.sin(t * Math.PI) * 0.6;
                                double lz = targetCenter.z + toPlayer.z * t;

                                serverLevel.sendParticles(ParticleTypes.PORTAL, lx, ly, lz, 1, dir.x * 0.2, dir.y * 0.2, dir.z * 0.2, 0.15);
                                if (step % 2 == 0) {
                                    serverLevel.sendParticles(ParticleTypes.WITCH, lx, ly, lz, 1, 0.02, 0.02, 0.02, 0.01);
                                }
                                if (step % 4 == 0) {
                                    serverLevel.sendParticles(ParticleTypes.END_ROD, lx, ly, lz, 1, 0.01, 0.01, 0.01, 0.01);
                                }
                            }

                            // Particelle di scatto/sollevamento alla posizione del mob
                            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 0.5, target.getZ(), 10, 0.25, 0.25, 0.25, 0.08);
                            serverLevel.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 0.5, target.getZ(), 6, 0.2, 0.2, 0.2, 0.04);
                            serverLevel.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 0.4, target.getZ(), 3, 0.15, 0.15, 0.15, 0.03);
                        }
                    }

                    if (!targets.isEmpty()) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.4F);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.75F);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.6F, 1.2F);
                    } else {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.6F);
                    }
                }
            } else {
                // Regular Right-Click: Zero-Gravity Lift
                for (LivingEntity target : targets) {
                    target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 1));
                    target.setDeltaMovement(target.getDeltaMovement().add(0, 0.45, 0));
                    target.hurtMarked = true;
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 0.5, target.getZ(), 10, 0.2, 0.3, 0.2, 0.1);
                        serverLevel.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 0.2, target.getZ(), 4, 0.15, 0.2, 0.15, 0.03);
                    }
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.2F);
            }

            int cooldown = player.isShiftKeyDown() ? PULL_COOLDOWN : LIFT_COOLDOWN;
            player.getCooldowns().addCooldown(itemStack, cooldown);

            if (!player.hasInfiniteMaterials() && itemStack.isDamageableItem()) {
                itemStack.hurtAndBreak(1, player, hand);
            }
        }

        player.swing(hand);
        return InteractionResult.SUCCESS;
    }
}
