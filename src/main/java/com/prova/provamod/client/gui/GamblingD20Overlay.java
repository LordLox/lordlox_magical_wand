package com.prova.provamod.client.gui;

import com.prova.provamod.Prova;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

public class GamblingD20Overlay {

    public static final Identifier D20_TEXTURE = Identifier.fromNamespaceAndPath(Prova.MODID, "textures/gui/d20_die.png");

    private static final long ROLL_DURATION_MS = 2000L;
    private static final long RESULT_DURATION_MS = 2200L;
    private static final long TOTAL_DURATION_MS = ROLL_DURATION_MS + RESULT_DURATION_MS;

    private static final Random RANDOM = new Random();

    private static boolean isActive = false;
    private static int targetRoll = 10;
    private static int betAmount = 0;
    private static Component rewardDescription = null;
    private static long startNanoTime = 0;
    private static long lastCycleMs = 0;
    private static int currentCyclingNumber = 10;
    private static boolean playedClimaxSound = false;

    public static void startRoll(int rollResult, int bet, Component rewardDesc) {
        targetRoll = rollResult;
        betAmount = bet;
        rewardDescription = rewardDesc;
        startNanoTime = System.nanoTime();
        lastCycleMs = 0;
        currentCyclingNumber = 1 + RANDOM.nextInt(20);
        playedClimaxSound = false;
        isActive = true;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!isActive) {
            return;
        }

