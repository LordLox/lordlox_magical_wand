package com.prova.provamod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());

            if (player.isShiftKeyDown()) {
                // Sneak + Right-Click: Gravity Pull towards player
                for (LivingEntity target : targets) {
                    Vec3 direction = player.position().subtract(target.position()).normalize().scale(1.2);
                    target.setDeltaMovement(direction.x, 0.25, direction.z);
                    target.hurtMarked = true;
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
            } else {
                // Regular Right-Click: Zero-Gravity Lift
                for (LivingEntity target : targets) {
                    target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 1));
                    target.setDeltaMovement(target.getDeltaMovement().add(0, 0.45, 0));
                    target.hurtMarked = true;
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.2F);
            }

            player.getCooldowns().addCooldown(itemStack, 30);
        }

        return InteractionResult.SUCCESS;
    }
}
