package com.prova.provamod.item;

import com.prova.provamod.Prova;
import com.prova.provamod.entity.BlackHoleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class BlackHoleWandItem extends Item {

    public static final double MAX_CAST_DISTANCE = 18.0;

    public BlackHoleWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 maxReachPos = eyePos.add(lookVec.scale(MAX_CAST_DISTANCE));

        // Raycast to check if looking at a block within max distance
        ClipContext clipContext = new ClipContext(
                eyePos,
                maxReachPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        );
        BlockHitResult hitResult = level.clip(clipContext);

        Vec3 spawnPos;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // Looking at a block: place the black hole slightly in front of the hit face
            var dir = hitResult.getDirection();
            Vec3 normal = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
            spawnPos = hitResult.getLocation().add(normal.scale(0.9));
        } else {
            // Looking into the air: spawn at the maximum cast distance limit
            spawnPos = maxReachPos;
        }

        if (!level.isClientSide()) {
            BlackHoleEntity blackHole = new BlackHoleEntity(Prova.BLACK_HOLE.get(), level);
            blackHole.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            blackHole.setOwner(player);
            level.addFreshEntity(blackHole);

            // Cooldown matching duration (7 seconds = 140 ticks)
            player.getCooldowns().addCooldown(itemStack, BlackHoleEntity.MAX_DURATION);

            // Damage item if not in creative
            if (!player.hasInfiniteMaterials() && itemStack.isDamageableItem()) {
                itemStack.hurtAndBreak(1, player, hand);
            }

            // Casting sounds
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.6F);
            level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                    SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.6F, 0.5F);
            level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                    SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.4F, 0.6F);
        }

        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipOutput, TooltipFlag flag) {
        tooltipOutput.accept(Component.translatable("item.prova.black_hole_wand.desc1").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltipOutput.accept(Component.translatable("item.prova.black_hole_wand.desc2").withStyle(ChatFormatting.GRAY));
        tooltipOutput.accept(Component.translatable("item.prova.black_hole_wand.desc3").withStyle(ChatFormatting.DARK_AQUA));
    }
}
