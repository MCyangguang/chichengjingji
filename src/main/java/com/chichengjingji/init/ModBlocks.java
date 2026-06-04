package com.chichengjingji.init;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.block.CashRegisterBlock;
import com.chichengjingji.block.VendingMachineBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Chichengjingji.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Chichengjingji.MODID);

    public static final DeferredBlock<VendingMachineBlock> VENDING_MACHINE = BLOCKS.register("vending_machine",
            () -> new VendingMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).noOcclusion()));
    public static final DeferredItem<BlockItem> VENDING_MACHINE_ITEM = ITEMS.registerSimpleBlockItem("vending_machine", VENDING_MACHINE);

    public static final DeferredBlock<CashRegisterBlock> CASH_REGISTER = BLOCKS.register("cash_register",
            () -> new CashRegisterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f)));
    public static final DeferredItem<BlockItem> CASH_REGISTER_ITEM = ITEMS.registerSimpleBlockItem("cash_register", CASH_REGISTER);
}