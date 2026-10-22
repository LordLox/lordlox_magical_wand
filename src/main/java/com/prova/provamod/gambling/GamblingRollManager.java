package com.prova.provamod.gambling;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class GamblingRollManager {

    public static class ActiveRoll {
        public final UUID playerId;
        public final int rollResult;
        public final int betAmount;
        public int remainingTicks;

        public ActiveRoll(UUID playerId, int rollResult, int betAmount, int remainingTicks) {
            this.playerId = playerId;
            this.rollResult = rollResult;
            this.betAmount = betAmount;
            this.remainingTicks = remainingTicks;
        }
    }

    private static final List<ActiveRoll> ACTIVE_ROLLS = new ArrayList<>();

    private static final List<EntityType<?>> HOSTILE_MOBS = List.of(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER,
            EntityType.ENDERMAN,
            EntityType.WITCH,
            EntityType.PILLAGER,
            EntityType.VINDICATOR,
            EntityType.DROWNED,
            EntityType.HUSK,
            EntityType.STRAY,
            EntityType.CAVE_SPIDER
    );

    private static final List<EntityType<?>> PASSIVE_MOBS = List.of(
            EntityType.COW,
            EntityType.PIG,
            EntityType.SHEEP,
            EntityType.CHICKEN,
            EntityType.RABBIT,
            EntityType.CAT,
            EntityType.WOLF,
            EntityType.FOX,
            EntityType.FROG,
            EntityType.PARROT,
            EntityType.PANDA,
            EntityType.AXOLOTL,
            EntityType.VILLAGER
    );

    public static void startRoll(ServerPlayer player, int rollResult, int betAmount, int durationTicks) {
        ACTIVE_ROLLS.add(new ActiveRoll(player.getUUID(), rollResult, betAmount, durationTicks));
    }

    public static boolean isPlayerRolling(UUID playerId) {
        for (ActiveRoll roll : ACTIVE_ROLLS) {
            if (roll.playerId.equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE_ROLLS.isEmpty()) {
            return;
        }

        MinecraftServer server = event.getServer();
        Iterator<ActiveRoll> iterator = ACTIVE_ROLLS.iterator();

        while (iterator.hasNext()) {
            ActiveRoll roll = iterator.next();
            roll.remainingTicks--;

            if (roll.remainingTicks <= 0) {
                iterator.remove();
                ServerPlayer player = server.getPlayerList().getPlayer(roll.playerId);
                if (player != null && player.isAlive()) {
                    executeOutcome(player, roll.rollResult, roll.betAmount);
                }
            }
        }
    }

    public static void executeOutcome(ServerPlayer player, int rollResult, int betAmount) {
        ServerLevel level = player.level();

        if (rollResult == 1) {
            // 1: muori
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
            if (lightning != null) {
                lightning.setPos(player.position());
                lightning.setVisualOnly(true);
                level.addFreshEntity(lightning);
            }

            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.8F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.5F, 0.7F);

            player.sendSystemMessage(Component.translatable("item.prova.gambling_wand.result.fail_death").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

            // Instant lethal damage
            player.kill(level);
        } else if (rollResult >= 2 && rollResult <= 7) {
            // 2-7: spawnano da 2 a 5 mob aggressivi casuali non boss
            int count = 2 + level.getRandom().nextInt(4); // 2 to 5
            for (int i = 0; i < count; i++) {
                EntityType<?> type = HOSTILE_MOBS.get(level.getRandom().nextInt(HOSTILE_MOBS.size()));
                double angle = (2 * Math.PI * i / count) + (level.getRandom().nextDouble() * 0.4 - 0.2);
                double dist = 2.5 + level.getRandom().nextDouble() * 2.0;
                double x = player.getX() + Math.cos(angle) * dist;
                double z = player.getZ() + Math.sin(angle) * dist;
                BlockPos ground = findGround(level, BlockPos.containing(x, player.getY(), z));

                Entity entity = type.create(level, EntitySpawnReason.EVENT);
                if (entity != null) {
                    entity.setPos(x, ground.getY() + 1.0, z);
                    entity.setYRot(level.getRandom().nextFloat() * 360F);
                    if (entity instanceof Mob mob) {
                        mob.setTarget(player);
                    }
                    level.addFreshEntity(entity);
                    level.sendParticles(ParticleTypes.SMOKE, x, ground.getY() + 1.0, z, 15, 0.3, 0.5, 0.3, 0.05);
                    level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, ground.getY() + 1.5, z, 3, 0.2, 0.2, 0.2, 0.0);
                }
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.6F);
            player.sendOverlayMessage(Component.translatable("item.prova.gambling_wand.result.hostile_mobs", count).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        } else if (rollResult >= 8 && rollResult <= 10) {
            // 8-10: spawnano da 2 a 5 mob passivi casuali
            int count = 2 + level.getRandom().nextInt(4); // 2 to 5
            for (int i = 0; i < count; i++) {
                EntityType<?> type = PASSIVE_MOBS.get(level.getRandom().nextInt(PASSIVE_MOBS.size()));
                double angle = (2 * Math.PI * i / count) + (level.getRandom().nextDouble() * 0.4 - 0.2);
                double dist = 2.5 + level.getRandom().nextDouble() * 2.0;
                double x = player.getX() + Math.cos(angle) * dist;
                double z = player.getZ() + Math.sin(angle) * dist;
                BlockPos ground = findGround(level, BlockPos.containing(x, player.getY(), z));

                Entity entity = type.create(level, EntitySpawnReason.EVENT);
                if (entity != null) {
                    entity.setPos(x, ground.getY() + 1.0, z);
                    entity.setYRot(level.getRandom().nextFloat() * 360F);
                    level.addFreshEntity(entity);
                    level.sendParticles(ParticleTypes.HEART, x, ground.getY() + 1.2, z, 5, 0.3, 0.3, 0.3, 0.0);
                }
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1.2F, 1.0F);
            player.sendOverlayMessage(Component.translatable("item.prova.gambling_wand.result.passive_mobs", count).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else if (rollResult >= 11 && rollResult <= 15) {
            // 11-15: spawnano alcuni minerali comuni, come ferro, coal e copper
            List<ItemStack> drops = List.of(
                    new ItemStack(Items.COAL, 8 + level.getRandom().nextInt(9)),
                    new ItemStack(Items.RAW_IRON, 6 + level.getRandom().nextInt(7)),
                    new ItemStack(Items.RAW_COPPER, 8 + level.getRandom().nextInt(9)),
                    new ItemStack(Items.IRON_INGOT, 2 + level.getRandom().nextInt(4)),
                    new ItemStack(Items.COPPER_INGOT, 2 + level.getRandom().nextInt(4))
            );

            for (ItemStack drop : drops) {
                ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY() + 0.8, player.getZ(), drop);
                itemEntity.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.3, 0.35, (level.getRandom().nextDouble() - 0.5) * 0.3);
                level.addFreshEntity(itemEntity);
            }

            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.0);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendOverlayMessage(Component.translatable("item.prova.gambling_wand.result.common_minerals").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        } else if (rollResult >= 16 && rollResult <= 19) {
            // 16-19: spawnano alcuni minerali rari come gold, redstone, lapis e diamanti
            List<ItemStack> drops = List.of(
                    new ItemStack(Items.RAW_GOLD, 6 + level.getRandom().nextInt(7)),
                    new ItemStack(Items.GOLD_INGOT, 4 + level.getRandom().nextInt(5)),
                    new ItemStack(Items.REDSTONE, 12 + level.getRandom().nextInt(13)),
                    new ItemStack(Items.LAPIS_LAZULI, 12 + level.getRandom().nextInt(13)),
                    new ItemStack(Items.DIAMOND, 3 + level.getRandom().nextInt(5)),
                    new ItemStack(Items.EMERALD, 2 + level.getRandom().nextInt(4))
            );

            for (ItemStack drop : drops) {
                ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY() + 0.8, player.getZ(), drop);
                itemEntity.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.35, 0.4, (level.getRandom().nextDouble() - 0.5) * 0.35);
                level.addFreshEntity(itemEntity);
            }

            level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.6, 0.6, 0.6, 0.1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.1F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.3F);
            player.sendOverlayMessage(Component.translatable("item.prova.gambling_wand.result.rare_minerals").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        } else if (rollResult == 20) {
            // 20: spawnano uno stack di diamanti (64x)
            ItemEntity diamondStack = new ItemEntity(level, player.getX(), player.getY() + 0.8, player.getZ(), new ItemStack(Items.DIAMOND, 64));
            diamondStack.setDeltaMovement(0, 0.45, 0);
            level.addFreshEntity(diamondStack);

            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.7, 0.7, 0.7, 0.2);
            level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.5, player.getZ(), 40, 0.5, 0.5, 0.5, 0.15);

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.5F, 1.0F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.2F, 1.2F);

            player.sendOverlayMessage(Component.translatable("item.prova.gambling_wand.result.jackpot").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.translatable("item.prova.gambling_wand.result.jackpot_chat").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    private static BlockPos findGround(ServerLevel level, BlockPos pos) {
        for (int dy = 3; dy >= -5; dy--) {
            BlockPos check = pos.above(dy);
            BlockState state = level.getBlockState(check);
            if (!state.isAir() && (!state.getCollisionShape(level, check).isEmpty() || state.isFaceSturdy(level, check, net.minecraft.core.Direction.UP))) {
                return check;
            }
        }
        return pos.below();
    }
}
