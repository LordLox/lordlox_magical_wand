package com.prova.provamod.client.gui;

import com.prova.provamod.network.SelectPortalMobPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnPortalWandScreen extends Screen {

    private static final int ROWS = 2;
    private static final int CARD_WIDTH = 62;
    private static final int CARD_HEIGHT = 74;
    private static final int CARD_GAP_X = 6;
    private static final int CARD_GAP_Y = 6;
    private static final int PANEL_HEIGHT = 222;

    private final InteractionHand hand;
    private String selectedMobId;
    private int currentPage = 0;
    private int totalPages = 1;

    private final List<PortalMobRegistry.MobCardEntry> allMobs;
    private final List<PortalMobRegistry.MobCardEntry> filteredMobs = new ArrayList<>();
    private final Map<String, LivingEntity> dummyEntityCache = new HashMap<>();

    private int cardsPerRow = 5;
    private int cardsPerPage = 10;
    private int gridStartX;
    private int gridStartY;
    private int totalGridWidth;
    private int totalGridHeight;
    private int panelX0;
    private int panelY0;
    private int panelX1;
    private int panelY1;
    private int footerY;

    private String searchQuery = "";
    private EditBox searchBox;
    private Button prevButton;
    private Button nextButton;

    public SpawnPortalWandScreen(InteractionHand hand, String currentSelectedMobId) {
        super(Component.translatable("gui.prova.spawn_portal_wand.title"));
        this.hand = hand;
        this.selectedMobId = currentSelectedMobId != null ? currentSelectedMobId : "minecraft:zombie";

        Minecraft mc = Minecraft.getInstance();
        this.allMobs = PortalMobRegistry.getAll(mc.level);
        this.filteredMobs.addAll(this.allMobs);
    }

    @Override
    protected void init() {
        super.init();

        this.cardsPerRow = (this.width < 370) ? 4 : 5;
        this.cardsPerPage = this.cardsPerRow * ROWS;

        this.totalGridWidth = cardsPerRow * CARD_WIDTH + (cardsPerRow - 1) * CARD_GAP_X;
        this.totalGridHeight = ROWS * CARD_HEIGHT + (ROWS - 1) * CARD_GAP_Y;

        this.panelY0 = Math.max(4, (this.height - PANEL_HEIGHT) / 2);
        this.panelY1 = this.panelY0 + PANEL_HEIGHT;

        int panelPadding = 10;
        this.gridStartX = (this.width - totalGridWidth) / 2;
        this.gridStartY = panelY0 + 38;
        this.panelX0 = gridStartX - panelPadding;
        this.panelX1 = gridStartX + totalGridWidth + panelPadding;
        this.footerY = panelY0 + 196;

        // Search Bar in header
        int searchBoxWidth = Math.min(220, totalGridWidth);
        int searchBoxX = (this.width - searchBoxWidth) / 2;
        int searchBoxY = panelY0 + 18;

        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, searchBoxWidth, 16,
                Component.translatable("gui.prova.spawn_portal_wand.search_hint"));
        this.searchBox.setHint(Component.translatable("gui.prova.spawn_portal_wand.search_hint").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setMaxLength(36);
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(this::onSearchQueryChanged);
        this.addRenderableWidget(this.searchBox);

        // Navigation buttons in footer
        int btnWidth = 24;
        int btnHeight = 18;

        this.prevButton = this.addRenderableWidget(
                Button.builder(Component.literal("◀"), b -> changePage(-1))
                        .bounds(gridStartX, footerY, btnWidth, btnHeight)
                        .build()
        );

        this.nextButton = this.addRenderableWidget(
                Button.builder(Component.literal("▶"), b -> changePage(1))
                        .bounds(gridStartX + totalGridWidth - btnWidth, footerY, btnWidth, btnHeight)
                        .build()
        );

        recalculatePages();
        updateButtonStates();
    }

    private void onSearchQueryChanged(String query) {
        this.searchQuery = query.trim().toLowerCase();
        this.filteredMobs.clear();

        if (this.searchQuery.isEmpty()) {
            this.filteredMobs.addAll(this.allMobs);
        } else {
            for (PortalMobRegistry.MobCardEntry entry : this.allMobs) {
                String name = entry.displayName().getString().toLowerCase();
                String id = entry.id().toLowerCase();
                if (name.contains(this.searchQuery) || id.contains(this.searchQuery)) {
                    this.filteredMobs.add(entry);
                }
            }
        }

        this.currentPage = 0;
        recalculatePages();
        updateButtonStates();
    }

    private void recalculatePages() {
        this.totalPages = Math.max(1, (int) Math.ceil((double) filteredMobs.size() / cardsPerPage));
        this.currentPage = Math.clamp(this.currentPage, 0, totalPages - 1);
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
                } catch (Throwable ignored) {
                    return null;
                }
            }
            return null;
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Dark background tint
        graphics.fill(0, 0, this.width, this.height, 0x880A0812);

        // Outer dark panel
        graphics.fill(panelX0, panelY0, panelX1, panelY1, 0xEE110F1D);
        graphics.outline(panelX0, panelY0, panelX1 - panelX0, panelY1 - panelY0, 0xFF4A3E6D);
        graphics.outline(panelX0 - 1, panelY0 - 1, (panelX1 - panelX0) + 2, (panelY1 - panelY0) + 2, 0x336C56A3);

        // Header Title
        graphics.centeredText(this.font,
                Component.translatable("gui.prova.spawn_portal_wand.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                this.width / 2, panelY0 + 6, 0xFFFFD700);

        // Draw Cards or empty notice
        if (filteredMobs.isEmpty()) {
            graphics.centeredText(this.font,
                    Component.translatable("gui.prova.spawn_portal_wand.no_results").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    this.width / 2, gridStartY + totalGridHeight / 2 - 4, 0xFFAAAAAA);
        } else {
            int startIndex = currentPage * cardsPerPage;
            int endIndex = Math.min(startIndex + cardsPerPage, filteredMobs.size());

            PortalMobRegistry.MobCardEntry hoveredEntry = null;

            for (int i = startIndex; i < endIndex; i++) {
                int localIndex = i - startIndex;
                int col = localIndex % cardsPerRow;
                int row = localIndex / cardsPerRow;

                int cardX = gridStartX + col * (CARD_WIDTH + CARD_GAP_X);
                int cardY = gridStartY + row * (CARD_HEIGHT + CARD_GAP_Y);

                boolean isHovered = mouseX >= cardX && mouseX < cardX + CARD_WIDTH && mouseY >= cardY && mouseY < cardY + CARD_HEIGHT;
                PortalMobRegistry.MobCardEntry entry = filteredMobs.get(i);
                boolean isSelected = entry.id().equals(this.selectedMobId);

                renderCard(graphics, entry, cardX, cardY, isHovered, isSelected, mouseX, mouseY);

                if (isHovered) {
                    hoveredEntry = entry;
                }
            }

            // Show tooltip when hovering over a mob card
            if (hoveredEntry != null) {
                graphics.setComponentTooltipForNextFrame(
                        this.font,
                        List.of(
                                hoveredEntry.displayName(),
                                Component.literal(hoveredEntry.id()).withStyle(ChatFormatting.DARK_GRAY)
                        ),
                        mouseX,
                        mouseY
                );
            }
        }

        // Render Page indicator in footer
        String pageStr = String.format("Pagina %d / %d", currentPage + 1, totalPages);
        graphics.centeredText(this.font, Component.literal(pageStr).withStyle(ChatFormatting.WHITE), this.width / 2, footerY + 1, 0xFFFFFFFF);

        // Render Active Mob indicator at very bottom of footer
        PortalMobRegistry.MobCardEntry selectedEntry = PortalMobRegistry.getById(this.selectedMobId);
        Component mobName = selectedEntry != null ? selectedEntry.displayName() : Component.literal(this.selectedMobId);
        Component statusText = Component.translatable("gui.prova.spawn_portal_wand.current_mob", mobName).withStyle(ChatFormatting.YELLOW);
        graphics.centeredText(this.font, statusText, this.width / 2, footerY + 11, 0xFFFFEE77);

        // Render child widgets (SearchBox & Buttons)
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
            graphics.outline(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 0xFFFFD700);
            graphics.outline(cardX + 1, cardY + 1, CARD_WIDTH - 2, CARD_HEIGHT - 2, 0xFFFFEA75);
        } else if (isHovered) {
            graphics.outline(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 0xFF5CE1E6);
        } else {
            graphics.outline(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 0xFF352E4B);
        }

        // Top Header of card: Mob Name
        Component name = entry.displayName();
        int maxNameWidth = CARD_WIDTH - 6;
        if (font.width(name) > maxNameWidth) {
            String rawStr = font.plainSubstrByWidth(name.getString(), maxNameWidth - 4) + "…";
            name = Component.literal(rawStr);
        }
        int nameColor = isSelected ? 0xFFFFD700 : (isHovered ? 0xFFFFFFFF : 0xFFCCCCCC);
        graphics.centeredText(this.font, name, cardX + CARD_WIDTH / 2, cardY + 4, nameColor);

        // Center: 3D Living Entity Render
        LivingEntity dummy = getOrCreateDummy(entry);
        if (dummy != null) {
            try {
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                        graphics,
                        cardX + 2,
                        cardY + 14,
                        cardX + CARD_WIDTH - 2,
                        cardY + CARD_HEIGHT - 14,
                        entry.previewScale(),
                        entry.previewOffsetY(),
                        mouseX,
                        mouseY,
                        dummy
                );
            } catch (Throwable ignored) {
                graphics.centeredText(this.font, Component.literal("?").withStyle(ChatFormatting.DARK_GRAY),
                        cardX + CARD_WIDTH / 2, cardY + CARD_HEIGHT / 2 - 4, 0xFF888888);
            }
        }

        // Bottom Badge: Category or "✓ ATTIVO"
        int badgeY0 = cardY + CARD_HEIGHT - 12;
        int badgeY1 = cardY + CARD_HEIGHT - 3;
        int badgeX0 = cardX + 3;
        int badgeX1 = cardX + CARD_WIDTH - 3;

        if (isSelected) {
            graphics.fill(badgeX0, badgeY0, badgeX1, badgeY1, 0xFFB8860B);
            graphics.centeredText(this.font,
                    Component.literal("✓ ATTIVO").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD),
                    cardX + CARD_WIDTH / 2, badgeY0 + 1, 0xFF000000);
        } else {
            graphics.fill(badgeX0, badgeY0, badgeX1, badgeY1, 0x44000000);
            Component cat = entry.categoryName();
            if (font.width(cat) > CARD_WIDTH - 8) {
                String shortCat = font.plainSubstrByWidth(cat.getString(), CARD_WIDTH - 10);
                cat = Component.literal(shortCat);
            }
            graphics.centeredText(this.font, cat, cardX + CARD_WIDTH / 2, badgeY0 + 1, entry.categoryColor());
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

            int startIndex = currentPage * cardsPerPage;
            int endIndex = Math.min(startIndex + cardsPerPage, filteredMobs.size());

            for (int i = startIndex; i < endIndex; i++) {
                int localIndex = i - startIndex;
                int col = localIndex % cardsPerRow;
                int row = localIndex / cardsPerRow;

                int cardX = gridStartX + col * (CARD_WIDTH + CARD_GAP_X);
                int cardY = gridStartY + row * (CARD_HEIGHT + CARD_GAP_Y);

                if (mouseX >= cardX && mouseX < cardX + CARD_WIDTH && mouseY >= cardY && mouseY < cardY + CARD_HEIGHT) {
                    PortalMobRegistry.MobCardEntry entry = filteredMobs.get(i);
                    this.selectedMobId = entry.id();

                    if (minecraft != null) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F));
                    }

                    int handIdx = (hand == InteractionHand.OFF_HAND) ? 1 : 0;
                    ClientPacketDistributor.sendToServer(new SelectPortalMobPayload(handIdx, entry.id()));
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            if (scrollY < 0) {
                changePage(1);
            } else {
                changePage(-1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
