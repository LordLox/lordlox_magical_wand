package com.prova.provamod.client.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortalMobRegistry {

    public record MobCardEntry(
            String id,
            EntityType<? extends LivingEntity> entityType,
            Component displayName,
            Component categoryName,
            int categoryColor,
            int previewScale,
            float previewOffsetY,
            int categoryPriority
    ) {}

    private static final List<MobCardEntry> MOBS = new ArrayList<>();
    private static final Map<String, MobCardEntry> BY_ID = new HashMap<>();
    private static boolean initialized = false;

    public static synchronized void ensureInitialized(@Nullable Level level) {
        if (initialized && !MOBS.isEmpty()) {
            return;
        }
        if (level == null) {
            return;
        }

        MOBS.clear();
        BY_ID.clear();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (key == null) {
                continue;
            }

            if (!type.canSummon() || type == EntityType.PLAYER) {
                continue;
            }

            Entity dummy;
            try {
                dummy = type.create(level, EntitySpawnReason.TRIGGERED);
            } catch (Throwable ignored) {
                continue;
            }

            if (dummy == null) {
                continue;
            }

            if (!(dummy instanceof LivingEntity living) || living instanceof ArmorStand || living instanceof Mannequin) {
                continue;
            }

            @SuppressWarnings("unchecked")
            EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;

            MobCardEntry entry = createCardEntry(key, livingType);
            MOBS.add(entry);
            BY_ID.put(entry.id(), entry);
        }

        // Sort by category priority, then alphabetically by localized display name
        MOBS.sort(Comparator.<MobCardEntry>comparingInt(e -> e.categoryPriority())
                .thenComparing(e -> e.displayName().getString()));

        initialized = true;
    }

    private static MobCardEntry createCardEntry(Identifier key, EntityType<? extends LivingEntity> type) {
        String id = key.toString();
        String path = key.getPath();
        Component displayName = type.getDescription();

        Component categoryName;
        int categoryColor;
        int priority;

        MobCategory category = type.getCategory();

        if (type == EntityType.WARDEN || type == EntityType.WITHER || type == EntityType.ENDER_DRAGON || type == EntityType.ELDER_GUARDIAN) {
            categoryName = Component.translatable("gui.prova.spawn_portal_wand.cat_boss");
            categoryColor = 0xFF00AAAA;
            priority = 0;
        } else if (category == MobCategory.MONSTER) {
            categoryName = Component.translatable("gui.prova.spawn_portal_wand.cat_hostile");
            categoryColor = 0xFFFF5555;
            priority = 1;
        } else if (category == MobCategory.CREATURE) {
            categoryName = Component.translatable("gui.prova.spawn_portal_wand.cat_passive");
            categoryColor = 0xFF88AA55;
            priority = 2;
        } else if (category == MobCategory.AXOLOTLS || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT || category == MobCategory.UNDERGROUND_WATER_CREATURE) {
            categoryName = Component.translatable("gui.prova.spawn_portal_wand.cat_aquatic");
            categoryColor = 0xFF55FFFF;
            priority = 3;
        } else if (category == MobCategory.AMBIENT) {
            categoryName = Component.translatable("gui.prova.spawn_portal_wand.cat_ambient");
            categoryColor = 0xFFFFAA55;
            priority = 4;
        } else if (type == EntityType.IRON_GOLEM || type == EntityType.SNOW_GOLEM) {
            categoryName = Component.translatable("gui.prova.spawn_portal_wand.cat_utility");
            categoryColor = 0xFF55FFFF;
            priority = 5;
        } else {
            categoryName = Component.translatable("gui.prova.spawn_portal_wand.cat_other");
            categoryColor = 0xFFDDDDDD;
            priority = 6;
        }

        EntityDimensions dims = type.getDimensions();
        float width = dims.width();
        float height = dims.height();
        float maxDim = Math.max(width, height);
        if (maxDim <= 0.05F) {
            maxDim = 1.8F;
        }

        int scale;
        float offsetY;

        switch (path) {
            case "zombie", "skeleton", "creeper", "stray", "husk", "drowned", "bogged", "parched" -> { scale = 23; offsetY = 0.05F; }
            case "enderman" -> { scale = 15; offsetY = -0.05F; }
            case "warden" -> { scale = 13; offsetY = -0.05F; }
            case "iron_golem" -> { scale = 14; offsetY = 0.0F; }
            case "spider", "cave_spider" -> { scale = 18; offsetY = 0.15F; }
            case "slime", "magma_cube" -> { scale = 22; offsetY = 0.05F; }
            case "allay", "bee", "bat", "vex" -> { scale = 28; offsetY = 0.10F; }
            case "pig", "cow", "sheep", "wolf", "cat", "ocelot", "fox" -> { scale = 20; offsetY = 0.12F; }
            case "villager", "witch", "evoker", "pillager", "vindicator", "illusioner", "wandering_trader" -> { scale = 22; offsetY = 0.05F; }
            case "ender_dragon" -> { scale = 6; offsetY = 0.0F; }
            case "ghast", "happy_ghast" -> { scale = 9; offsetY = 0.05F; }
            case "giant" -> { scale = 6; offsetY = -0.10F; }
            case "wither" -> { scale = 13; offsetY = 0.0F; }
            case "ravager" -> { scale = 11; offsetY = 0.05F; }
            case "sniffer" -> { scale = 10; offsetY = 0.10F; }
            case "camel", "camel_husk", "horse", "donkey", "mule", "skeleton_horse", "zombie_horse" -> { scale = 13; offsetY = 0.05F; }
            case "squid", "glow_squid", "dolphin", "turtle" -> { scale = 17; offsetY = 0.08F; }
            default -> {
                scale = Math.clamp(Math.round(38.0F / maxDim), 6, 28);
                offsetY = (height > 2.2F) ? -0.05F : (height < 1.0F ? 0.12F : 0.05F);
            }
        }

        return new MobCardEntry(id, type, displayName, categoryName, categoryColor, scale, offsetY, priority);
    }

    public static List<MobCardEntry> getAll() {
        return Collections.unmodifiableList(MOBS);
    }

    public static List<MobCardEntry> getAll(@Nullable Level level) {
        ensureInitialized(level);
        return Collections.unmodifiableList(MOBS);
    }

    @Nullable
    public static MobCardEntry getById(String id) {
        MobCardEntry entry = BY_ID.get(id);
        if (entry != null) {
            return entry;
        }
        // Fallback if not yet initialized or queried directly
        Identifier key = Identifier.tryParse(id);
        if (key != null) {
            var opt = BuiltInRegistries.ENTITY_TYPE.getOptional(key);
            if (opt.isPresent() && opt.get().canSummon()) {
                @SuppressWarnings("unchecked")
                EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) opt.get();
                return createCardEntry(key, livingType);
            }
        }
        return null;
    }
}
