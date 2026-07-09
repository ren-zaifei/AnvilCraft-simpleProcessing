package com.renzaifei.asp.entity.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

public abstract class SimpleBlockEntity extends BlockEntity {
    /**
     * 此构造函数的input与output槽位会自动构建
     * @param type
     * @param pos
     * @param blockState
     */
    public SimpleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.input = createInventory();
        this.output = createInventory();
    }

    /**
     * 此构造函数需手动传入input与output
     * @param type
     * @param pos
     * @param blockState
     * @param input
     * @param output
     */
    public SimpleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState ,ItemStackHandler input ,ItemStackHandler output) {
        super(type, pos, blockState);
        this.input = input;
        this.output = output;
    }

    /**
     * 输入槽位实例
     */
    protected final ItemStackHandler input;
    /**
     * 输出槽位实例
     */
    protected final ItemStackHandler output;

    /**
     * 创建槽位，在构造函数中被调用
     *
     */
    protected ItemStackHandler createInventory() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return true;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                for (int i = 0; i < getSlots(); i++) {
                    if (i == slot) continue;
                    ItemStack other = getStackInSlot(i);
                    if (!other.isEmpty()
                            && ItemStack.isSameItemSameComponents(other, stack)) {
                        return stack;
                    }
                }
                return super.insertItem(slot, stack, simulate);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                syncToClient();
            }
        };
    }

    /**
     *  同步客户端
     */
    public void syncToClient() {
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            ClientboundBlockEntityDataPacket packet = (ClientboundBlockEntityDataPacket) getUpdatePacket();
            if (packet != null) {
                serverLevel.players().forEach(p -> p.connection.send(packet));
            }
        }
    }

    /**
     *
     * 获取input槽位实例
     * @param side 交互的方位，可传null
     *
     */
    public IItemHandler getInputItemHandler(@Nullable Direction side) {
        return input;
    }
    /**
     *
     * 获取output槽位实例
     * @param side 交互的方位，可传null
     *
     */
    public IItemHandler getOutputItemHandler(@Nullable Direction side) {
        return output;
    }

    /**
     * 构建并返回自动吸取范围
     *
     */
    public AABB getPickupArea() {
        return new AABB(
                worldPosition.getX() + 1, worldPosition.getY() + 1.25, worldPosition.getZ() + 1,
                worldPosition.getX(), worldPosition.getY() + 0.75, worldPosition.getZ()
        );
    }

    /**
     * 获取用于同步到客户端的数据包
     * @param registries
     * @return
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    /**
     * 读入NBT并应用到本地 BlockEntity
     * @param tag
     * @param registries
     */
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        loadAdditional(tag, registries);
    }

    /**
     * 处理从服务器接收的更新包
     * @return
     */
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * 服务端tick逻辑
     * @param level
     * @param pos
     * @param state
     * @param be
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SimpleBlockEntity be) {
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        be.pickupItems();
    }


    /**
     * 保存数据到 NBT
     * @param tag
     * @param registries
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("input", input.serializeNBT(registries));
        tag.put("output", output.serializeNBT(registries));
    }

    /**
     * 从 NBT 加载数据
     * @param tag
     * @param registries
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input.deserializeNBT(registries, tag.getCompound("input"));
        output.deserializeNBT(registries, tag.getCompound("output"));
    }

    /**
     * 吸取物品逻辑，在serverTick中调用
     */
    protected void pickupItems() {
        AABB area = getPickupArea();
        if (area == null) return;
        IItemHandler handler = this.getInputItemHandler(null);
        assert level != null;
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area);
        for (ItemEntity itemEntity : items) {
            this.insertSlot(itemEntity,handler);
        }
    }

    /**
     * 尝试将物品存入 handler——优先堆叠同种，否则占空槽。同一物品最多占一个槽位。
     *
     * @return 未能存入的剩余物品（全部存入则返回 {@code ItemStack.EMPTY}）
     */
    public static ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
        int slots = handler.getSlots();
        int targetSlot = -1;

        // 1. 检查是否已有同种物品：有则只能堆叠到该槽位，满了就拒收
        for (int s = 0; s < slots; s++) {
            ItemStack inSlot = handler.getStackInSlot(s);
            if (!inSlot.isEmpty()
                    && ItemStack.isSameItemSameComponents(inSlot, stack)) {
                int max = Math.min(inSlot.getMaxStackSize(), 64);
                if (inSlot.getCount() >= max) {
                    return stack.copy(); // 已满，拒收
                }
                targetSlot = s;
                break;
            }
        }
        // 2. 没有同种 → 找空槽
        if (targetSlot == -1) {
            for (int s = 0; s < slots; s++) {
                if (handler.getStackInSlot(s).isEmpty()) {
                    targetSlot = s;
                    break;
                }
            }
        }
        // 3. 无处可放 → 返回原物品
        if (targetSlot == -1) return stack.copy();

        // 4. 执行放入
        ItemStack slotStack = handler.getStackInSlot(targetSlot);
        int max = Math.min(slotStack.isEmpty() ? 64 : slotStack.getMaxStackSize(), 64);
        int space = max - slotStack.getCount();
        int toTake = Math.min(space, stack.getCount());
        ItemStack leftover = stack.copyWithCount(stack.getCount() - toTake);

        if (slotStack.isEmpty()) {
            handler.insertItem(targetSlot, stack.copyWithCount(toTake), false);
        } else {
            slotStack.grow(toTake);
            if (handler instanceof ItemStackHandler ish) {
                ish.setStackInSlot(targetSlot, slotStack);
            }
        }
        return leftover;
    }

    /**
     * 存入槽位，其中的一种物品最多存入一个槽位中
     * @param itemEntity
     * @param handler
     */
    protected void insertSlot(ItemEntity itemEntity, IItemHandler handler) {
        if (!itemEntity.isAlive()) return;
        ItemStack leftover = insertIntoHandler(handler, itemEntity.getItem());
        if (leftover.getCount() < itemEntity.getItem().getCount()) {
            itemEntity.setItem(leftover);
            if (leftover.isEmpty()) {
                itemEntity.discard();
            }
        }
    }

}
