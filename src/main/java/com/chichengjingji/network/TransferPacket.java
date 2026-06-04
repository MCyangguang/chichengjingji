package com.chichengjingji.network;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record TransferPacket(UUID target, long amount) implements CustomPacketPayload {
    public static final Type<TransferPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "transfer")
    );

    public static final StreamCodec<FriendlyByteBuf, TransferPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, TransferPacket packet) {
            buf.writeUUID(packet.target());
            buf.writeLong(packet.amount());
        }
        @Override
        public TransferPacket decode(FriendlyByteBuf buf) {
            return new TransferPacket(buf.readUUID(), buf.readLong());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;
            UUID from = sender.getUUID();
            UUID to = target;
            long amount = this.amount;
            if (amount <= 0) {
                sender.sendSystemMessage(Component.literal("§c金额必须大于0"));
                return;
            }
            if (from.equals(to)) {
                sender.sendSystemMessage(Component.literal("§c不能转账给自己"));
                return;
            }
            EconomyManager economy = EconomyManager.get(sender.getServer());
            // 获取接收方名称
            String toName = sender.getServer().getProfileCache().get(to).map(p -> p.getName()).orElse("未知玩家");
            boolean success = economy.transfer(from, sender.getName().getString(), to, toName, amount);
            if (success) {
                sender.sendSystemMessage(Component.literal("§a转账成功！"));
                // 同步双方余额
                long newFromBal = economy.getBalance(from);
                long newToBal = economy.getBalance(to);
                ModMessages.sendToPlayer(new BalanceSyncPacket(from, newFromBal), sender);
                ServerPlayer targetPlayer = sender.getServer().getPlayerList().getPlayer(to);
                if (targetPlayer != null) {
                    ModMessages.sendToPlayer(new BalanceSyncPacket(to, newToBal), targetPlayer);
                    targetPlayer.sendSystemMessage(Component.literal("§a收到来自 " + sender.getName().getString() + " 的转账 " + amount + " 赤城币"));
                }
            } else {
                sender.sendSystemMessage(Component.literal("§c转账失败，余额不足"));
            }
        });
    }
}