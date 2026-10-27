package com.lordlox.magicalwands.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class DiamondBetSlider extends AbstractSliderButton {

    public static final int MAX_BET = 32;

    private final Consumer<Integer> onValueChanged;

    public DiamondBetSlider(int x, int y, int width, int height, int initialBet, Consumer<Integer> onValueChanged) {
        super(x, y, width, height, Component.empty(), Math.clamp((double) initialBet / (double) MAX_BET, 0.0, 1.0));
        this.onValueChanged = onValueChanged;
        updateMessage();
    }

    public int getBetAmount() {
        return (int) Math.round(this.value * (double) MAX_BET);
    }

    public void setBetAmount(int bet) {
        this.value = Math.clamp((double) bet / (double) MAX_BET, 0.0, 1.0);
        updateMessage();
        applyValue();
    }

    @Override
    protected void updateMessage() {
        int bet = getBetAmount();
        this.setMessage(switch (bet) {
            case 0 -> Component.translatable("gui.lordlox_magical_wands.gambling_wand.slider_zero");
            case 1 -> Component.translatable("gui.lordlox_magical_wands.gambling_wand.slider_single", bet);
            default -> Component.translatable("gui.lordlox_magical_wands.gambling_wand.slider_multiple", bet);
        });
    }

    @Override
    protected void applyValue() {
        if (onValueChanged != null) {
            onValueChanged.accept(getBetAmount());
        }
    }
}
