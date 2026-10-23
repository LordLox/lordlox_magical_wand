package com.lordlox.magicalwands.block;

import com.mojang.serialization.MapCodec;
import com.lordlox.magicalwands.block.state.WandWorkbenchPart;
import com.lordlox.magicalwands.world.inventory.WandWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class WandWorkbenchBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<WandWorkbenchBlock> MAP_CODEC = simpleCodec(WandWorkbenchBlock::new);
    public static final EnumProperty<WandWorkbenchPart> PART = EnumProperty.create("part", WandWorkbenchPart.class);
    private static final Component CONTAINER_TITLE = Component.translatable("container.lordlox_magical_wands.wand_workbench");

    @Override
    public MapCodec<WandWorkbenchBlock> codec() {
        return MAP_CODEC;
    }

    public WandWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, WandWorkbenchPart.LEFT));
    }

    public static Direction getNeighbourDirection(WandWorkbenchPart part, Direction facing) {
        return part == WandWorkbenchPart.LEFT ? facing.getCounterClockWise() : facing.getClockWise();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        BlockPos rightPos = pos.relative(facing.getCounterClockWise());
        Level level = context.getLevel();
        return level.getBlockState(rightPos).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(rightPos)
                ? this.defaultBlockState().setValue(FACING, facing).setValue(PART, WandWorkbenchPart.LEFT)
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        if (!level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            BlockPos rightPos = pos.relative(facing.getCounterClockWise());
            level.setBlock(rightPos, state.setValue(PART, WandWorkbenchPart.RIGHT), Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        }
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        Direction expectedNeighborDir = getNeighbourDirection(state.getValue(PART), state.getValue(FACING));
        if (directionToNeighbour == expectedNeighborDir) {
            WandWorkbenchPart expectedPart = (state.getValue(PART) == WandWorkbenchPart.LEFT) ? WandWorkbenchPart.RIGHT : WandWorkbenchPart.LEFT;
            return (neighbourState.is(this) && neighbourState.getValue(FACING) == state.getValue(FACING) && neighbourState.getValue(PART) == expectedPart)
                    ? state
                    : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            WandWorkbenchPart part = state.getValue(PART);
            Direction facing = state.getValue(FACING);
            BlockPos otherPos = pos.relative(getNeighbourDirection(part, facing));
            BlockState otherState = level.getBlockState(otherPos);
            WandWorkbenchPart expectedOtherPart = (part == WandWorkbenchPart.LEFT) ? WandWorkbenchPart.RIGHT : WandWorkbenchPart.LEFT;

            if (otherState.is(this) && otherState.getValue(FACING) == facing && otherState.getValue(PART) == expectedOtherPart) {
                // Clear the other half on both server and client (UPDATE_ALL), suppressing duplicate drops
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockPos mainPos = (state.getValue(PART) == WandWorkbenchPart.LEFT)
                    ? pos
                    : pos.relative(getNeighbourDirection(state.getValue(PART), state.getValue(FACING)));
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new WandWorkbenchMenu(containerId, inventory, ContainerLevelAccess.create(level, mainPos)),
                    CONTAINER_TITLE
            ));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}
