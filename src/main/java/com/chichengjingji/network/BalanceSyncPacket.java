package com.chichengjingji.network;

import com.chichengjingji.Chichengjingji;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record BalanceSyncPacket(UUID playerUUID, long balance) implements CustomPacketPayload {
    public static final Type<BalanceSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "balance_sync")
    );

    public static final StreamCodec<FriendlyByteBuf, BalanceSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, BalanceSyncPacket packet) {
            buf.writeUUID(packet.playerUUID());
            buf.writeLong(packet.balance());
        }

        @Override
        public BalanceSyncPacket decode(FriendlyByteBuf buf) {
            return new BalanceSyncPacket(buf.readUUID(), buf.readLong());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // 客户端缓存余额
            ClientBalanceCache.setBalance(playerUUID, balance);
        });
    }
}