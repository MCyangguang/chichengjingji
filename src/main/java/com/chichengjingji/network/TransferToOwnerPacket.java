package com.chichengjingji.network;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.blockentity.CashRegisterBE;
import com.chichengjingji.economy.EconomyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record TransferToOwnerPacket(BlockPos pos, long amount) implements CustomPacketPayload {
    public static final Type<TransferToOwnerPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "transfer_to_owner")
    );
    public static final StreamCodec<FriendlyByteBuf, TransferToOwnerPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, TransferToOwnerPacket packet) {
            buf.writeBlockPos(packet.pos());
            buf.writeLong(packet.amount());
        }
        @Override
        public TransferToOwnerPacket decode(FriendlyByteBuf buf) {
            return new TransferToOwnerPacket(buf.readBlockPos(), buf.readLong());
        }
    };
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;
            var level = sender.serverLevel();
            if (!(level.getBlockEntity(pos) instanceof CashRegisterBE be)) return;
            UUID ownerId = be.getOwnerUUID();
            if (ownerId == null) {
                sender.sendSystemMessage(Component.literal("§c收银机未绑定所有者"));
                return;
            }
            if (amount <= 0) {
                sender.sendSystemMessage(Component.literal("§c金额必须大于0"));
                return;
            }
            EconomyManager economy = EconomyManager.get(sender.getServer());
            long senderBalance = economy.getBalance(sender.getUUID());
            if (senderBalance < amount) {
                sender.sendSystemMessage(Component.literal("§c余额不足，需要 " + amount + " 赤城币，当前 " + senderBalance));
                return;
            }
            // 扣除付款人余额
            if (!economy.removeBalance(sender.getUUID(), sender.getName().getString(), amount)) {
                sender.sendSystemMessage(Component.literal("§c扣款失败"));
                return;
            }
            // 加给所有者
            String ownerName = be.getOwnerName() != null ? be.getOwnerName() : "未知";
            economy.addBalance(ownerId, ownerName, amount);
            // 同步双方余额
            ModMessages.sendToPlayer(new BalanceSyncPacket(sender.getUUID(), economy.getBalance(sender.getUUID())), sender);
            ServerPlayer owner = sender.getServer().getPlayerList().getPlayer(ownerId);
            if (owner != null) {
                ModMessages.sendToPlayer(new BalanceSyncPacket(ownerId, economy.getBalance(ownerId)), owner);
                owner.sendSystemMessage(Component.literal("§a玩家 " + sender.getName().getString() + " 向你转账 " + amount + " 赤城币"));
            }
            sender.sendSystemMessage(Component.literal("§a成功转账 " + amount + " 赤城币给 " + ownerName));
        });
    }
}