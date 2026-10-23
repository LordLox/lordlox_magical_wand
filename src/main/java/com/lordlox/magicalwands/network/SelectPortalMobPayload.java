package com.lordlox.magicalwands.network;

import com.lordlox.magicalwands.MagicalWands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectPortalMobPayload(int handIndex, String mobId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectPortalMobPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MagicalWands.MODID, "select_portal_mob"));

    public static final StreamCodec<FriendlyByteBuf, SelectPortalMobPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SelectPortalMobPayload::handIndex,
                    ByteBufCodecs.STRING_UTF8, SelectPortalMobPayload::mobId,
                    SelectPortalMobPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
