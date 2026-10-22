package com.prova.provamod.network;

import com.prova.provamod.Prova;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RollD20Payload(int rollResult, int betAmount, Component rewardDescription) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RollD20Payload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Prova.MODID, "roll_d20"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RollD20Payload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RollD20Payload::rollResult,
                    ByteBufCodecs.VAR_INT, RollD20Payload::betAmount,
                    ComponentSerialization.STREAM_CODEC, RollD20Payload::rewardDescription,
                    RollD20Payload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public Component rewardDescription() {
        return this.rewardDescription;
    }
}
