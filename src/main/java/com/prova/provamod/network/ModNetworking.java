package com.prova.provamod.network;

import java.util.ArrayList;
import java.util.List;

import com.prova.provamod.Prova;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
    }

    public static void handleSelectPortalMob(SelectPortalMobPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player == null) {
            return;
        }

        InteractionHand hand = payload.handIndex() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(Prova.SPAWN_PORTAL_WAND.get())) {
            stack.set(Prova.SELECTED_MOB.get(), payload.mobId());
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);

            Identifier id = Identifier.tryParse(payload.mobId());
            Component mobName = id != null ? BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                    .map(type -> type.getDescription())
                    .orElseGet(() -> Component.translatable("entity." + payload.mobId().replace(':', '.')))
                    : Component.translatable("entity." + payload.mobId().replace(':', '.'));

            List<String> favs = stack.get(Prova.FAVORITE_MOBS.get());
            if (favs != null && favs.contains(payload.mobId())) {
                int index = favs.indexOf(payload.mobId()) + 1;
                player.sendOverlayMessage(
                        Component.translatable("item.prova.spawn_portal_wand.favorite_selected", mobName, index, favs.size())
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                );
            } else {
                player.sendOverlayMessage(
                        Component.translatable("item.prova.spawn_portal_wand.selected", mobName)
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

        if (stack.is(Prova.SPAWN_PORTAL_WAND.get())) {
            List<String> current = stack.get(Prova.FAVORITE_MOBS.get());
            List<String> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
            if (payload.isFavorite()) {
                if (!updated.contains(payload.mobId())) {
                    updated.add(payload.mobId());
                }
            } else {
                updated.remove(payload.mobId());
            }
            stack.set(Prova.FAVORITE_MOBS.get(), List.copyOf(updated));
        }
    }
}
