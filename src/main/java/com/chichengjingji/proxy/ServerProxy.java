package com.chichengjingji.proxy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ServerProxy implements IProxy {
    @Override
    public void openVendingMachineScreen(Level level, BlockPos pos, Player player) {
        // 服务端不做任何事
    }

    @Override
    public void openSetupScreen(Level level, BlockPos pos, Player player, ItemStack item) {
        // 服务端不做任何事
    }

    @Override
    public void openCashRegisterScreen(Level level, BlockPos pos, Player player, String ownerName) {
        // 服务端不做任何事
    }
}