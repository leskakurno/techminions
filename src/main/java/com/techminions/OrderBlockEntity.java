package com.techminions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Текущий контракт Блока заказов: что сдать, сколько уже сдано, награда. Один контракт за раз. */
public class OrderBlockEntity extends BlockEntity {
    private String item = "";
    private int need;
    private int delivered;
    private int reward;
    private UUID owner;

    public OrderBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.ORDER_BE.get(), pos, state);
    }

    public boolean active() {
        return need > 0 && !item.isEmpty();
    }

    public void start(UUID owner, Item item, int need, int reward) {
        this.owner = owner;
        this.item = BuiltInRegistries.ITEM.getKey(item).toString();
        this.need = need;
        this.delivered = 0;
        this.reward = reward;
        setChanged();
    }

    public Item item() {
        ResourceLocation id = ResourceLocation.tryParse(item);
        return id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
    }

    public int remaining() {
        return Math.max(0, need - delivered);
    }

    public int need() {
        return need;
    }

    public int delivered() {
        return delivered;
    }

    public int reward() {
        return reward;
    }

    public UUID owner() {
        return owner;
    }

    public void deliver(int count) {
        delivered += count;
        setChanged();
    }

    public void clear() {
        item = "";
        need = 0;
        delivered = 0;
        reward = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Item", item);
        tag.putInt("Need", need);
        tag.putInt("Delivered", delivered);
        tag.putInt("Reward", reward);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        item = tag.getString("Item");
        need = tag.getInt("Need");
        delivered = tag.getInt("Delivered");
        reward = tag.getInt("Reward");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
