package com.chichengjingji.network;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.blockentity.VendingMachineBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record SetupItemPacket(BlockPos pos, CompoundTag itemTag, long price, int amount) implements CustomPacketPayload {
    public static final Type<SetupItemPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "setup_item"));

    public static final StreamCodec<FriendlyByteBuf, SetupItemPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, SetupItemPacket packet) {
            buf.writeBlockPos(packet.pos());
            buf.writeNbt(packet.itemTag());
            buf.writeLong(packet.price());
            buf.writeInt(packet.amount());
        }

        @Override
        public SetupItemPacket decode(FriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            CompoundTag tag = buf.readNbt();
            long price = buf.readLong();
            int amount = buf.readInt();
            return new SetupItemPacket(pos, tag, price, amount);
        }
    };

    // 便捷构造器（从 ItemStack 创建）
    public SetupItemPacket(BlockPos pos, ItemStack stack, long price, int amount, HolderLookup.Provider registries) {
        this(pos, saveItemStack(stack, registries), price, amount);
    }

    private static CompoundTag saveItemStack(ItemStack stack, HolderLookup.Provider registries) {
        return (CompoundTag) stack.save(registries, new CompoundTag());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var level = player.serverLevel();
            if (!(level.getBlockEntity(pos) instanceof VendingMachineBE be)) return;
            if (!be.isOwner(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§c你不是这台售货机的所有者"));
                return;
            }
            // 还原 ItemStack
            ItemStack item = ItemStack.parse(level.registryAccess(), itemTag).orElse(ItemStack.EMPTY);
            if (item.isEmpty()) {
                player.sendSystemMessage(Component.literal("§c无效物品"));
                return;
            }

            // 检查背包中该物品的数量（匹配完整 NBT）
            int total = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameComponents(s, item)) total += s.getCount();
            }
            if (total < amount) {
                player.sendSystemMessage(Component.literal("§c背包中 " + item.getHoverName().getString() + " 不足 " + amount + " 个"));
                return;
            }
            // 扣除物品
            int remain = amount;
            for (int i = 0; i < player.getInventory().getContainerSize() && remain > 0; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameComponents(s, item)) {
                    int take = Math.min(remain, s.getCount());
                    s.shrink(take);
                    remain -= take;
                }
            }
            // 上架
            be.addItem(item.copy(), price, amount);

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
            player.sendSystemMessage(Component.literal("§a已上架 " + amount + " 个 " + item.getHoverName().getString()));
        });
    }
}