package com.lordlox.magicalwands;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.mojang.serialization.Codec;
import com.lordlox.magicalwands.entity.BlackHoleEntity;
import com.lordlox.magicalwands.entity.SpawnPortalEntity;
import com.lordlox.magicalwands.item.BlackHoleWandItem;
import com.lordlox.magicalwands.item.GamblingWandItem;
import com.lordlox.magicalwands.item.GravityWandItem;
import com.lordlox.magicalwands.item.SpawnPortalWandItem;
import com.lordlox.magicalwands.gambling.GamblingRollManager;
import com.lordlox.magicalwands.network.ModNetworking;
import net.neoforged.neoforge.common.NeoForge;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
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

@Mod(MagicalWands.MODID)
public final class MagicalWands {
    public static final String MODID = "lordlox_magical_wands";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    // Data Component for storing selected mob ID
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SELECTED_MOB = DATA_COMPONENT_TYPES.register(
            "selected_mob",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build()
    );

    // Data Component for storing favorite mob IDs list
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> FAVORITE_MOBS = DATA_COMPONENT_TYPES.register(
            "favorite_mobs",
            () -> DataComponentType.<List<String>>builder()
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
                    .build()
    );

    // Data Component for storing wagered diamonds on gambling wand
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GAMBLING_BET = DATA_COMPONENT_TYPES.register(
            "gambling_bet",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

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

    // Spawn Portal Wand custom item
    public static final DeferredItem<SpawnPortalWandItem> SPAWN_PORTAL_WAND = ITEMS.registerItem(
            "spawn_portal_wand",
            SpawnPortalWandItem::new,
            p -> p.durability(100).rarity(Rarity.EPIC)
    );

    // Gambling Wand custom item
    public static final DeferredItem<GamblingWandItem> GAMBLING_WAND = ITEMS.registerItem(
            "gambling_wand",
            GamblingWandItem::new,
            p -> p.durability(150).rarity(Rarity.EPIC)
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

    // Spawn Portal Entity
    public static final DeferredHolder<EntityType<?>, EntityType<SpawnPortalEntity>> SPAWN_PORTAL = ENTITIES.register(
            "spawn_portal",
            () -> EntityType.Builder.<SpawnPortalEntity>of(SpawnPortalEntity::new, MobCategory.MISC)
                    .sized(2.5F, 2.8F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, "spawn_portal")))
    );

    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    // Wand Workbench Block
    public static final net.neoforged.neoforge.registries.DeferredBlock<com.lordlox.magicalwands.block.WandWorkbenchBlock> WAND_WORKBENCH = BLOCKS.registerBlock(
            "wand_workbench",
            com.lordlox.magicalwands.block.WandWorkbenchBlock::new,
            p -> p.strength(2.5F).sound(net.minecraft.world.level.block.SoundType.WOOD)
    );

    // Wand Workbench BlockItem
    public static final DeferredItem<net.minecraft.world.item.BlockItem> WAND_WORKBENCH_ITEM = ITEMS.registerSimpleBlockItem(WAND_WORKBENCH);

    // Wand Workbench MenuType
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.lordlox.magicalwands.world.inventory.WandWorkbenchMenu>> WAND_WORKBENCH_MENU = MENUS.register(
            "wand_workbench",
            () -> new net.minecraft.world.inventory.MenuType<>(com.lordlox.magicalwands.world.inventory.WandWorkbenchMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET)
    );

    // Creative Tab for LordLox's Magical Wands mod items
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAGICAL_WANDS_TAB = CREATIVE_MODE_TABS.register("magical_wands_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.lordlox_magical_wands"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> SPAWN_PORTAL_WAND.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(WAND_WORKBENCH_ITEM.get());
                output.accept(GRAVITY_WAND.get());
                output.accept(BLACK_HOLE_WAND.get());
                output.accept(SPAWN_PORTAL_WAND.get());
                output.accept(GAMBLING_WAND.get());
            })
            .build());

    public MagicalWands(IEventBus modEventBus) {
        // Expand crafting recipe pattern limits to 5x5 for Wand Workbench recipes
        net.minecraft.world.item.crafting.ShapedRecipePattern.setCraftingSize(5, 5);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        DATA_COMPONENT_TYPES.register(modEventBus);

        modEventBus.addListener(ModNetworking::registerPayloads);

        // Register server tick listener for gambling wand roll results
        NeoForge.EVENT_BUS.addListener(GamblingRollManager::onServerTick);
    }
}
