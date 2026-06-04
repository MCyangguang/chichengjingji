package com.chichengjingji.network;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.blockentity.VendingMachineBE;
import com.chichengjingji.economy.EconomyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record WithdrawPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<WithdrawPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "withdraw"));
    public static final StreamCodec<FriendlyByteBuf, WithdrawPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, WithdrawPacket packet) {
            buf.writeBlockPos(packet.pos());
        }
        @Override
        public WithdrawPacket decode(FriendlyByteBuf buf) {
            return new WithdrawPacket(buf.readBlockPos());
        }
    };
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var level = player.serverLevel();
            if (!(level.getBlockEntity(pos) instanceof VendingMachineBE be)) return;
            if (!be.isOwner(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§c你不是所有者"));
                return;
            }
            long amount = be.withdrawEarnings();
            if (amount > 0) {
                EconomyManager economy = EconomyManager.get(player.getServer());
                economy.addBalance(player.getUUID(), player.getName().getString(), amount);
                // 关键：立即同步余额到客户端
                ModMessages.sendToPlayer(new BalanceSyncPacket(player.getUUID(), economy.getBalance(player.getUUID())), player);
                player.sendSystemMessage(Component.literal("§a取出 " + amount + " 赤城币"));
                // 广播同步
                List<SyncMachinePacket.ItemData> itemDataList = new ArrayList<>();
                for (VendingMachineBE.ShopItem si : be.getItems()) {
                    if (si.item.isEmpty()) {
                        Chichengjingji.LOGGER.warn("Skipping empty item during broadcast at {}", pos);
                        continue;
                    }
                    CompoundTag tag = (CompoundTag) si.item.save(level.registryAccess(), new CompoundTag());
                    itemDataList.add(new SyncMachinePacket.ItemData(tag, si.price, si.stock));
                }
                SyncMachinePacket sync = new SyncMachinePacket(pos, itemDataList, be.getEarnings());
                ModMessages.sendToAllClients(sync);
            } else {
                player.sendSystemMessage(Component.literal("§c暂无收益"));
            }
        });
    }
}