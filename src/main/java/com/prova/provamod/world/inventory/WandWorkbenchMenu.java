package com.prova.provamod.world.inventory;

import com.prova.provamod.Prova;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class WandWorkbenchMenu extends AbstractContainerMenu {
    public static final int GRID_WIDTH = 5;
    public static final int GRID_HEIGHT = 5;
    public static final int RESULT_SLOT = 0;
    public static final int CRAFT_SLOT_START = 1;
    public static final int CRAFT_SLOT_COUNT = 25;
    public static final int CRAFT_SLOT_END = 26;
    public static final int INV_SLOT_START = 26;
    public static final int INV_SLOT_END = 53;
    public static final int HOTBAR_SLOT_START = 53;
    public static final int HOTBAR_SLOT_END = 62;

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, GRID_WIDTH, GRID_HEIGHT);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    public WandWorkbenchMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public WandWorkbenchMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(Prova.WAND_WORKBENCH_MENU.get(), containerId);
        this.access = access;
        this.player = playerInventory.player;

        // Slot 0: Result Slot
        this.addSlot(new ResultSlot(playerInventory.player, this.craftSlots, this.resultSlots, 0, 152, 54));

        // Slots 1..25: 5x5 Crafting Grid
        int gridX = 14;
        int gridY = 18;
        for (int row = 0; row < GRID_HEIGHT; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                int slotIndex = col + row * GRID_WIDTH;
                this.addSlot(new Slot(this.craftSlots, slotIndex, gridX + col * 18, gridY + row * 18));
            }
        }

        // Slots 26..52: Player Main Inventory (3 rows of 9)
        int invX = 20;
        int invY = 120;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + (row + 1) * 9, invX + col * 18, invY + row * 18));
            }
        }

        // Slots 53..61: Player Hotbar (9 slots)
        int hotbarX = 20;
        int hotbarY = 178;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, hotbarX + col * 18, hotbarY));
        }
    }

    protected static void slotChangedCraftingGrid(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer container,
            ResultContainer resultSlots,
            @Nullable RecipeHolder<CraftingRecipe> recipeHint
    ) {
        CraftingInput input = container.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> maybeRecipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level, recipeHint);
        if (maybeRecipe.isPresent()) {
            RecipeHolder<CraftingRecipe> recipeHolder = maybeRecipe.get();
            CraftingRecipe craftingRecipe = recipeHolder.value();
            if (resultSlots.setRecipeUsed(serverPlayer, recipeHolder)) {
                ItemStack recipeResult = craftingRecipe.assemble(input);
                if (recipeResult.isItemEnabled(level.enabledFeatures())) {
                    result = recipeResult;
                }
            }
        }

        resultSlots.setItem(0, result);
        menu.setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, result));
    }

    @Override
    public void slotsChanged(Container container) {
        this.access.execute((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                slotChangedCraftingGrid(this, serverLevel, this.player, this.craftSlots, this.resultSlots, null);
            }
        });
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Prova.WAND_WORKBENCH.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == RESULT_SLOT) {
                stack.getItem().onCraftedBy(stack, player);
                if (!this.moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex >= INV_SLOT_START && slotIndex < HOTBAR_SLOT_END) {
                if (!this.moveItemStackTo(stack, CRAFT_SLOT_START, CRAFT_SLOT_END, false)) {
                    if (slotIndex < HOTBAR_SLOT_START) {
                        if (!this.moveItemStackTo(stack, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_START, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
            if (slotIndex == RESULT_SLOT) {
                player.drop(stack, false);
            }
        }
        return clicked;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    public Slot getResultSlot() {
        return this.slots.get(RESULT_SLOT);
    }

    public CraftingContainer getCraftSlots() {
        return this.craftSlots;
    }
}
