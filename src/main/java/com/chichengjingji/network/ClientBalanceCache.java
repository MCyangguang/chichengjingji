package com.chichengjingji.network;

import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientBalanceCache {
    private static final Map<UUID, Long> cache = new HashMap<>();

    public static void setBalance(UUID uuid, long balance) {
        cache.put(uuid, balance);
    }

    public static long getBalance(UUID uuid) {
        return cache.getOrDefault(uuid, 0L);
    }

    public static long getOwnBalance() {
        if (Minecraft.getInstance().player == null) return 0;
        return getBalance(Minecraft.getInstance().player.getUUID());
    }
}