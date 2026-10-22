package com.prova.provamod.portal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.HashMap;
import java.util.Map;

/**
 * Central configuration registry defining diamond costs and spawn intervals for all mobs
 * summoned via the Spawn Portal Wand. Safe for both server and client use.
 */
public final class PortalMobConfig {

    public record MobProperty(int diamondCost, int spawnIntervalTicks) {}

    private static final Map<String, MobProperty> SPECIFIC_PROPERTIES = new HashMap<>();

    static {
        // --- APEX BOSSES (Extremely rare, destructive, prestigious) ---
        register("minecraft:ender_dragon", 32, 240); // 12.0s
        register("minecraft:wither", 24, 180);       // 9.0s
        register("minecraft:warden", 24, 180);       // 9.0s

        // --- MINI-BOSSES & HIGH THREAT / SIEGE MOBS ---
        register("minecraft:elder_guardian", 12, 120); // 6.0s
        register("minecraft:giant", 12, 120);          // 6.0s
        register("minecraft:ravager", 10, 100);        // 5.0s
        register("minecraft:iron_golem", 8, 90);       // 4.5s
        register("minecraft:evoker", 8, 80);           // 4.0s
        register("minecraft:piglin_brute", 7, 75);     // 3.75s

        // --- ELITE / NETHER / END / TRIAL CHAMBER MOBS ---
        register("minecraft:breeze", 6, 70);           // 3.5s
        register("minecraft:ghast", 6, 80);            // 4.0s
        register("minecraft:happy_ghast", 6, 80);      // 4.0s
        register("minecraft:shulker", 6, 70);          // 3.5s
        register("minecraft:illusioner", 6, 70);       // 3.5s
        register("minecraft:wither_skeleton", 5, 60);  // 3.0s
        register("minecraft:enderman", 5, 60);         // 3.0s
        register("minecraft:vindicator", 5, 60);       // 3.0s
        register("minecraft:witch", 5, 60);            // 3.0s
        register("minecraft:sniffer", 5, 80);          // 4.0s
        register("minecraft:guardian", 4, 60);         // 3.0s
        register("minecraft:blaze", 4, 55);            // 2.75s
        register("minecraft:hoglin", 4, 50);           // 2.5s
        register("minecraft:zoglin", 4, 50);           // 2.5s
        register("minecraft:pillager", 4, 50);         // 2.5s

        // --- STANDARD HOSTILE MOBS ---
        register("minecraft:creeper", 3, 45);          // 2.25s
        register("minecraft:bogged", 3, 45);           // 2.25s
        register("minecraft:drowned", 3, 45);          // 2.25s
        register("minecraft:phantom", 3, 45);          // 2.25s
        register("minecraft:skeleton", 3, 40);         // 2.0s
        register("minecraft:stray", 3, 40);            // 2.0s
        register("minecraft:husk", 3, 40);             // 2.0s
        register("minecraft:cave_spider", 3, 40);      // 2.0s
        register("minecraft:spider", 2, 40);           // 2.0s
        register("minecraft:zombified_piglin", 3, 40); // 2.0s
        register("minecraft:piglin", 3, 40);           // 2.0s
        register("minecraft:magma_cube", 2, 35);       // 1.75s
        register("minecraft:slime", 2, 35);            // 1.75s
        register("minecraft:zombie", 2, 35);           // 1.75s
        register("minecraft:silverfish", 2, 30);       // 1.5s
        register("minecraft:endermite", 2, 30);        // 1.5s
        register("minecraft:vex", 2, 30);              // 1.5s

        // --- UTILITY / TRADERS / PETS / MOUNTS ---
        register("minecraft:villager", 4, 60);         // 3.0s
        register("minecraft:wandering_trader", 3, 50); // 2.5s
        register("minecraft:snow_golem", 2, 40);       // 2.0s
        register("minecraft:horse", 2, 40);            // 2.0s
        register("minecraft:donkey", 2, 40);           // 2.0s
        register("minecraft:mule", 2, 40);             // 2.0s
        register("minecraft:skeleton_horse", 2, 40);   // 2.0s
        register("minecraft:zombie_horse", 2, 40);     // 2.0s
        register("minecraft:camel", 2, 40);            // 2.0s
        register("minecraft:camel_husk", 2, 40);       // 2.0s
        register("minecraft:llama", 2, 40);            // 2.0s
        register("minecraft:trader_llama", 2, 40);     // 2.0s
        register("minecraft:wolf", 2, 35);             // 1.75s
        register("minecraft:cat", 2, 35);              // 1.75s
        register("minecraft:ocelot", 2, 35);           // 1.75s
        register("minecraft:fox", 2, 35);              // 1.75s
        register("minecraft:panda", 2, 35);            // 1.75s
        register("minecraft:polar_bear", 2, 35);       // 1.75s

        // --- COMMON PASSIVES & AMBIENT ANIMALS ---
        register("minecraft:cow", 1, 25);              // 1.25s
        register("minecraft:pig", 1, 25);              // 1.25s
        register("minecraft:sheep", 1, 25);            // 1.25s
        register("minecraft:chicken", 1, 25);          // 1.25s
        register("minecraft:rabbit", 1, 25);           // 1.25s
        register("minecraft:goat", 1, 25);             // 1.25s
        register("minecraft:mooshroom", 1, 25);        // 1.25s
        register("minecraft:armadillo", 1, 25);        // 1.25s
        register("minecraft:strider", 1, 25);          // 1.25s
        register("minecraft:bee", 1, 25);              // 1.25s
        register("minecraft:allay", 1, 25);            // 1.25s
        register("minecraft:bat", 1, 25);              // 1.25s
        register("minecraft:axolotl", 1, 25);          // 1.25s
        register("minecraft:frog", 1, 25);             // 1.25s
        register("minecraft:tadpole", 1, 25);          // 1.25s
        register("minecraft:turtle", 1, 25);           // 1.25s
        register("minecraft:dolphin", 1, 25);          // 1.25s
        register("minecraft:squid", 1, 25);            // 1.25s
        register("minecraft:glow_squid", 1, 25);       // 1.25s
        register("minecraft:pufferfish", 1, 25);       // 1.25s
        register("minecraft:salmon", 1, 25);           // 1.25s
        register("minecraft:cod", 1, 25);              // 1.25s
        register("minecraft:tropical_fish", 1, 25);    // 1.25s
    }

