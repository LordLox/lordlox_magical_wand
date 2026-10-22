package com.prova.provamod.client.gui;

import com.prova.provamod.network.SelectPortalMobPayload;
import com.prova.provamod.network.SetPortalMobFavoritePayload;
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
    private final List<String> favoriteMobIds = new ArrayList<>();
    private boolean showOnlyFavorites = false;

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
    private Button filterFavoritesButton;
    private Button prevButton;
    private Button nextButton;

    public SpawnPortalWandScreen(InteractionHand hand, String currentSelectedMobId) {
        this(hand, currentSelectedMobId, List.of());
    }

    public SpawnPortalWandScreen(InteractionHand hand, String currentSelectedMobId, List<String> initialFavorites) {
        super(Component.translatable("gui.prova.spawn_portal_wand.title"));
        this.hand = hand;
        this.selectedMobId = currentSelectedMobId != null ? currentSelectedMobId : "minecraft:zombie";
        if (initialFavorites != null) {
            this.favoriteMobIds.addAll(initialFavorites);
        }

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

        // Search Bar & Favorites Filter Button in header
        int btnFavWidth = 74;
        int searchBoxWidth = totalGridWidth - btnFavWidth - 4;
        int searchBoxX = gridStartX;
        int searchBoxY = panelY0 + 18;

        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, searchBoxWidth, 16,
                Component.translatable("gui.prova.spawn_portal_wand.search_hint"));
        this.searchBox.setHint(Component.translatable("gui.prova.spawn_portal_wand.search_hint").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setMaxLength(36);
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(this::onSearchQueryChanged);
        this.addRenderableWidget(this.searchBox);

        this.filterFavoritesButton = this.addRenderableWidget(
                Button.builder(getFilterButtonText(), b -> {
                    this.showOnlyFavorites = !this.showOnlyFavorites;
                    this.filterFavoritesButton.setMessage(getFilterButtonText());
                    if (minecraft != null) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                    }
                    updateFilteredMobs();
                })
                .bounds(gridStartX + totalGridWidth - btnFavWidth, searchBoxY - 1, btnFavWidth, 18)
                .build()
        );

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

    private Component getFilterButtonText() {
        return showOnlyFavorites
                ? Component.translatable("gui.prova.spawn_portal_wand.filter_favs_active").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                : Component.translatable("gui.prova.spawn_portal_wand.filter_favs_all").withStyle(ChatFormatting.WHITE);
    }

    private void onSearchQueryChanged(String query) {
        this.searchQuery = query.trim().toLowerCase();
        updateFilteredMobs();
    }

    private void updateFilteredMobs() {
        this.filteredMobs.clear();
        for (PortalMobRegistry.MobCardEntry entry : this.allMobs) {
            if (this.showOnlyFavorites && !this.favoriteMobIds.contains(entry.id())) {
                continue;
            }
            if (!this.searchQuery.isEmpty()) {
                String name = entry.displayName().getString().toLowerCase();
                String id = entry.id().toLowerCase();
                if (!name.contains(this.searchQuery) && !id.contains(this.searchQuery)) {
                    continue;
                }
            }
            this.filteredMobs.add(entry);
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
            Component emptyMessage = showOnlyFavorites
                    ? Component.translatable("gui.prova.spawn_portal_wand.no_favorites_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                    : Component.translatable("gui.prova.spawn_portal_wand.no_results").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            graphics.centeredText(this.font, emptyMessage, this.width / 2, gridStartY + totalGridHeight / 2 - 4, 0xFFAAAAAA);
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

            // Show rich tooltip when hovering over a mob card
            if (hoveredEntry != null) {
                List<Component> tooltipLines = new ArrayList<>();
                tooltipLines.add(hoveredEntry.displayName());
                tooltipLines.add(Component.literal(hoveredEntry.id()).withStyle(ChatFormatting.DARK_GRAY));

                if (this.favoriteMobIds.contains(hoveredEntry.id())) {
                    tooltipLines.add(Component.translatable("gui.prova.spawn_portal_wand.is_favorite").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                }

                tooltipLines.add(Component.translatable("gui.prova.spawn_portal_wand.hint_select").withStyle(ChatFormatting.GRAY));
                tooltipLines.add(Component.translatable("gui.prova.spawn_portal_wand.hint_favorite").withStyle(ChatFormatting.AQUA));

                graphics.setComponentTooltipForNextFrame(
                        this.font,
                        tooltipLines,
                        mouseX,
                        mouseY
                );
            }
        }

        // Render Page indicator in footer
        String pageStr = String.format("Pagina %d / %d", currentPage + 1, totalPages);
        graphics.centeredText(this.font, Component.literal(pageStr).withStyle(ChatFormatting.WHITE), this.width / 2, footerY + 1, 0xFFFFFFFF);

        // Render Active Mob and Favorites count in footer
        PortalMobRegistry.MobCardEntry selectedEntry = PortalMobRegistry.getById(this.selectedMobId);
        Component mobName = selectedEntry != null ? selectedEntry.displayName() : Component.literal(this.selectedMobId);
        Component statusText = Component.translatable(
                "gui.prova.spawn_portal_wand.footer_status",
                mobName,
                favoriteMobIds.size()
        ).withStyle(ChatFormatting.YELLOW);
        graphics.centeredText(this.font, statusText, this.width / 2, footerY + 11, 0xFFFFEE77);

        // Render child widgets (SearchBox & Buttons)
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void renderCard(GuiGraphicsExtractor graphics, PortalMobRegistry.MobCardEntry entry,
                            int cardX, int cardY, boolean isHovered, boolean isSelected,
                            int mouseX, int mouseY) {
        boolean isFavorite = this.favoriteMobIds.contains(entry.id());

        // Card Background
        int bgColor = isSelected ? 0xEE2A1E45 : (isHovered ? 0xEE201B35 : 0xEE161325);
        graphics.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, bgColor);

        // Card Border
        if (isSelected) {
            graphics.outline(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 0xFFFFD700);
            graphics.outline(cardX + 1, cardY + 1, CARD_WIDTH - 2, CARD_HEIGHT - 2, 0xFFFFEA75);
        } else if (isHovered) {
            graphics.outline(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 0xFF5CE1E6);
        } else if (isFavorite) {
            // Subtle gold accent border for favorites even when unhovered
            graphics.outline(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 0xFF9E782F);
        } else {
            graphics.outline(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, 0xFF352E4B);
        }

        // Top Header of card: Mob Name
        Component name = entry.displayName();
        int maxNameWidth = CARD_WIDTH - 14;
        if (font.width(name) > maxNameWidth) {
            String rawStr = font.plainSubstrByWidth(name.getString(), maxNameWidth - 4) + "…";
            name = Component.literal(rawStr);
        }
        int nameColor = isSelected ? 0xFFFFD700 : (isHovered ? 0xFFFFFFFF : 0xFFCCCCCC);
        graphics.text(this.font, name, cardX + 4, cardY + 4, nameColor);

        // Star Icon in top right corner
        int starX = cardX + CARD_WIDTH - 10;
        int starY = cardY + 4;
        boolean isHoveringStar = mouseX >= starX - 2 && mouseX <= cardX + CARD_WIDTH && mouseY >= cardY && mouseY <= cardY + 12;

        if (isFavorite) {
            int starColor = isHoveringStar ? 0xFFFFF275 : 0xFFFFD700;
            graphics.text(this.font, "★", starX, starY, starColor);
        } else if (isHovered || isHoveringStar) {
            int starColor = isHoveringStar ? 0xFFFFD700 : 0xFF777788;
            graphics.text(this.font, "☆", starX, starY, starColor);
        }

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
                int starX = cardX + CARD_WIDTH - 12;
                boolean isStarClicked = (mouseX >= starX - 2 && mouseX <= cardX + CARD_WIDTH && mouseY >= cardY && mouseY <= cardY + 14);

                // Right click ON the card OR Left click on the star icon -> Toggle Favorite
                if (event.button() == 1 || (event.button() == 0 && isStarClicked)) {
                    toggleFavorite(entry.id());
                    return true;
                }

                // Left click on card -> Select Mob
                if (event.button() == 0) {
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

    private void toggleFavorite(String mobId) {
        boolean nowFavorite;
        if (this.favoriteMobIds.contains(mobId)) {
            this.favoriteMobIds.remove(mobId);
            nowFavorite = false;
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_SNARE.value(), 1.0F));
            }
        } else {
            this.favoriteMobIds.add(mobId);
            nowFavorite = true;
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.4F));
            }
        }

        int handIdx = (hand == InteractionHand.OFF_HAND) ? 1 : 0;
        ClientPacketDistributor.sendToServer(new SetPortalMobFavoritePayload(handIdx, mobId, nowFavorite));

        if (showOnlyFavorites) {
            updateFilteredMobs();
        }
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
