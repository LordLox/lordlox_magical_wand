package com.prova.provamod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Rarity;
import com.prova.provamod.item.GravityWandItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Prova.MODID)
public final class Prova {
    public static final String MODID = "prova";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Gravity Wand custom item
    public static final DeferredItem<GravityWandItem> GRAVITY_WAND = ITEMS.registerItem(
            "gravity_wand",
            GravityWandItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE)
    );

    // Creative Tab for Prova mod items
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PROVA_TAB = CREATIVE_MODE_TABS.register("prova_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.prova"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> GRAVITY_WAND.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(GRAVITY_WAND.get()))
            .build());

    public Prova(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
