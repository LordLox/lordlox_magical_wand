package com.lordlox.magicalwands.portal;

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
        register("minecraft:ender_dragon", 45, 240); // 12.0s
        register("minecraft:wither", 32, 180);       // 9.0s
        register("minecraft:warden", 32, 180);       // 9.0s

        // --- MINI-BOSSES & HIGH THREAT / SIEGE MOBS ---
        register("minecraft:elder_guardian", 16, 120); // 6.0s
        register("minecraft:giant", 16, 120);          // 6.0s
        register("minecraft:ravager", 14, 100);        // 5.0s
        register("minecraft:iron_golem", 12, 90);       // 4.5s
        register("minecraft:evoker", 12, 80);           // 4.0s
        register("minecraft:piglin_brute", 10, 75);     // 3.75s

        // --- ELITE / NETHER / END / TRIAL CHAMBER MOBS ---
        register("minecraft:breeze", 8, 70);           // 3.5s
        register("minecraft:ghast", 8, 80);            // 4.0s
        register("minecraft:happy_ghast", 8, 80);      // 4.0s
        register("minecraft:shulker", 9, 70);          // 3.5s
        register("minecraft:illusioner", 8, 70);       // 3.5s
        register("minecraft:wither_skeleton", 7, 60);  // 3.0s
        register("minecraft:enderman", 7, 60);         // 3.0s
        register("minecraft:vindicator", 7, 60);       // 3.0s
        register("minecraft:witch", 7, 60);            // 3.0s
        register("minecraft:sniffer", 7, 80);          // 4.0s
        register("minecraft:guardian", 6, 60);         // 3.0s
        register("minecraft:blaze", 6, 55);            // 2.75s
        register("minecraft:hoglin", 5, 50);           // 2.5s
        register("minecraft:zoglin", 5, 50);           // 2.5s
        register("minecraft:pillager", 5, 50);         // 2.5s

        // --- STANDARD HOSTILE MOBS ---
        register("minecraft:creeper", 4, 45);          // 2.25s
        register("minecraft:bogged", 4, 45);           // 2.25s
        register("minecraft:drowned", 4, 45);          // 2.25s
        register("minecraft:phantom", 4, 45);          // 2.25s
        register("minecraft:skeleton", 4, 40);         // 2.0s
        register("minecraft:stray", 4, 40);            // 2.0s
        register("minecraft:husk", 4, 40);             // 2.0s
        register("minecraft:cave_spider", 4, 40);      // 2.0s
        register("minecraft:spider", 3, 40);           // 2.0s
        register("minecraft:zombified_piglin", 4, 40); // 2.0s
        register("minecraft:piglin", 4, 40);           // 2.0s
        register("minecraft:magma_cube", 3, 35);       // 1.75s
        register("minecraft:slime", 3, 35);            // 1.75s
        register("minecraft:zombie", 3, 35);           // 1.75s
        register("minecraft:silverfish", 3, 30);       // 1.5s
        register("minecraft:endermite", 3, 30);        // 1.5s
        register("minecraft:vex", 3, 30);              // 1.5s

        // --- UTILITY / TRADERS / PETS / MOUNTS ---
        register("minecraft:villager", 6, 60);         // 3.0s
        register("minecraft:wandering_trader", 5, 50); // 2.5s
        register("minecraft:snow_golem", 3, 40);       // 2.0s
        register("minecraft:horse", 3, 40);            // 2.0s
        register("minecraft:donkey", 3, 40);           // 2.0s
        register("minecraft:mule", 3, 40);             // 2.0s
        register("minecraft:skeleton_horse", 3, 40);   // 2.0s
        register("minecraft:zombie_horse", 3, 40);     // 2.0s
        register("minecraft:camel", 3, 40);            // 2.0s
        register("minecraft:camel_husk", 3, 40);       // 2.0s
        register("minecraft:llama", 3, 40);            // 2.0s
        register("minecraft:trader_llama", 3, 40);     // 2.0s
        register("minecraft:wolf", 3, 35);             // 1.75s
        register("minecraft:cat", 3, 35);              // 1.75s
        register("minecraft:ocelot", 3, 35);           // 1.75s
        register("minecraft:fox", 3, 35);              // 1.75s
        register("minecraft:panda", 3, 35);            // 1.75s
        register("minecraft:polar_bear", 3, 35);       // 1.75s

        // --- COMMON PASSIVES & AMBIENT ANIMALS ---
        register("minecraft:cow", 2, 25);              // 1.25s
        register("minecraft:pig", 2, 25);              // 1.25s
        register("minecraft:sheep", 2, 25);            // 1.25s
        register("minecraft:chicken", 2, 25);          // 1.25s
        register("minecraft:rabbit", 2, 25);           // 1.25s
        register("minecraft:goat", 2, 25);             // 1.25s
        register("minecraft:mooshroom", 2, 25);        // 1.25s
        register("minecraft:armadillo", 2, 25);        // 1.25s
        register("minecraft:strider", 2, 25);          // 1.25s
        register("minecraft:bee", 2, 25);              // 1.25s
        register("minecraft:allay", 3, 25);            // 1.25s
        register("minecraft:bat", 2, 25);              // 1.25s
        register("minecraft:axolotl", 2, 25);          // 1.25s
        register("minecraft:frog", 2, 25);             // 1.25s
        register("minecraft:tadpole", 2, 25);          // 1.25s
        register("minecraft:turtle", 2, 25);           // 1.25s
        register("minecraft:dolphin", 2, 25);          // 1.25s
        register("minecraft:squid", 2, 25);            // 1.25s
        register("minecraft:glow_squid", 2, 25);       // 1.25s
        register("minecraft:pufferfish", 2, 25);       // 1.25s
        register("minecraft:salmon", 2, 25);           // 1.25s
        register("minecraft:cod", 2, 25);              // 1.25s
        register("minecraft:tropical_fish", 2, 25);    // 1.25s
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
            return new MobProperty(3, 40);
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
                    return new MobProperty(32, 180);
                }
                MobCategory category = type.getCategory();
                if (category == MobCategory.MONSTER) {
                    return new MobProperty(4, 45);
                }
                if (category == MobCategory.CREATURE || category == MobCategory.AMBIENT
                        || category == MobCategory.AXOLOTLS || category == MobCategory.WATER_CREATURE
                        || category == MobCategory.WATER_AMBIENT || category == MobCategory.UNDERGROUND_WATER_CREATURE) {
                    return new MobProperty(2, 25);
                }
            }
        }

        return new MobProperty(3, 40);
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
            return Component.translatable("gui.lordlox_magical_wands.spawn_portal_wand.speed_very_fast");
        } else if (intervalTicks <= 45) {
            return Component.translatable("gui.lordlox_magical_wands.spawn_portal_wand.speed_fast");
        } else if (intervalTicks <= 75) {
            return Component.translatable("gui.lordlox_magical_wands.spawn_portal_wand.speed_medium");
        } else if (intervalTicks <= 110) {
            return Component.translatable("gui.lordlox_magical_wands.spawn_portal_wand.speed_slow");
        } else {
            return Component.translatable("gui.lordlox_magical_wands.spawn_portal_wand.speed_very_slow");
        }
    }

    public static int getCostColor(int cost) {
        if (cost <= 2) {
            return 0xFF55FFFF; // Aqua
        } else if (cost <= 5) {
            return 0xFF55FF55; // Light green
        } else if (cost <= 10) {
            return 0xFFFFD700; // Gold
        } else if (cost <= 16) {
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
