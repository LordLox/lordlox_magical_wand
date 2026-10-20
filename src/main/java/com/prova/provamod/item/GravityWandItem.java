package com.prova.provamod.item;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public class GravityWandItem extends Item {

    public GravityWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            double range = 10.0;
            AABB area = player.getBoundingBox().inflate(range);
            List<LivingEntity> targets = level.getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    e -> e != player && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand)
            );

            // Ordina i bersagli partendo da quelli più vicini al giocatore
            targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

            // Se la durabilità rimanente è inferiore al numero di entità, limita i bersagli a quelli sostenibili
            int maxAllowed = targets.size();
            if (!player.hasInfiniteMaterials() && itemStack.isDamageableItem()) {
                int remainingDurability = itemStack.getMaxDamage() - itemStack.getDamageValue();
                if (remainingDurability < maxAllowed) {
                    maxAllowed = Math.max(0, remainingDurability);
                }
            }

            List<LivingEntity> affectedTargets = targets.subList(0, maxAllowed);

            if (player.isShiftKeyDown()) {
                // Shift + Tasto Destro: Esplosione nemici con particelle di sangue ed esplosione
                if (level instanceof ServerLevel serverLevel) {
                    for (LivingEntity target : affectedTargets) {
                        double tx = target.getX();
                        double ty = target.getY() + target.getBbHeight() / 2.0;
                        double tz = target.getZ();

                        // Particelle di esplosione
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION, tx, ty, tz, 2, 0.2, 0.2, 0.2, 0.0);
                        serverLevel.sendParticles(ParticleTypes.SMOKE, tx, ty, tz, 15, 0.3, 0.3, 0.3, 0.05);
                        serverLevel.sendParticles(ParticleTypes.FLAME, tx, ty, tz, 8, 0.2, 0.2, 0.2, 0.05);

                        // Particelle di sangue (schizzi di sangue rosso vivo, grumi scuri e cuori di danno)
                        serverLevel.sendParticles(
                                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                                tx, ty, tz, 45, 0.4, 0.4, 0.4, 0.2);
                        serverLevel.sendParticles(
                                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHER_WART_BLOCK.defaultBlockState()),
                                tx, ty, tz, 25, 0.4, 0.4, 0.4, 0.15);
                        serverLevel.sendParticles(
                                new ItemParticleOption(ParticleTypes.ITEM, Items.REDSTONE),
                                tx, ty, tz, 20, 0.3, 0.3, 0.3, 0.1);
                        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, tx, ty, tz, 10, 0.3, 0.3, 0.3, 0.1);

                        // Suono di esplosione
                        level.playSound(null, tx, ty, tz,
                                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.3F);

                        // Morte del bersaglio
                        target.invulnerableTime = 0;
                        target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), Float.MAX_VALUE);
                        if (target.isAlive()) {
                            target.kill(serverLevel);
                        }
                    }

                    if (affectedTargets.isEmpty()) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.4F);
                    }
                }
            } else {
                // Regular Right-Click: Zero-Gravity Lift
                for (LivingEntity target : affectedTargets) {
                    target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 1));
                    target.setDeltaMovement(target.getDeltaMovement().add(0, 0.45, 0));
                    target.hurtMarked = true;
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.2F);
            }

            player.getCooldowns().addCooldown(itemStack, 30);

            if (!affectedTargets.isEmpty()) {
                itemStack.hurtAndBreak(affectedTargets.size(), player, hand);
            } else if (!player.hasInfiniteMaterials() && itemStack.isDamageableItem() && itemStack.getMaxDamage() - itemStack.getDamageValue() <= 0) {
                itemStack.hurtAndBreak(1, player, hand);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
