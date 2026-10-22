package com.prova.provamod.network;

import com.prova.provamod.Prova;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RollD20Payload(int rollResult, int betAmount) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RollD20Payload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Prova.MODID, "roll_d20"));

    public static final StreamCodec<FriendlyByteBuf, RollD20Payload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RollD20Payload::rollResult,
                    ByteBufCodecs.VAR_INT, RollD20Payload::betAmount,
                    RollD20Payload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
