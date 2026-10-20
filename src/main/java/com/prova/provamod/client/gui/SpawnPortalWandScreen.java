package com.prova.provamod.client.gui;

import com.prova.provamod.network.SelectPortalMobPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnPortalWandScreen extends Screen {

    private static final int CARDS_PER_ROW = 4;
    private static final int ROWS = 2;
    private static final int CARDS_PER_PAGE = CARDS_PER_ROW * ROWS; // 8 cards per page
    private static final int CARD_WIDTH = 76;
    private static final int CARD_HEIGHT = 100;
    private static final int CARD_GAP_X = 10;
    private static final int CARD_GAP_Y = 10;

    private final InteractionHand hand;
    private String selectedMobId;
    private int currentPage = 0;
    private final int totalPages;
    private final List<PortalMobRegistry.MobCardEntry> allMobs;
    private final Map<String, LivingEntity> dummyEntityCache = new HashMap<>();

    private Button prevButton;
    private Button nextButton;

    public SpawnPortalWandScreen(InteractionHand hand, String currentSelectedMobId) {
        super(Component.translatable("gui.prova.spawn_portal_wand.title"));
        this.hand = hand;
        this.selectedMobId = currentSelectedMobId != null ? currentSelectedMobId : "minecraft:zombie";
        this.allMobs = PortalMobRegistry.getAll();
        this.totalPages = Math.max(1, (int) Math.ceil((double) allMobs.size() / CARDS_PER_PAGE));
    }

    @Override
    protected void init() {
        super.init();

        int totalGridWidth = CARDS_PER_ROW * CARD_WIDTH + (CARDS_PER_ROW - 1) * CARD_GAP_X;
        int gridStartX = (this.width - totalGridWidth) / 2;
        int gridStartY = (this.height - (ROWS * CARD_HEIGHT + (ROWS - 1) * CARD_GAP_Y)) / 2 + 10;
        int footerY = gridStartY + ROWS * CARD_HEIGHT + (ROWS - 1) * CARD_GAP_Y + 12;

        // Navigation buttons
        this.prevButton = this.addRenderableWidget(
                Button.builder(Component.literal("◀"), b -> changePage(-1))
                        .bounds(gridStartX, footerY, 28, 20)
                        .build()
        );

        this.nextButton = this.addRenderableWidget(
                Button.builder(Component.literal("▶"), b -> changePage(1))
                        .bounds(gridStartX + totalGridWidth - 28, footerY, 28, 20)
                        .build()
        );

        updateButtonStates();
    }

    private void changePage(int delta) {
        this.currentPage = Math.clamp(this.currentPage + delta, 0, totalPages - 1);
        updateButtonStates();
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void updateButtonStates() {
        if (prevButton != null) {
            prevButton.active = currentPage > 0;
        }
        if (nextButton != null) {
            nextButton.active = currentPage < totalPages - 1;
        }
    }

    private LivingEntity getOrCreateDummy(PortalMobRegistry.MobCardEntry entry) {
        return dummyEntityCache.computeIfAbsent(entry.id(), id -> {
            if (minecraft != null && minecraft.level != null) {
                try {
                    return entry.entityType().create(minecraft.level, EntitySpawnReason.TRIGGERED);
                } catch (Exception ignored) {
                    return null;
                }
            }
            return null;
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Subtle background tint
        graphics.fill(0, 0, this.width, this.height, 0x880A0812);

        int totalGridWidth = CARDS_PER_ROW * CARD_WIDTH + (CARDS_PER_ROW - 1) * CARD_GAP_X;
        int totalGridHeight = ROWS * CARD_HEIGHT + (ROWS - 1) * CARD_GAP_Y;
        int gridStartX = (this.width - totalGridWidth) / 2;
        int gridStartY = (this.height - totalGridHeight) / 2 + 10;

        // Panel background with glow
        int panelPadding = 12;
        int panelX0 = gridStartX - panelPadding;
        int panelY0 = gridStartY - 38;
        int panelX1 = gridStartX + totalGridWidth + panelPadding;
        int panelY1 = gridStartY + totalGridHeight + 38;

        // Outer dark panel
        graphics.fill(panelX0, panelY0, panelX1, panelY1, 0xEE110F1D);
        graphics.outline(panelX0, panelY0, panelX1 - panelX0, panelY1 - panelY0, 0xFF4A3E6D);
        graphics.outline(panelX0 - 1, panelY0 - 1, (panelX1 - panelX0) + 2, (panelY1 - panelY0) + 2, 0x336C56A3);

        // Header Title
        graphics.centeredText(this.font,
                Component.translatable("gui.prova.spawn_portal_wand.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                this.width / 2, gridStartY - 30, 0xFFFFD700);

        // Header Subtitle
        graphics.centeredText(this.font,
                Component.translatable("gui.prova.spawn_portal_wand.subtitle").withStyle(ChatFormatting.AQUA),
                this.width / 2, gridStartY - 18, 0xFF79E6F3);

        // Draw Cards
        int startIndex = currentPage * CARDS_PER_PAGE;
        int endIndex = Math.min(startIndex + CARDS_PER_PAGE, allMobs.size());

        for (int i = startIndex; i < endIndex; i++) {
            int localIndex = i - startIndex;
            int col = localIndex % CARDS_PER_ROW;
            int row = localIndex / CARDS_PER_ROW;

            int cardX = gridStartX + col * (CARD_WIDTH + CARD_GAP_X);
            int cardY = gridStartY + row * (CARD_HEIGHT + CARD_GAP_Y);

            boolean isHovered = mouseX >= cardX && mouseX < cardX + CARD_WIDTH && mouseY >= cardY && mouseY < cardY + CARD_HEIGHT;
            PortalMobRegistry.MobCardEntry entry = allMobs.get(i);
            boolean isSelected = entry.id().equals(this.selectedMobId);

            renderCard(graphics, entry, cardX, cardY, isHovered, isSelected, mouseX, mouseY);
        }

        // Render Page indicator in footer
        int footerY = gridStartY + totalGridHeight + 17;
        String pageStr = String.format("Pagina %d / %d", currentPage + 1, totalPages);
        graphics.centeredText(this.font, Component.literal(pageStr).withStyle(ChatFormatting.WHITE), this.width / 2, footerY, 0xFFFFFFFF);

        // Render Active Mob indicator at very bottom
        PortalMobRegistry.MobCardEntry selectedEntry = PortalMobRegistry.getById(this.selectedMobId);
        Component mobName = selectedEntry != null ? selectedEntry.displayName() : Component.literal(this.selectedMobId);
        Component statusText = Component.translatable("gui.prova.spawn_portal_wand.current_mob", mobName).withStyle(ChatFormatting.YELLOW);
        graphics.centeredText(this.font, statusText, this.width / 2, panelY1 - 14, 0xFFFFEE77);

        // Render child widgets (buttons)
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void renderCard(GuiGraphicsExtractor graphics, PortalMobRegistry.MobCardEntry entry,
                            int cardX, int cardY, boolean isHovered, boolean isSelected,
                            int mouseX, int mouseY) {
        // Card Background
        int bgColor = isSelected ? 0xEE2A1E45 : (isHovered ? 0xEE201B35 : 0xEE161325);
        graphics.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, bgColor);

        // Card Border
        if (isSelected) {
            // Radiant golden double border for selected card
            graphics.outline(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 0xFFFFD700);
            graphics.outline(cardX + 1, cardY + 1, CARD_WIDTH - 2, CARD_HEIGHT - 2, 0xFFFFEA75);
        } else if (isHovered) {
            // Neon cyan border for hovered card
            graphics.outline(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 0xFF5CE1E6);
        } else {
            // Subtle dark slate border for unselected card
            graphics.outline(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 0xFF352E4B);
        }

        // Top Header of card: Mob Name
        Component name = entry.displayName();
        int maxNameWidth = CARD_WIDTH - 6;
        if (font.width(name) > maxNameWidth) {
            String rawStr = font.plainSubstrByWidth(name.getString(), maxNameWidth - 6) + "…";
            name = Component.literal(rawStr);
        }
        int nameColor = isSelected ? 0xFFFFD700 : (isHovered ? 0xFFFFFFFF : 0xFFCCCCCC);
        graphics.centeredText(this.font, name, cardX + CARD_WIDTH / 2, cardY + 5, nameColor);

        // Center: 3D Living Entity Render
        LivingEntity dummy = getOrCreateDummy(entry);
        if (dummy != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics,
                    cardX + 4,
                    cardY + 16,
                    cardX + CARD_WIDTH - 4,
                    cardY + CARD_HEIGHT - 18,
                    entry.previewScale(),
                    entry.previewOffsetY(),
                    mouseX,
                    mouseY,
                    dummy
            );
        }

        // Bottom Badge: Category or "SELEZIONATO"
        int badgeY0 = cardY + CARD_HEIGHT - 15;
        int badgeY1 = cardY + CARD_HEIGHT - 4;
        int badgeX0 = cardX + 5;
        int badgeX1 = cardX + CARD_WIDTH - 5;

        if (isSelected) {
            graphics.fill(badgeX0, badgeY0, badgeX1, badgeY1, 0xFFB8860B);
            graphics.centeredText(this.font,
                    Component.literal("✓ ATTIVO").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD),
                    cardX + CARD_WIDTH / 2, badgeY0 + 2, 0xFF000000);
        } else {
            // Category tag
            graphics.fill(badgeX0, badgeY0, badgeX1, badgeY1, 0x44000000);
            graphics.centeredText(this.font, entry.categoryName(), cardX + CARD_WIDTH / 2, badgeY0 + 2, entry.categoryColor());
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.button() == 0) { // Left click
            double mouseX = event.x();
            double mouseY = event.y();

            int totalGridWidth = CARDS_PER_ROW * CARD_WIDTH + (CARDS_PER_ROW - 1) * CARD_GAP_X;
            int totalGridHeight = ROWS * CARD_HEIGHT + (ROWS - 1) * CARD_GAP_Y;
            int gridStartX = (this.width - totalGridWidth) / 2;
            int gridStartY = (this.height - totalGridHeight) / 2 + 10;

            int startIndex = currentPage * CARDS_PER_PAGE;
            int endIndex = Math.min(startIndex + CARDS_PER_PAGE, allMobs.size());

            for (int i = startIndex; i < endIndex; i++) {
                int localIndex = i - startIndex;
                int col = localIndex % CARDS_PER_ROW;
                int row = localIndex / CARDS_PER_ROW;

                int cardX = gridStartX + col * (CARD_WIDTH + CARD_GAP_X);
                int cardY = gridStartY + row * (CARD_HEIGHT + CARD_GAP_Y);

                if (mouseX >= cardX && mouseX < cardX + CARD_WIDTH && mouseY >= cardY && mouseY < cardY + CARD_HEIGHT) {
                    PortalMobRegistry.MobCardEntry entry = allMobs.get(i);
                    this.selectedMobId = entry.id();

                    // Play selection sound
                    if (minecraft != null) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F));
                    }

                    // Send selection payload to server
                    int handIdx = (hand == InteractionHand.OFF_HAND) ? 1 : 0;
                    ClientPacketDistributor.sendToServer(new SelectPortalMobPayload(handIdx, entry.id()));
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
