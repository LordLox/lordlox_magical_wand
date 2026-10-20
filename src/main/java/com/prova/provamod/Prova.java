package com.prova.provamod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.prova.provamod.entity.BlackHoleEntity;
import com.prova.provamod.item.BlackHoleWandItem;
import com.prova.provamod.item.GravityWandItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Rarity;
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
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Gravity Wand custom item
    public static final DeferredItem<GravityWandItem> GRAVITY_WAND = ITEMS.registerItem(
            "gravity_wand",
            GravityWandItem::new,
            p -> p.durability(250).rarity(Rarity.RARE)
    );

    // Black Hole Wand custom item
    public static final DeferredItem<BlackHoleWandItem> BLACK_HOLE_WAND = ITEMS.registerItem(
            "black_hole_wand",
            BlackHoleWandItem::new,
            p -> p.durability(100).rarity(Rarity.EPIC)
    );

    // Black Hole Entity
    public static final DeferredHolder<EntityType<?>, EntityType<BlackHoleEntity>> BLACK_HOLE = ENTITIES.register(
            "black_hole",
            () -> EntityType.Builder.<BlackHoleEntity>of(BlackHoleEntity::new, MobCategory.MISC)
                    .sized(1.2F, 1.2F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, "black_hole")))
    );

    // Creative Tab for Prova mod items
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PROVA_TAB = CREATIVE_MODE_TABS.register("prova_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.prova"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> BLACK_HOLE_WAND.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(GRAVITY_WAND.get());
                output.accept(BLACK_HOLE_WAND.get());
            })
            .build());

    public Prova(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
