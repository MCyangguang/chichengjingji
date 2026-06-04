package com.chichengjingji.hud;

import com.chichengjingji.Config;
import com.chichengjingji.Chichengjingji;
import com.chichengjingji.network.ClientBalanceCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public class BalanceHUD {
    public static final LayeredDraw.Layer LAYER = (guiGraphics, deltaTracker) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!Config.SHOW_BALANCE_HUD.get()) return;
        long balance = ClientBalanceCache.getOwnBalance();
        String text = "赤城币: " + balance;
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        int textWidth = mc.font.width(text);
        int x = screenWidth - textWidth - 10;
        int y = screenHeight - 40;
        guiGraphics.drawString(mc.font, Component.literal(text), x, y, 0xFFFFFF, true);
    };

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "balance_hud"),
                LAYER);
    }
}