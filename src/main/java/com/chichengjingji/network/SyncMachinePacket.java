package com.chichengjingji.network;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.blockentity.VendingMachineBE;
import com.chichengjingji.gui.VendingMachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record SyncMachinePacket(BlockPos pos, List<ItemData> items, long earnings) implements CustomPacketPayload {
    public static final Type<SyncMachinePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "sync_machine"));

    public static final StreamCodec<FriendlyByteBuf, SyncMachinePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, SyncMachinePacket packet) {
            buf.writeBlockPos(packet.pos());
            buf.writeVarInt(packet.items().size());
            for (ItemData data : packet.items()) {
                buf.writeNbt(data.tag());          // 写入 NBT
                buf.writeLong(data.price());
                buf.writeInt(data.stock());
            }
            buf.writeLong(packet.earnings());
        }

        @Override
        public SyncMachinePacket decode(FriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int size = buf.readVarInt();
            List<ItemData> items = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                CompoundTag tag = buf.readNbt();
                long price = buf.readLong();
                int stock = buf.readInt();
                items.add(new ItemData(tag, price, stock));
            }
            long earnings = buf.readLong();
            return new SyncMachinePacket(pos, items, earnings);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;
            if (Minecraft.getInstance().level.getBlockEntity(pos) instanceof VendingMachineBE be) {
                // 直接传入原始的 items 列表（包含 CompoundTag）
                be.updateFromSync(items, earnings);
                if (Minecraft.getInstance().screen instanceof VendingMachineScreen screen) {
                    screen.refreshData();
                }
            }
        });
    }

    public record ItemData(CompoundTag tag, long price, int stock) {
        // 只需要这一个构造器
        public ItemStack toStack(HolderLookup.Provider registries) {
            if (tag.isEmpty()) {
                System.out.println("SyncMachinePacket: Received empty tag for price " + price + " stock " + stock);
            } else {
                System.out.println("SyncMachinePacket: tag = " + tag);
            }
            return ItemStack.parse(registries, tag).orElse(ItemStack.EMPTY);
        }
    }
}