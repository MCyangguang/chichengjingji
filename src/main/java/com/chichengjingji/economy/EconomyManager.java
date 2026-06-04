package com.chichengjingji.economy;

import com.chichengjingji.Chichengjingji;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager extends SavedData {
    private static final String DATA_NAME = Chichengjingji.MODID + "_economy";
    private final Map<UUID, PlayerBalance> balances = new ConcurrentHashMap<>();

    public static EconomyManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(EconomyManager::new, (tag, registries) -> new EconomyManager(tag, registries)),
                DATA_NAME);
    }

    public EconomyManager() {}
    public EconomyManager(CompoundTag tag, HolderLookup.Provider registries) { load(tag, registries); }

    private void load(CompoundTag tag, HolderLookup.Provider registries) {
        balances.clear();
        ListTag list = tag.getList("Balances", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            PlayerBalance pb = new PlayerBalance(list.getCompound(i));
            balances.put(pb.getPlayerUUID(), pb);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (PlayerBalance pb : balances.values()) list.add(pb.toNBT());
        tag.put("Balances", list);
        return tag;
    }

    public PlayerBalance getOrCreate(UUID uuid, String name) {
        return balances.computeIfAbsent(uuid, k -> {
            PlayerBalance pb = new PlayerBalance(uuid, name);
            long starting = com.chichengjingji.Config.STARTING_BALANCE.get();
            pb.setBalance(starting);
            return pb;
        });
    }

    public long getBalance(UUID uuid) { return balances.getOrDefault(uuid, new PlayerBalance(uuid, "")).getBalance(); }
    public void addBalance(UUID uuid, String name, long amount) { getOrCreate(uuid, name).add(amount); setDirty(); }
    public boolean removeBalance(UUID uuid, String name, long amount) { if (getBalance(uuid) < amount) return false; getOrCreate(uuid, name).subtract(amount); setDirty(); return true; }
    public void setBalance(UUID uuid, String name, long amount) { getOrCreate(uuid, name).setBalance(amount); setDirty(); }
    public boolean transfer(UUID from, String fromName, UUID to, String toName, long amount) {
        if (getBalance(from) < amount) return false;
        getOrCreate(from, fromName).subtract(amount);
        getOrCreate(to, toName).add(amount);
        setDirty();
        return true;
    }
    public void ensurePlayer(UUID uuid, String name) { getOrCreate(uuid, name); }
}