        long elapsedMs = (System.nanoTime() - startNanoTime) / 1_000_000L;
        if (elapsedMs >= TOTAL_DURATION_MS) {
            isActive = false;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        boolean isRolling = elapsedMs < ROLL_DURATION_MS;

        Component header = isRolling
                ? Component.translatable("overlay.prova.gambling.rolling")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        .append(Component.literal(" ◆" + betAmount).withStyle(ChatFormatting.AQUA))
                : getResultHeader(targetRoll);

        Component footer = isRolling
                ? Component.translatable("item.prova.gambling_wand.desc1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                : (rewardDescription != null ? rewardDescription : getResultDescription(targetRoll));

        // Overlay card dimensions & positioning - adaptive width to fit reward text without clipping
        int contentWidth = Math.max(font.width(header), font.width(footer));
        int cardW = Math.max(220, contentWidth + 24);
        int cardH = 92;
        int centerX = screenW / 2;
        int cardY0 = Math.max(16, screenH / 5);
        int cardX0 = centerX - cardW / 2;
        int cardX1 = cardX0 + cardW;
        int cardY1 = cardY0 + cardH;

        // Smooth fade-in & fade-out alpha
        float alpha = 1.0F;
        if (elapsedMs < 200) {
            alpha = elapsedMs / 200.0F;
        } else if (elapsedMs > TOTAL_DURATION_MS - 400) {
            alpha = (TOTAL_DURATION_MS - elapsedMs) / 400.0F;
        }
        alpha = Math.clamp(alpha, 0.0F, 1.0F);

        int alphaBits = ((int) (alpha * 240.0F)) << 24;
        int bgCol = alphaBits | 0x0E0B19;
        int borderCol = alphaBits | (elapsedMs >= ROLL_DURATION_MS ? getResultBorderColor(targetRoll) : 0xD4AF37);
        int innerBorderCol = alphaBits | 0x33284A;

        // Background card
        graphics.fill(cardX0, cardY0, cardX1, cardY1, bgCol);
        graphics.outline(cardX0, cardY0, cardW, cardH, borderCol);
        graphics.outline(cardX0 + 1, cardY0 + 1, cardW - 2, cardH - 2, innerBorderCol);

        if (isRolling) {
            // Decelerating cycle interval
            float progress = (float) elapsedMs / ROLL_DURATION_MS;
            long cycleInterval = 45L + (long) (Math.pow(progress, 2.5) * 220.0);

            if (elapsedMs - lastCycleMs > cycleInterval) {
                currentCyclingNumber = 1 + RANDOM.nextInt(20);
                lastCycleMs = elapsedMs;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.4F + RANDOM.nextFloat() * 0.4F));
            }

            graphics.centeredText(font, header, centerX, cardY0 + 8, 0xFFFFD700);

            // D20 Graphic & Number in center
            int dieSize = 44;
            int dieX = centerX - dieSize / 2;
            int dieY = cardY0 + 22;
            graphics.blit(D20_TEXTURE, dieX, dieY, dieSize, dieSize, 0.0F, 0.0F, 1.0F, 1.0F);

            Component numText = Component.literal(String.valueOf(currentCyclingNumber))
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
            graphics.centeredText(font, numText, centerX, dieY + 18, 0xFFFFFF55);

            graphics.centeredText(font, footer, centerX, cardY1 - 14, 0xFFAAAAAA);

        } else {
            // Landed on final result!
            if (!playedClimaxSound) {
                playedClimaxSound = true;
                playResultSound(mc, targetRoll);
            }

            graphics.centeredText(font, header, centerX, cardY0 + 8, 0xFFFFFFFF);

            // D20 Graphic & Number
            int dieSize = 44;
            int dieX = centerX - dieSize / 2;
            int dieY = cardY0 + 22;
            graphics.blit(D20_TEXTURE, dieX, dieY, dieSize, dieSize, 0.0F, 0.0F, 1.0F, 1.0F);

            Component numText = getResultNumberText(targetRoll);
            graphics.centeredText(font, numText, centerX, dieY + 18, 0xFFFFFFFF);

            // Footer reward description stating what was actually obtained
            graphics.centeredText(font, footer, centerX, cardY1 - 14, 0xFFFFFFFF);
        }
    }

    private static int getResultBorderColor(int roll) {
        if (roll == 20) return 0xFFD700; // Bright Gold
        if (roll >= 16) return 0x00E5FF; // Diamond Cyan
        if (roll >= 11) return 0xE0E0E0; // Silver/Iron
        if (roll >= 8)  return 0x55FF55; // Passive Green
        if (roll >= 2)  return 0xFF5555; // Danger Red
        return 0x880000;                 // Dark Crimson Skull
    }

    private static Component getResultHeader(int roll) {
        if (roll == 20) {
            return Component.literal("★ ★ ★ NATURAL 20! ★ ★ ★").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        } else if (roll >= 16) {
            return Component.literal("★ SUCCESSO ECCELLENTE! ★").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        } else if (roll >= 11) {
            return Component.literal("MINERALI COMUNI").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        } else if (roll >= 8) {
            return Component.literal("CREATURE AMICHEVOLI").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        } else if (roll >= 2) {
            return Component.literal("ATTENZIONE: MOSTRI!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else {
            return Component.literal("☠ CRITICAL FAIL! ☠").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        }
    }

    private static Component getResultNumberText(int roll) {
        if (roll == 20) {
            return Component.literal("★ 20 ★").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        } else if (roll >= 16) {
            return Component.literal(String.valueOf(roll)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        } else if (roll >= 11) {
            return Component.literal(String.valueOf(roll)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        } else if (roll >= 8) {
            return Component.literal(String.valueOf(roll)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        } else if (roll >= 2) {
            return Component.literal(String.valueOf(roll)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else {
            return Component.literal("☠ 1 ☠").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        }
    }

    private static Component getResultDescription(int roll) {
        if (roll == 20) {
            return Component.literal("Ottenuto: 64x Diamanti (JACKPOT!)").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        } else if (roll >= 16) {
            return Component.literal("Ottenuto: Minerali Rari").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        } else if (roll >= 11) {
            return Component.literal("Ottenuto: Minerali Comuni").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        } else if (roll >= 8) {
            return Component.literal("Evocate Creature pacifiche!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        } else if (roll >= 2) {
            return Component.literal("Evocati Mostri ostili!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else {
            return Component.literal("Nessuna vincita: Morte istantanea!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        }
    }

    private static void playResultSound(Minecraft mc, int roll) {
        if (roll == 20) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.TOTEM_USE, 1.2F));
        } else if (roll >= 16) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.3F));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.1F));
        } else if (roll >= 11) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
        } else if (roll >= 8) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F));
        } else if (roll >= 2) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WITHER_SPAWN, 1.8F));
        } else {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8F));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WITHER_DEATH, 0.9F));
        }
    }
}
