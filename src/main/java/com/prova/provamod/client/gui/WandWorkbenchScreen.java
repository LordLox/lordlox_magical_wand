package com.prova.provamod.client.gui;

import com.prova.provamod.Prova;
import com.prova.provamod.world.inventory.WandWorkbenchMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class WandWorkbenchScreen extends AbstractContainerScreen<WandWorkbenchMenu> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Prova.MODID, "textures/gui/wand_workbench.png");

    public WandWorkbenchScreen(WandWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 200, 206);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 14;
        this.titleLabelY = 6;
        this.inventoryLabelX = 20;
        this.inventoryLabelY = 108;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;

        // Render main background GUI panel
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        long time = System.currentTimeMillis();

        // 1. Result slot magical glow when a wand / item is crafted
        boolean hasResult = this.menu.getResultSlot().hasItem();
        if (hasResult) {
            float pulse = (float) Math.sin(time * 0.006) * 0.5F + 0.5F;
            int alpha = (int) (60 + pulse * 120);
            int glowColor = (alpha << 24) | 0xDF9FFF;
            int innerGlow = (alpha << 24) | 0xFFD700;

            int resX = xo + 152;
            int resY = yo + 54;
            graphics.fill(resX - 4, resY - 4, resX + 20, resY + 20, glowColor);
            graphics.outline(resX - 5, resY - 5, 26, 26, innerGlow);
        }

        // 2. Animated mystical rune circle & channeling runes between grid and result slot
        renderArcaneRunes(graphics, xo + 128, yo + 63, time, hasResult);

        // 3. Floating ambient sparkles / arcane dust particles across the crafting interface
        renderFloatingSparkles(graphics, xo, yo, time);
    }

    private void renderArcaneRunes(GuiGraphicsExtractor graphics, int cx, int cy, long time, boolean active) {
        float speed = active ? 0.003F : 0.001F;
        float angle = (time * speed) % (float) (Math.PI * 2);

        // Outer rotating arcane points
        int radius = 10;
        int numPoints = 6;
        int color = active ? 0xFFE082 : 0x7E57C2;

        for (int i = 0; i < numPoints; i++) {
            float a = angle + i * ((float) Math.PI * 2 / numPoints);
            int px = cx + (int) (Math.cos(a) * radius);
            int py = cy + (int) (Math.sin(a) * radius);
            graphics.fill(px, py, px + 2, py + 2, (0xCC << 24) | color);
        }

        // Inner glowing core
        int coreAlpha = active ? (int) (180 + Math.sin(time * 0.008) * 60) : 100;
        int coreColor = active ? 0xFFF9C4 : 0xBA68C8;
        graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, (coreAlpha << 24) | coreColor);

        // Mystic connection beam / arrow shimmer when active
        if (active) {
            float beamPulse = (float) Math.sin(time * 0.01) * 0.5F + 0.5F;
            int beamAlpha = (int) (80 + beamPulse * 140);
            graphics.fill(cx + 8, cy - 1, cx + 18, cy + 2, (beamAlpha << 24) | 0xFFD54F);
        }
    }

    private void renderFloatingSparkles(GuiGraphicsExtractor graphics, int xo, int yo, long time) {
        int[] sparkleColors = {
                0xFFD700, // Gold
                0xC77DFF, // Arcane Violet
                0x70E000, // Emerald
                0x48CAE4  // Cyan Sparkle
        };

        // 8 persistent drifting sparkle particles
        for (int i = 0; i < 8; i++) {
            float seed = i * 137.5F;
            float life = ((time + i * 850) % 4000) / 4000.0F; // 0.0 to 1.0

            // Floating from bottom-ish to top of the crafting box
            int startX = 18 + (int) ((seed * 11) % 80);
            int startY = 95 - (int) ((seed * 7) % 65);

            float driftX = (float) Math.sin(time * 0.002 + seed) * 4.0F;
            float driftY = -life * 28.0F;

            int px = xo + startX + (int) driftX;
            int py = yo + startY + (int) driftY;

            // Fade in and out
            float alphaFactor = (float) Math.sin(life * Math.PI);
            int alpha = (int) (alphaFactor * 220);
            if (alpha > 20) {
                int col = sparkleColors[i % sparkleColors.length];
                graphics.fill(px, py, px + 1, py + 1, (alpha << 24) | col);
            }
        }
    }
}
