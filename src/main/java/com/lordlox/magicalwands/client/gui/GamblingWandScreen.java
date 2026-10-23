package com.lordlox.magicalwands.client.gui;

import com.lordlox.magicalwands.MagicalWands;
import com.lordlox.magicalwands.network.SetGamblingBetPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class GamblingWandScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 200;

    private final InteractionHand hand;
    private int currentBet;
    private int availableDiamonds;
    private boolean isCreative;

    private DiamondBetSlider slider;

    private int panelX0;
    private int panelY0;
    private int panelX1;
    private int panelY1;

    public GamblingWandScreen(InteractionHand hand, int initialBet) {
        super(Component.translatable("gui.lordlox_magical_wands.gambling_wand.title"));
        this.hand = hand;
        this.currentBet = Math.clamp(initialBet, 0, 20);

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            this.isCreative = player.hasInfiniteMaterials();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(Items.DIAMOND)) {
                    this.availableDiamonds += stack.getCount();
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        this.panelX0 = (this.width - PANEL_WIDTH) / 2;
        this.panelY0 = (this.height - PANEL_HEIGHT) / 2;
        this.panelX1 = this.panelX0 + PANEL_WIDTH;
        this.panelY1 = this.panelY0 + PANEL_HEIGHT;

        // Slider in the middle
        int sliderWidth = 200;
        int sliderX = (this.width - sliderWidth) / 2;
        int sliderY = panelY0 + 64;

        this.slider = this.addRenderableWidget(new DiamondBetSlider(
                sliderX, sliderY, sliderWidth, 20,
                this.currentBet,
                newBet -> this.currentBet = newBet
        ));

        // Quick Preset Buttons: 0, 5, 10, 15, 20, MAX
        int[] presets = {0, 5, 10, 15, 20};
        int presetBtnWidth = 28;
        int presetBtnHeight = 16;
        int totalPresetsWidth = presets.length * presetBtnWidth + (presets.length - 1) * 4 + 36;
        int startPresetX = (this.width - totalPresetsWidth) / 2;
        int presetY = sliderY + 26;

        for (int i = 0; i < presets.length; i++) {
            final int pVal = presets[i];
            this.addRenderableWidget(
                    Button.builder(Component.literal(String.valueOf(pVal)), b -> {
                        this.currentBet = pVal;
                        this.slider.setBetAmount(pVal);
                        playClick();
                    })
                    .bounds(startPresetX + i * (presetBtnWidth + 4), presetY, presetBtnWidth, presetBtnHeight)
                    .build()
            );
        }

        // MAX button
        int maxBetPossible = isCreative ? 20 : Math.min(20, availableDiamonds);
        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.lordlox_magical_wands.gambling_wand.btn_max").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), b -> {
                    this.currentBet = maxBetPossible;
                    this.slider.setBetAmount(maxBetPossible);
                    playClick();
                })
                .bounds(startPresetX + presets.length * (presetBtnWidth + 4), presetY, 32, presetBtnHeight)
                .build()
        );

        // Confirm / Close Button at bottom
        int confirmWidth = 140;
        int confirmHeight = 20;
        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.lordlox_magical_wands.gambling_wand.confirm").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), b -> onClose())
                .bounds((this.width - confirmWidth) / 2, panelY1 - 28, confirmWidth, confirmHeight)
                .build()
        );
    }

    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Dark background tint
        graphics.fill(0, 0, this.width, this.height, 0x880A0812);

        // Outer dark casino panel
        graphics.fill(panelX0, panelY0, panelX1, panelY1, 0xEE110F1D);
        graphics.outline(panelX0, panelY0, PANEL_WIDTH, PANEL_HEIGHT, 0xFF4A3E6D);
        graphics.outline(panelX0 - 1, panelY0 - 1, PANEL_WIDTH + 2, PANEL_HEIGHT + 2, 0x336C56A3);

        // Header Title
        graphics.centeredText(this.font,
                Component.translatable("gui.lordlox_magical_wands.gambling_wand.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                this.width / 2, panelY0 + 8, 0xFFFFD700);

        graphics.centeredText(this.font,
                Component.translatable("gui.lordlox_magical_wands.gambling_wand.subtitle").withStyle(ChatFormatting.GRAY),
                this.width / 2, panelY0 + 20, 0xFFAAAAAA);

        // Diamonds in inventory indicator
        int diamondBoxY = panelY0 + 36;
        int diamondItemX = this.width / 2 - 70;
        graphics.fakeItem(new ItemStack(Items.DIAMOND), diamondItemX, diamondBoxY);

        Component diamondText = Component.translatable(
                "gui.lordlox_magical_wands.gambling_wand.diamonds_available",
                isCreative ? "∞" : availableDiamonds
        ).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        graphics.text(this.font, diamondText, diamondItemX + 20, diamondBoxY + 4, 0xFF55FFFF);

        // Luck / Odds description card
        int luckCardY = panelY0 + 114;
        int luckCardX = panelX0 + 14;
        int luckCardWidth = PANEL_WIDTH - 28;
        graphics.fill(luckCardX, luckCardY, luckCardX + luckCardWidth, luckCardY + 28, 0xCC1A162B);
        graphics.outline(luckCardX, luckCardY, luckCardWidth, 28, 0xFF2D2545);

        Component luckInfo = getLuckDescription(this.currentBet);
        graphics.centeredText(this.font, luckInfo, this.width / 2, luckCardY + 9, 0xFFFFFFFF);

        // Warning if player doesn't have enough diamonds for bet
        if (!isCreative && currentBet > availableDiamonds) {
            Component warning = Component.literal("⚠ Diamanti insufficienti! Ne possiedi solo " + availableDiamonds)
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            graphics.centeredText(this.font, warning, this.width / 2, luckCardY + 34, 0xFFFF4444);
        }

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private Component getLuckDescription(int bet) {
        if (bet == 0) {
            return Component.translatable("gui.lordlox_magical_wands.gambling_wand.luck_0").withStyle(ChatFormatting.GRAY);
        } else if (bet <= 5) {
            return Component.translatable("gui.lordlox_magical_wands.gambling_wand.luck_low").withStyle(ChatFormatting.YELLOW);
        } else if (bet <= 12) {
            return Component.translatable("gui.lordlox_magical_wands.gambling_wand.luck_med").withStyle(ChatFormatting.GREEN);
        } else if (bet <= 19) {
            return Component.translatable("gui.lordlox_magical_wands.gambling_wand.luck_high").withStyle(ChatFormatting.AQUA);
        } else {
            return Component.translatable("gui.lordlox_magical_wands.gambling_wand.luck_max").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        }
    }

    @Override
    public void onClose() {
        // Send chosen bet to server
        int handIdx = (hand == InteractionHand.OFF_HAND) ? 1 : 0;
        ClientPacketDistributor.sendToServer(new SetGamblingBetPayload(handIdx, this.currentBet));

        // Update local stack directly for instant tooltip
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(MagicalWands.GAMBLING_WAND.get())) {
                stack.set(MagicalWands.GAMBLING_BET.get(), this.currentBet);
            }
        }

        super.onClose();
    }
}
