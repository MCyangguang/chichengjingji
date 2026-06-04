package com.chichengjingji;

import com.chichengjingji.block.CashRegisterBlock;
import com.chichengjingji.blockentity.CashRegisterBE;
import com.chichengjingji.blockentity.VendingMachineBE;
import com.chichengjingji.client.ClientEvents;
import com.chichengjingji.economy.EconomyCommands;
import com.chichengjingji.economy.EconomyManager;
import com.chichengjingji.hud.BalanceHUD;
import com.chichengjingji.init.ModBlockEntities;
import com.chichengjingji.init.ModBlocks;
import com.chichengjingji.init.ModCreativeTabs;
import com.chichengjingji.network.BalanceSyncPacket;
import com.chichengjingji.network.ModMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.chichengjingji.block.VendingMachineBlock;


@Mod(Chichengjingji.MODID)
public class Chichengjingji {
    public static final String MODID = "chichengjingji";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public Chichengjingji(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(this::registerNetwork);
        modEventBus.addListener(this::registerGuiLayers);
        modEventBus.addListener(ClientEvents::onClientSetup);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        Block block = state.getBlock();
        if (block instanceof VendingMachineBlock) {
            // ... 售货机保护
        } else if (block instanceof CashRegisterBlock) {
            BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
            if (be instanceof CashRegisterBE cr) {
                UUID ownerId = cr.getOwnerUUID();
                if (ownerId != null && !cr.isOwner(event.getPlayer().getUUID())) {
                    event.setCanceled(true);
                    event.getPlayer().sendSystemMessage(Component.literal("§c你不能破坏别人的收银机"));
                }
            }
        }
        // 收银机保护
        else if (block instanceof CashRegisterBlock) {
            BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
            if (be instanceof CashRegisterBE cr) {
                UUID ownerId = cr.getOwnerUUID();
                if (ownerId != null && !cr.isOwner(event.getPlayer().getUUID())) {
                    event.setCanceled(true);
                    event.getPlayer().sendSystemMessage(Component.literal("§c你不能破坏别人的收银机"));
                }
            }
        }
    }

    private void registerNetwork(RegisterPayloadHandlersEvent event) {
        ModMessages.register(event);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        BalanceHUD.register(event);
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EconomyManager economy = EconomyManager.get(player.getServer());
        economy.ensurePlayer(player.getUUID(), player.getName().getString());
        long bal = economy.getBalance(player.getUUID());
        ModMessages.sendToPlayer(new BalanceSyncPacket(player.getUUID(), bal), player);

        // 补发离线收益（使用全局注册表，无需遍历区块）
        long total = 0;
        List<VendingMachineBE> toClear = new ArrayList<>();
        for (VendingMachineBE vbe : VendingMachineBE.getMachinesForOwner(player.getUUID())) {
            long earnings = vbe.getEarnings();
            if (earnings > 0) {
                total += earnings;
                toClear.add(vbe);
            }
        }
        if (total > 0) {
            economy.addBalance(player.getUUID(), player.getName().getString(), total);
            ModMessages.sendToPlayer(new BalanceSyncPacket(player.getUUID(), economy.getBalance(player.getUUID())), player);
            player.sendSystemMessage(Component.literal("§a你离线期间售货机获得 " + total + " 赤城币"));
            for (VendingMachineBE vbe : toClear) {
                vbe.setEarnings(0);
            }
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        EconomyCommands.register(event.getDispatcher());
    }
}