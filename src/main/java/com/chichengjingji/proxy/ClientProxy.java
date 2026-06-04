package com.chichengjingji.proxy;

import com.chichengjingji.gui.CashRegisterScreen;
import com.chichengjingji.gui.SetupScreen;
import com.chichengjingji.gui.VendingMachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientProxy implements IProxy {
    @Override
    public void openVendingMachineScreen(Level level, BlockPos pos, Player player) {
        Minecraft.getInstance().setScreen(new VendingMachineScreen(pos));
    }

    @Override
    public void openSetupScreen(Level level, BlockPos pos, Player player, ItemStack item) {
        Minecraft.getInstance().setScreen(new SetupScreen(item.copy(), pos));
    }

    @Override
    public void openCashRegisterScreen(Level level, BlockPos pos, Player player, String ownerName) {
        Minecraft.getInstance().setScreen(new CashRegisterScreen(pos, ownerName));
    }
}