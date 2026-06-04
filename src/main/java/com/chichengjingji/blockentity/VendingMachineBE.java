package com.chichengjingji.blockentity;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.init.ModBlockEntities;
import com.chichengjingji.network.SyncMachinePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VendingMachineBE extends BlockEntity {
    // 全局注册表：所有者UUID -> 该玩家拥有的售货机列表（仅服务端）
    private static final ConcurrentHashMap<UUID, List<VendingMachineBE>> ownerMachines = new ConcurrentHashMap<>();

    private UUID ownerUUID;
    private String ownerName;
    private List<ShopItem> items = new ArrayList<>();
    private long earnings = 0;

    public static class ShopItem {
        public ItemStack item;
        public long price;
        public int stock;
        public ShopItem() {}
        public ShopItem(ItemStack item, long price, int stock) {
            this.item = item;
            this.price = price;
            this.stock = stock;
        }
    }

    public VendingMachineBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VENDING_MACHINE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && ownerUUID != null) {
            registerToGlobal();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (ownerUUID != null) {
            List<VendingMachineBE> list = ownerMachines.get(ownerUUID);
            if (list != null) {
                list.remove(this);
                if (list.isEmpty()) {
                    ownerMachines.remove(ownerUUID);
                }
            }
        }
    }

    private void registerToGlobal() {
        ownerMachines.computeIfAbsent(ownerUUID, k -> Collections.synchronizedList(new ArrayList<>())).add(this);
    }

    public void setOwner(UUID uuid, String name) {
        // 移除旧所有者的注册
        if (this.ownerUUID != null) {
            List<VendingMachineBE> oldList = ownerMachines.get(this.ownerUUID);
            if (oldList != null) {
                oldList.remove(this);
                if (oldList.isEmpty()) ownerMachines.remove(this.ownerUUID);
            }
        }
        this.ownerUUID = uuid;
        this.ownerName = name;
        setChanged();
        if (level != null && !level.isClientSide && uuid != null) {
            registerToGlobal();
        }
    }

    public boolean isOwner(UUID uuid) {
        return ownerUUID != null && ownerUUID.equals(uuid);
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public List<ShopItem> getItems() {
        return items;
    }

    public long getEarnings() {
        return earnings;
    }

    public void addEarnings(long amount) {
        earnings += amount;
        setChanged();
    }

    public long withdrawEarnings() {
        long e = earnings;
        earnings = 0;
        setChanged();
        return e;
    }

    public void addItem(ItemStack item, long price, int amount) {
        if (item.isEmpty()) return;
        for (ShopItem si : items) {
            if (ItemStack.isSameItemSameComponents(si.item, item)) {
                si.stock += amount;
                setChanged();
                return;
            }
        }
        ShopItem si = new ShopItem();
        si.item = item.copy();
        si.price = price;
        si.stock = amount;
        items.add(si);
        setChanged();
    }

    public boolean reduceStock(int index, int amount) {
        if (index < 0 || index >= items.size()) return false;
        ShopItem si = items.get(index);
        if (si.stock < amount) return false;
        si.stock -= amount;
        if (si.stock == 0) items.remove(index);
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
            tag.putString("OwnerName", ownerName);
        }
        tag.putLong("Earnings", earnings);
        ListTag list = new ListTag();
        for (ShopItem si : items) {
            CompoundTag t = new CompoundTag();
            t.put("Item", si.item.save(registries, new CompoundTag()));
            t.putLong("Price", si.price);
            t.putInt("Stock", si.stock);
            list.add(t);
        }
        tag.put("Items", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
            ownerName = tag.getString("OwnerName");
        }
        earnings = tag.getLong("Earnings");
        items.clear();
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            ItemStack loadedItem = ItemStack.parse(registries, t.getCompound("Item")).orElse(ItemStack.EMPTY);
            if (loadedItem.isEmpty()) {
                Chichengjingji.LOGGER.warn("Skipping invalid item at index {}", i);
                continue;
            }
            ShopItem si = new ShopItem();
            si.item = loadedItem;
            si.price = t.getLong("Price");
            si.stock = t.getInt("Stock");
            items.add(si);
        }
    }

    public void setEarnings(long earnings) {
        this.earnings = earnings;
        setChanged();
    }

    public void updateFromSync(List<SyncMachinePacket.ItemData> items, long earnings) {
        this.items.clear();
        if (level == null) return;
        for (SyncMachinePacket.ItemData data : items) {
            ItemStack stack = data.toStack(level.registryAccess());
            if (stack.isEmpty()) continue;
            this.items.add(new ShopItem(stack.copy(), data.price(), data.stock()));
        }
        this.earnings = earnings;
    }

    // 供外部获取某玩家的所有售货机（仅服务端调用）
    public static List<VendingMachineBE> getMachinesForOwner(UUID ownerId) {
        return ownerMachines.getOrDefault(ownerId, Collections.emptyList());
    }
}