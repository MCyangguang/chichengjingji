package com.chichengjingji.economy;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public class PlayerBalance {
    private final UUID playerUUID;
    private String playerName;
    private long balance;

    public PlayerBalance(UUID uuid, String name) { this.playerUUID = uuid; this.playerName = name; this.balance = 0; }
    public PlayerBalance(CompoundTag tag) {
        this.playerUUID = tag.getUUID("UUID");
        this.playerName = tag.getString("Name");
        this.balance = tag.getLong("Balance");
    }
    public CompoundTag toNBT() { CompoundTag tag = new CompoundTag(); tag.putUUID("UUID", playerUUID); tag.putString("Name", playerName); tag.putLong("Balance", balance); return tag; }
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = Math.max(0, balance); }
    public void add(long amount) { if (amount > 0) this.balance += amount; }
    public void subtract(long amount) { if (amount > 0) this.balance -= amount; }
}