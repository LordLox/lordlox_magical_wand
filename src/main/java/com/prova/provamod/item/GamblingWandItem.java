package com.prova.provamod.item;

import com.prova.provamod.Prova;
import com.prova.provamod.gambling.GamblingRollManager;
import com.prova.provamod.network.RollD20Payload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class GamblingWandItem extends Item {

    public GamblingWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Sneak + Right Click: Open the Diamond Wager Slider GUI
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                int currentBet = itemStack.getOrDefault(Prova.GAMBLING_BET.get(), 0);
                com.prova.provamod.client.gui.GamblingWandClientHelper.openScreen(hand, currentBet);
            }
            return InteractionResult.SUCCESS;
        }

        // Normal Right Click: Roll the D20
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;

        if (GamblingRollManager.isPlayerRolling(serverPlayer.getUUID())) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("item.prova.gambling_wand.already_rolling")
                            .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.2F);
            return InteractionResult.FAIL;
        }

        int bet = itemStack.getOrDefault(Prova.GAMBLING_BET.get(), 0);
        bet = Math.clamp(bet, 0, 20);

        boolean isCreative = player.hasInfiniteMaterials();

        // Check and consume diamonds if not in creative
        if (!isCreative && bet > 0) {
            int diamondCount = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.is(Items.DIAMOND)) {
                    diamondCount += invStack.getCount();
                }
            }

            if (diamondCount < bet) {
                serverPlayer.sendOverlayMessage(
                        Component.translatable("item.prova.gambling_wand.need_diamonds", bet)
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                );
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
                return InteractionResult.FAIL;
            }

            int toConsume = bet;
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

        // Calculate D20 roll with bias based on wager
        int roll;
        if (bet == 0) {
            roll = 1 + level.getRandom().nextInt(20);
        } else {
            double power = 1.0 + (bet * 0.15);
            double u = level.getRandom().nextDouble();
            double biased = Math.pow(u, 1.0 / power);
            roll = Math.clamp((int)(1 + 20 * biased), 1, 20);
        }

        // Cooldown & durability
        player.getCooldowns().addCooldown(itemStack, 60); // 3 seconds
        if (!isCreative && itemStack.isDamageableItem()) {
            itemStack.hurtAndBreak(1, player, hand);
        }

        // Roll casting sounds
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9F, 1.3F);

        // Prepare outcome: generates exact drops/mobs and localized description
        GamblingRollManager.RollPreparation prep = GamblingRollManager.prepareRoll(serverPlayer.level(), roll, bet);

        // Schedule outcome: triggers after 40 ticks (~2.0 seconds)
        GamblingRollManager.startRoll(serverPlayer, prep, 40);

        // Send roll packet to client for animated overlay
        PacketDistributor.sendToPlayer(serverPlayer, new RollD20Payload(roll, bet, prep.rewardDescription()));

        player.swing(hand);
        return InteractionResult.SUCCESS;
    }
}
