package com.chichengjingji.network;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.blockentity.VendingMachineBE;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record RequestSyncPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RequestSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "request_sync")
    );
    public static final StreamCodec<FriendlyByteBuf, RequestSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, RequestSyncPacket packet) {
            buf.writeBlockPos(packet.pos());
        }
        @Override
        public RequestSyncPacket decode(FriendlyByteBuf buf) {
            return new RequestSyncPacket(buf.readBlockPos());
        }
    };
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var level = player.serverLevel();
            if (!(level.getBlockEntity(pos) instanceof VendingMachineBE be)) return;
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
        });
    }
}