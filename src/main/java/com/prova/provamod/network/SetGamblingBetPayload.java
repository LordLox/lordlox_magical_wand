package com.prova.provamod.network;

import com.prova.provamod.Prova;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetGamblingBetPayload(int handIndex, int betAmount) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetGamblingBetPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Prova.MODID, "set_gambling_bet"));

    public static final StreamCodec<FriendlyByteBuf, SetGamblingBetPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SetGamblingBetPayload::handIndex,
                    ByteBufCodecs.VAR_INT, SetGamblingBetPayload::betAmount,
                    SetGamblingBetPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
