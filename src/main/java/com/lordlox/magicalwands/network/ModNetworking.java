package com.lordlox.magicalwands.network;

import java.util.ArrayList;
import java.util.List;

import com.lordlox.magicalwands.MagicalWands;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                SelectPortalMobPayload.TYPE,
                SelectPortalMobPayload.STREAM_CODEC,
                ModNetworking::handleSelectPortalMob
        );
        registrar.playToServer(
                SetPortalMobFavoritePayload.TYPE,
                SetPortalMobFavoritePayload.STREAM_CODEC,
                ModNetworking::handleSetPortalMobFavorite
        );
        registrar.playToServer(
                SetGamblingBetPayload.TYPE,
                SetGamblingBetPayload.STREAM_CODEC,
                ModNetworking::handleSetGamblingBet
        );
        registrar.playToClient(
                RollD20Payload.TYPE,
                RollD20Payload.STREAM_CODEC,
                ModNetworking::handleRollD20
        );
    }

    public static void handleSelectPortalMob(SelectPortalMobPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player == null) {
            return;
        }

        InteractionHand hand = payload.handIndex() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(MagicalWands.SPAWN_PORTAL_WAND.get())) {
            stack.set(MagicalWands.SELECTED_MOB.get(), payload.mobId());
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);

            Identifier id = Identifier.tryParse(payload.mobId());
            Component mobName = id != null ? BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                    .map(EntityType::getDescription)
                    .orElseGet(() -> Component.translatable("entity." + payload.mobId().replace(':', '.')))
                    : Component.translatable("entity." + payload.mobId().replace(':', '.'));

            int cost = com.lordlox.magicalwands.portal.PortalMobConfig.getDiamondCost(payload.mobId());
            float rate = com.lordlox.magicalwands.portal.PortalMobConfig.getSpawnIntervalSeconds(payload.mobId());
            Component statBadge = Component.literal(String.format(java.util.Locale.ROOT, " [◆%d | ⏱%.1fs]", cost, rate))
                    .withStyle(ChatFormatting.AQUA);

            List<String> favs = stack.get(MagicalWands.FAVORITE_MOBS.get());
            if (favs != null && favs.contains(payload.mobId())) {
                int index = favs.indexOf(payload.mobId()) + 1;
                player.sendOverlayMessage(
                        Component.translatable("item.lordlox_magical_wands.spawn_portal_wand.favorite_selected", mobName, index, favs.size())
                                .append(statBadge)
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                );
            } else {
                player.sendOverlayMessage(
                        Component.translatable("item.lordlox_magical_wands.spawn_portal_wand.selected", mobName)
                                .append(statBadge)
                                .withStyle(ChatFormatting.GREEN)
                );
            }
        }
    }

    public static void handleSetPortalMobFavorite(SetPortalMobFavoritePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player == null) {
            return;
        }

        InteractionHand hand = payload.handIndex() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(MagicalWands.SPAWN_PORTAL_WAND.get())) {
            List<String> current = stack.get(MagicalWands.FAVORITE_MOBS.get());
            List<String> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
            if (payload.isFavorite()) {
                if (!updated.contains(payload.mobId())) {
                    updated.add(payload.mobId());
                }
            } else {
                updated.remove(payload.mobId());
            }
            stack.set(MagicalWands.FAVORITE_MOBS.get(), List.copyOf(updated));
        }
    }

    public static void handleSetGamblingBet(SetGamblingBetPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player == null) {
            return;
        }

        InteractionHand hand = payload.handIndex() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(MagicalWands.GAMBLING_WAND.get())) {
            int bet = Math.clamp(payload.betAmount(), 0, 20);
            stack.set(MagicalWands.GAMBLING_BET.get(), bet);
        }
    }

    public static void handleRollD20(RollD20Payload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            com.lordlox.magicalwands.client.gui.GamblingWandClientHelper.triggerRollOverlay(
                    payload.rollResult(), payload.betAmount(), payload.rewardDescription()
            );
        });
    }
}
