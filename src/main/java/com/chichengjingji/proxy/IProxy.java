package com.chichengjingji.proxy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IProxy {
    void openVendingMachineScreen(Level level, BlockPos pos, Player player);
    void openSetupScreen(Level level, BlockPos pos, Player player, ItemStack item);
    void openCashRegisterScreen(Level level, BlockPos pos, Player player, String ownerName);
}