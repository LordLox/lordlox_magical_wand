package com.prova.provamod.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PortalMobRegistry {

    public record MobCardEntry(
            String id,
            EntityType<? extends LivingEntity> entityType,
            Component displayName,
            Component categoryName,
            int categoryColor,
            int previewScale,
            float previewOffsetY
    ) {}

    private static final List<MobCardEntry> MOBS = new ArrayList<>();

    static {
        // Page 1: Hostile Mobs
        register("minecraft:zombie", EntityType.ZOMBIE, Component.translatable("entity.minecraft.zombie"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_hostile"), 0xFFFF5555, 27, 0.05F);

        register("minecraft:skeleton", EntityType.SKELETON, Component.translatable("entity.minecraft.skeleton"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_hostile"), 0xFFFF5555, 27, 0.05F);

        register("minecraft:creeper", EntityType.CREEPER, Component.translatable("entity.minecraft.creeper"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_hostile"), 0xFF55FF55, 26, 0.05F);

        register("minecraft:spider", EntityType.SPIDER, Component.translatable("entity.minecraft.spider"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_hostile"), 0xFFFF7755, 20, 0.15F);

        register("minecraft:enderman", EntityType.ENDERMAN, Component.translatable("entity.minecraft.enderman"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_hostile"), 0xFFAA00AA, 17, -0.05F);

        register("minecraft:blaze", EntityType.BLAZE, Component.translatable("entity.minecraft.blaze"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_hostile"), 0xFFFFAA00, 24, 0.05F);

        register("minecraft:witch", EntityType.WITCH, Component.translatable("entity.minecraft.witch"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_hostile"), 0xFFCC44CC, 25, 0.05F);

        register("minecraft:slime", EntityType.SLIME, Component.translatable("entity.minecraft.slime"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_hostile"), 0xFF55FF77, 26, 0.05F);

        // Page 2: Passive, Friendly & Boss Mobs
        register("minecraft:iron_golem", EntityType.IRON_GOLEM, Component.translatable("entity.minecraft.iron_golem"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_utility"), 0xFF55FFFF, 16, 0.0F);

        register("minecraft:wolf", EntityType.WOLF, Component.translatable("entity.minecraft.wolf"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_neutral"), 0xFFFFAA55, 26, 0.12F);

        register("minecraft:pig", EntityType.PIG, Component.translatable("entity.minecraft.pig"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_passive"), 0xFFFF88AA, 26, 0.15F);

        register("minecraft:cow", EntityType.COW, Component.translatable("entity.minecraft.cow"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_passive"), 0xFF88AA55, 24, 0.10F);

        register("minecraft:sheep", EntityType.SHEEP, Component.translatable("entity.minecraft.sheep"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_passive"), 0xFFDDDDDD, 24, 0.10F);

        register("minecraft:villager", EntityType.VILLAGER, Component.translatable("entity.minecraft.villager"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_passive"), 0xFF55AAFF, 25, 0.05F);

        register("minecraft:allay", EntityType.ALLAY, Component.translatable("entity.minecraft.allay"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_passive"), 0xFF55FFFF, 34, 0.10F);

        register("minecraft:warden", EntityType.WARDEN, Component.translatable("entity.minecraft.warden"),
                Component.translatable("gui.prova.spawn_portal_wand.cat_boss"), 0xFF00AAAA, 13, -0.05F);
    }

    private static void register(String id, EntityType<? extends LivingEntity> type, Component name,
                                 Component category, int color, int scale, float offsetY) {
        MOBS.add(new MobCardEntry(id, type, name, category, color, scale, offsetY));
    }

    public static List<MobCardEntry> getAll() {
        return Collections.unmodifiableList(MOBS);
    }

    @Nullable
    public static MobCardEntry getById(String id) {
        for (MobCardEntry entry : MOBS) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }
}
