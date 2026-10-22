package com.prova.provamod.item;

import com.prova.provamod.Prova;
import com.prova.provamod.client.gui.SpawnPortalWandClientHelper;
import com.prova.provamod.entity.SpawnPortalEntity;
import com.prova.provamod.portal.PortalMobConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

public class SpawnPortalWandItem extends Item {

    public static final double MAX_CAST_DISTANCE = 20.0;
    public static final int DEFAULT_DIAMONDS_REQUIRED = 2;

    public SpawnPortalWandItem(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Sneak + Right Click: Open the 3D Mob Card Selection Screen
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                String currentMob = itemStack.get(Prova.SELECTED_MOB.get());
                List<String> favorites = itemStack.get(Prova.FAVORITE_MOBS.get());
                SpawnPortalWandClientHelper.openScreen(hand, currentMob, favorites);
            }
            return InteractionResult.SUCCESS;
        }

        // Normal Right Click: Summon the Spawn Portal
        String selectedMob = itemStack.get(Prova.SELECTED_MOB.get());
        if (selectedMob == null || selectedMob.isEmpty()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(
                        Component.translatable("item.prova.spawn_portal_wand.no_mob_selected")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                );
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResult.FAIL;
        }

        boolean isCreative = player.hasInfiniteMaterials();
        int diamondCost = PortalMobConfig.getDiamondCost(selectedMob);

        // Check if player has at least diamondCost diamonds
        if (!isCreative) {
            int diamondCount = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(Items.DIAMOND)) {
                    diamondCount += stack.getCount();
                }
            }

            if (diamondCount < diamondCost) {
                if (!level.isClientSide()) {
                    player.sendOverlayMessage(
                            Component.translatable("item.prova.spawn_portal_wand.need_diamonds", diamondCost)
                                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    );
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                return InteractionResult.FAIL;
            }
        }

        // Calculate portal position: ensure it floats nicely near the ground (~2.3 blocks above ground)
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 maxReachPos = eyePos.add(lookVec.scale(MAX_CAST_DISTANCE));

        ClipContext clipContext = new ClipContext(
                eyePos,
                maxReachPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        );
        BlockHitResult hitResult = level.clip(clipContext);

        Vec3 targetPos = (hitResult.getType() == HitResult.Type.BLOCK) ? hitResult.getLocation() : eyePos.add(lookVec.scale(12.0));

        // Find ground level directly beneath target position
        BlockPos targetBlock = BlockPos.containing(targetPos);
        BlockPos groundBlock = targetBlock;
        for (int dy = 0; dy < 16; dy++) {
            BlockPos check = targetBlock.below(dy);
            var state = level.getBlockState(check);
            if ((!state.isAir() && state.blocksMotion()) || !state.getFluidState().isEmpty()) {
                groundBlock = check;
                break;
            }
        }
        var targetState = level.getBlockState(targetBlock);
        if (!targetState.isAir() && targetState.blocksMotion()) {
            groundBlock = targetBlock;
        }

        // Position vertical portal so its base (radius 1.35) rests right at ground level (+1.0)
        Vec3 portalSpawnPos = new Vec3(targetPos.x, groundBlock.getY() + 2.35, targetPos.z);

        if (!level.isClientSide()) {
            // Consume diamonds
            if (!isCreative) {
                int toConsume = diamondCost;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (invStack.is(Items.DIAMOND)) {
                        int take = Math.min(toConsume, invStack.getCount());
                        invStack.shrink(take);
                        toConsume -= take;
                        if (toConsume <= 0) {
                            break;
                        }
                    }
                }
            }

            // Spawn portal entity facing the cast direction
            SpawnPortalEntity portal = new SpawnPortalEntity(Prova.SPAWN_PORTAL.get(), level);
            portal.setPos(portalSpawnPos.x, portalSpawnPos.y, portalSpawnPos.z);
            portal.setPortalYaw(player.getYRot());
            portal.setOwner(player);
            portal.setSelectedMobId(selectedMob);
            level.addFreshEntity(portal);

            // Cooldown and durability
            player.getCooldowns().addCooldown(itemStack, 100); // 5 seconds cooldown
            if (!isCreative && itemStack.isDamageableItem()) {
                itemStack.hurtAndBreak(1, player, hand);
            }

            // Casting sounds
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.7F);
            level.playSound(null, portalSpawnPos.x, portalSpawnPos.y, portalSpawnPos.z,
                    SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.4F, 0.7F);
            level.playSound(null, portalSpawnPos.x, portalSpawnPos.y, portalSpawnPos.z,
                    SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.2F, 1.1F);
        }

        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipOutput, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltipOutput, flag);

        String selectedMob = stack.get(Prova.SELECTED_MOB.get());
        if (selectedMob != null && !selectedMob.isEmpty()) {
            Identifier id = Identifier.tryParse(selectedMob);
            Component mobName = id != null ? BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                    .map(type -> type.getDescription())
                    .orElseGet(() -> Component.translatable("entity." + selectedMob.replace(':', '.')))
                    : Component.translatable("entity." + selectedMob.replace(':', '.'));

            tooltipOutput.accept(
                    Component.translatable("item.prova.spawn_portal_wand.selected", mobName)
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            );

            int cost = PortalMobConfig.getDiamondCost(selectedMob);
            float intervalSec = PortalMobConfig.getSpawnIntervalSeconds(selectedMob);
            tooltipOutput.accept(
                    Component.translatable("item.prova.spawn_portal_wand.cost_info", cost, String.format(java.util.Locale.ROOT, "%.1f", intervalSec))
                            .withStyle(ChatFormatting.AQUA)
            );
        } else {
            tooltipOutput.accept(
                    Component.translatable("item.prova.spawn_portal_wand.none_selected")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
            );
        }

        List<String> favorites = stack.get(Prova.FAVORITE_MOBS.get());
        if (favorites != null && !favorites.isEmpty()) {
            tooltipOutput.accept(
                    Component.translatable("item.prova.spawn_portal_wand.favorites_count", favorites.size())
                            .withStyle(ChatFormatting.LIGHT_PURPLE)
            );
        }

        tooltipOutput.accept(Component.translatable("item.prova.spawn_portal_wand.desc1").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltipOutput.accept(Component.translatable("item.prova.spawn_portal_wand.desc_scroll").withStyle(ChatFormatting.YELLOW));
        tooltipOutput.accept(Component.translatable("item.prova.spawn_portal_wand.desc2").withStyle(ChatFormatting.AQUA));
        tooltipOutput.accept(Component.translatable("item.prova.spawn_portal_wand.desc3").withStyle(ChatFormatting.DARK_AQUA));
    }
}
