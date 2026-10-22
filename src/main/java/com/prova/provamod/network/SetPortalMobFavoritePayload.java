package com.prova.provamod.network;

import com.prova.provamod.Prova;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetPortalMobFavoritePayload(int handIndex, String mobId, boolean isFavorite) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetPortalMobFavoritePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Prova.MODID, "set_portal_mob_favorite"));

    public static final StreamCodec<FriendlyByteBuf, SetPortalMobFavoritePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SetPortalMobFavoritePayload::handIndex,
                    ByteBufCodecs.STRING_UTF8, SetPortalMobFavoritePayload::mobId,
                    ByteBufCodecs.BOOL, SetPortalMobFavoritePayload::isFavorite,
                    SetPortalMobFavoritePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
