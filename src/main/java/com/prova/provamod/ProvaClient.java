package com.prova.provamod;

import com.prova.provamod.client.renderer.BlackHoleRenderer;
import com.prova.provamod.client.renderer.SpawnPortalRenderer;
import com.prova.provamod.network.SelectPortalMobPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import com.prova.provamod.client.gui.GamblingD20Overlay;
import com.prova.provamod.portal.PortalMobConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.List;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Prova.MODID, dist = Dist.CLIENT)
public final class ProvaClient {
    public ProvaClient(IEventBus modEventBus) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerGuiLayers);
        modEventBus.addListener(this::registerMenuScreens);

        // Register gameplay listeners on NeoForge game event bus
        NeoForge.EVENT_BUS.addListener(this::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(this::onItemTooltip);
    }

    private void registerMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(Prova.WAND_WORKBENCH_MENU.get(), com.prova.provamod.client.gui.WandWorkbenchScreen::new);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        // Client-specific initialization
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Prova.BLACK_HOLE.get(), BlackHoleRenderer::new);
        event.registerEntityRenderer(Prova.SPAWN_PORTAL.get(), SpawnPortalRenderer::new);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(Prova.MODID, "gambling_d20_overlay"),
                GamblingD20Overlay::render
        );
    }

    private void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        // Only handle when no screen is open
        if (mc.screen != null) {
            return;
        }

        // Check if Shift is down
        boolean isShift = player.isShiftKeyDown() || mc.options.keyShift.isDown();
        if (!isShift) {
            return;
        }

        // Check if player holds the spawn portal wand in either hand
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(Prova.SPAWN_PORTAL_WAND.get())) {
            if (player.getOffhandItem().is(Prova.SPAWN_PORTAL_WAND.get())) {
                stack = player.getOffhandItem();
                hand = InteractionHand.OFF_HAND;
            } else {
                return;
            }
        }

        double scrollY = event.getScrollDeltaY();
        if (scrollY == 0) {
            return;
        }

        // Intercept scroll: cancel vanilla hotbar slot change!
        event.setCanceled(true);

        List<String> favorites = stack.get(Prova.FAVORITE_MOBS.get());
        if (favorites == null || favorites.isEmpty()) {
            player.sendOverlayMessage(
                    Component.translatable("item.prova.spawn_portal_wand.no_favorites")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            );
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.2F));
            return;
        }

        String currentMob = stack.get(Prova.SELECTED_MOB.get());
        int currentIndex = (currentMob != null) ? favorites.indexOf(currentMob) : -1;

        int nextIndex;
        if (scrollY < 0) {
            // Scroll down -> next favorite
            nextIndex = (currentIndex + 1) % favorites.size();
        } else {
            // Scroll up -> previous favorite
            nextIndex = (currentIndex - 1 + favorites.size()) % favorites.size();
        }

        String nextMobId = favorites.get(nextIndex);

        // Update locally immediately for instant feedback
        stack.set(Prova.SELECTED_MOB.get(), nextMobId);

        // Sound feedback
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.6F));

        // Display overlay message
        Identifier id = Identifier.tryParse(nextMobId);
        Component mobName = id != null ? BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                .map(EntityType::getDescription)
                .orElseGet(() -> Component.translatable("entity." + nextMobId.replace(':', '.')))
                : Component.translatable("entity." + nextMobId.replace(':', '.'));

        int cost = com.prova.provamod.portal.PortalMobConfig.getDiamondCost(nextMobId);
        float rate = com.prova.provamod.portal.PortalMobConfig.getSpawnIntervalSeconds(nextMobId);
        Component statBadge = Component.literal(String.format(java.util.Locale.ROOT, " [◆%d | ⏱%.1fs]", cost, rate))
                .withStyle(ChatFormatting.AQUA);

        Component message = Component.translatable(
                "item.prova.spawn_portal_wand.favorite_selected",
                mobName,
                nextIndex + 1,
                favorites.size()
        ).append(statBadge)
        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        player.sendOverlayMessage(message);

        // Send to server
        int handIdx = (hand == InteractionHand.OFF_HAND) ? 1 : 0;
        ClientPacketDistributor.sendToServer(new SelectPortalMobPayload(handIdx, nextMobId));
    }

    private void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(Prova.SPAWN_PORTAL_WAND.get())) {
            addSpawnPortalWandTooltip(stack, event.getToolTip());
        } else if (stack.is(Prova.GAMBLING_WAND.get())) {
            addGamblingWandTooltip(stack, event.getToolTip());
        } else if (stack.is(Prova.BLACK_HOLE_WAND.get())) {
            addBlackHoleWandTooltip(stack, event.getToolTip());
        } else if (stack.is(Prova.GRAVITY_WAND.get())) {
            addGravityWandTooltip(stack, event.getToolTip());
        }
    }

    private void addGravityWandTooltip(ItemStack stack, List<Component> tooltip) {
        int index = Math.min(1, tooltip.size());
        tooltip.add(index++, Component.translatable("item.prova.gravity_wand.desc1").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(index++, Component.translatable("item.prova.gravity_wand.desc2").withStyle(ChatFormatting.AQUA));
    }

    private void addSpawnPortalWandTooltip(ItemStack stack, List<Component> tooltip) {
        int index = Math.min(1, tooltip.size());

        String selectedMob = stack.get(Prova.SELECTED_MOB.get());
        if (selectedMob != null && !selectedMob.isEmpty()) {
            Identifier id = Identifier.tryParse(selectedMob);
            Component mobName = id != null ? BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                    .map(EntityType::getDescription)
                    .orElseGet(() -> Component.translatable("entity." + selectedMob.replace(':', '.')))
                    : Component.translatable("entity." + selectedMob.replace(':', '.'));

            tooltip.add(index++,
                    Component.translatable("item.prova.spawn_portal_wand.selected", mobName)
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            );

            int cost = PortalMobConfig.getDiamondCost(selectedMob);
            float intervalSec = PortalMobConfig.getSpawnIntervalSeconds(selectedMob);
            tooltip.add(index++,
                    Component.translatable("item.prova.spawn_portal_wand.cost_info", cost, String.format(java.util.Locale.ROOT, "%.1f", intervalSec))
                            .withStyle(ChatFormatting.AQUA)
            );
        } else {
            tooltip.add(index++,
                    Component.translatable("item.prova.spawn_portal_wand.none_selected")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
            );
        }

        List<String> favorites = stack.get(Prova.FAVORITE_MOBS.get());
        if (favorites != null && !favorites.isEmpty()) {
            tooltip.add(index++,
                    Component.translatable("item.prova.spawn_portal_wand.favorites_count", favorites.size())
                            .withStyle(ChatFormatting.LIGHT_PURPLE)
            );
        }

        tooltip.add(index++, Component.translatable("item.prova.spawn_portal_wand.desc1").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(index++, Component.translatable("item.prova.spawn_portal_wand.desc_scroll").withStyle(ChatFormatting.YELLOW));
        tooltip.add(index++, Component.translatable("item.prova.spawn_portal_wand.desc2").withStyle(ChatFormatting.AQUA));
        tooltip.add(index++, Component.translatable("item.prova.spawn_portal_wand.desc3").withStyle(ChatFormatting.DARK_AQUA));
    }

    private void addGamblingWandTooltip(ItemStack stack, List<Component> tooltip) {
        int index = Math.min(1, tooltip.size());
        int bet = stack.getOrDefault(Prova.GAMBLING_BET.get(), 0);
        tooltip.add(index++,
                Component.translatable("item.prova.gambling_wand.current_bet", bet)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        );

        tooltip.add(index++, Component.translatable("item.prova.gambling_wand.desc1").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(index++, Component.translatable("item.prova.gambling_wand.desc2").withStyle(ChatFormatting.YELLOW));
        tooltip.add(index++, Component.translatable("item.prova.gambling_wand.desc3").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
    }

    private void addBlackHoleWandTooltip(ItemStack stack, List<Component> tooltip) {
        int index = Math.min(1, tooltip.size());
        tooltip.add(index++, Component.translatable("item.prova.black_hole_wand.desc1").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(index++, Component.translatable("item.prova.black_hole_wand.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(index++, Component.translatable("item.prova.black_hole_wand.desc3").withStyle(ChatFormatting.DARK_AQUA));
    }
}
