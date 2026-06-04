package com.chichengjingji.init;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.blockentity.CashRegisterBE;
import com.chichengjingji.blockentity.VendingMachineBE;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, Chichengjingji.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VendingMachineBE>> VENDING_MACHINE =
            BLOCK_ENTITIES.register("vending_machine", () -> BlockEntityType.Builder.of(VendingMachineBE::new, ModBlocks.VENDING_MACHINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CashRegisterBE>> CASH_REGISTER =
            BLOCK_ENTITIES.register("cash_register", () -> BlockEntityType.Builder.of(CashRegisterBE::new, ModBlocks.CASH_REGISTER.get()).build(null));
}