    private static void register(String mobId, int diamondCost, int spawnIntervalTicks) {
        SPECIFIC_PROPERTIES.put(mobId, new MobProperty(Math.max(1, diamondCost), Math.max(10, spawnIntervalTicks)));
    }

    /**
     * Resolves the configured diamond cost and spawn interval for a given mob ID.
     * Uses dynamic fallbacks based on MobCategory for unlisted or modded entities.
     */
    public static MobProperty getProperties(String mobId) {
        if (mobId == null || mobId.isEmpty()) {
            return new MobProperty(2, 40);
        }

        MobProperty prop = SPECIFIC_PROPERTIES.get(mobId);
        if (prop != null) {
            return prop;
        }

        // Dynamic fallback using EntityType category
        Identifier id = Identifier.tryParse(mobId);
        if (id != null) {
            var optionalType = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            if (optionalType.isPresent()) {
                EntityType<?> type = optionalType.get();
                if (type == EntityType.WARDEN || type == EntityType.WITHER || type == EntityType.ENDER_DRAGON || type == EntityType.ELDER_GUARDIAN) {
                    return new MobProperty(24, 180);
                }
                MobCategory category = type.getCategory();
                if (category == MobCategory.MONSTER) {
                    return new MobProperty(3, 45);
                }
                if (category == MobCategory.CREATURE || category == MobCategory.AMBIENT
                        || category == MobCategory.AXOLOTLS || category == MobCategory.WATER_CREATURE
                        || category == MobCategory.WATER_AMBIENT || category == MobCategory.UNDERGROUND_WATER_CREATURE) {
                    return new MobProperty(1, 25);
                }
            }
        }

        return new MobProperty(2, 40);
    }

    public static int getDiamondCost(String mobId) {
        return getProperties(mobId).diamondCost();
    }

    public static int getSpawnInterval(String mobId) {
        return getProperties(mobId).spawnIntervalTicks();
    }

    public static float getSpawnIntervalSeconds(String mobId) {
        return getSpawnInterval(mobId) / 20.0F;
    }

    public static Component getSpeedDescriptor(int intervalTicks) {
        if (intervalTicks <= 30) {
            return Component.translatable("gui.prova.spawn_portal_wand.speed_very_fast");
        } else if (intervalTicks <= 45) {
            return Component.translatable("gui.prova.spawn_portal_wand.speed_fast");
        } else if (intervalTicks <= 75) {
            return Component.translatable("gui.prova.spawn_portal_wand.speed_medium");
        } else if (intervalTicks <= 110) {
            return Component.translatable("gui.prova.spawn_portal_wand.speed_slow");
        } else {
            return Component.translatable("gui.prova.spawn_portal_wand.speed_very_slow");
        }
    }

    public static int getCostColor(int cost) {
        if (cost <= 1) {
            return 0xFF55FFFF; // Aqua
        } else if (cost <= 3) {
            return 0xFF55FF55; // Light green
        } else if (cost <= 7) {
            return 0xFFFFD700; // Gold
        } else if (cost <= 12) {
            return 0xFFFFAA00; // Orange
        } else {
            return 0xFFFF5555; // Red (Apex / Boss)
        }
    }

    public static int getCadenceColor(int intervalTicks) {
        if (intervalTicks <= 30) {
            return 0xFF55FF55; // Green (Very fast)
        } else if (intervalTicks <= 50) {
            return 0xFFFFFFFF; // White (Normal)
        } else if (intervalTicks <= 80) {
            return 0xFFFFD700; // Gold (Medium-slow)
        } else {
            return 0xFFFF7777; // Soft red (Slow / Boss)
        }
    }

    private PortalMobConfig() {}
}
