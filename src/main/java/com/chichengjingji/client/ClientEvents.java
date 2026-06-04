package com.chichengjingji.client;

import com.chichengjingji.init.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientEvents {
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VENDING_MACHINE.get(), RenderType.cutout());
        });
    }
}