package com.chichengjingji.init;

import com.chichengjingji.Chichengjingji;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Chichengjingji.MODID);

    public static final Supplier<CreativeModeTab> CHICHENG_TAB = CREATIVE_TABS.register("chicheng_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chichengjingji"))
                    .icon(() -> new ItemStack(ModBlocks.VENDING_MACHINE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.VENDING_MACHINE.get());   // 售货机
                        output.accept(ModBlocks.CASH_REGISTER.get());    // 收银机
                        // 后续如有其他物品可继续添加
                    })
                    .build());
}