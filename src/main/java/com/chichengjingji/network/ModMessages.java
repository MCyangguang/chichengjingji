package com.chichengjingji.network;

import com.chichengjingji.Chichengjingji;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModMessages {
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Chichengjingji.MODID).versioned("1");
        registrar.playToClient(BalanceSyncPacket.TYPE, BalanceSyncPacket.STREAM_CODEC, BalanceSyncPacket::handle);
        registrar.playToServer(TransferPacket.TYPE, TransferPacket.STREAM_CODEC, TransferPacket::handle);
        registrar.playToServer(SetupItemPacket.TYPE, SetupItemPacket.STREAM_CODEC, SetupItemPacket::handle);
        registrar.playToServer(PurchasePacket.TYPE, PurchasePacket.STREAM_CODEC, PurchasePacket::handle);
        registrar.playToServer(WithdrawPacket.TYPE, WithdrawPacket.STREAM_CODEC, WithdrawPacket::handle);
        registrar.playToClient(SyncMachinePacket.TYPE, SyncMachinePacket.STREAM_CODEC, SyncMachinePacket::handle);
        registrar.playToServer(RequestSyncPacket.TYPE, RequestSyncPacket.STREAM_CODEC, RequestSyncPacket::handle);
        registrar.playToServer(TransferToOwnerPacket.TYPE, TransferToOwnerPacket.STREAM_CODEC, TransferToOwnerPacket::handle);
    }

    // 发送给特定玩家
    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    // 发送给服务器（客户端调用）
    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    // 发送给所有玩家（可选）
    public static void sendToAllClients(CustomPacketPayload packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }
}