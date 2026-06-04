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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PurchasePacket(BlockPos pos, int index, int amount) implements CustomPacketPayload {
    public static final Type<PurchasePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "purchase"));
    public static final StreamCodec<FriendlyByteBuf, PurchasePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, PurchasePacket packet) {
            buf.writeBlockPos(packet.pos());
            buf.writeInt(packet.index());
            buf.writeInt(packet.amount());
        }
        @Override
        public PurchasePacket decode(FriendlyByteBuf buf) {
            return new PurchasePacket(buf.readBlockPos(), buf.readInt(), buf.readInt());
        }
    };
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer buyer)) return;
            var level = buyer.serverLevel();
            if (!(level.getBlockEntity(pos) instanceof VendingMachineBE be)) return;
            var items = be.getItems();
            if (index < 0 || index >= items.size()) return;
            var item = items.get(index);
            if (amount > item.stock) {
                buyer.sendSystemMessage(Component.literal("§c库存不足"));
                return;
            }
            long cost = item.price * amount;
            EconomyManager economy = EconomyManager.get(buyer.getServer());
            long buyerBalance = economy.getBalance(buyer.getUUID());
            if (buyerBalance < cost) {
                buyer.sendSystemMessage(Component.literal("§c余额不足，需要 " + cost + " 赤城币，当前 " + buyerBalance));
                return;
            }

            // 精确计算背包剩余空间
            int maxStack = item.item.getMaxStackSize();
            int need = amount;
            for (int i = 0; i < buyer.getInventory().getContainerSize(); i++) {
                ItemStack invStack = buyer.getInventory().getItem(i);
                if (invStack.isEmpty()) {
                    need -= maxStack;
                } else if (ItemStack.isSameItemSameComponents(invStack, item.item)) {
                    int space = maxStack - invStack.getCount();
                    if (space > 0) {
                        need -= space;
                    }
                }
                if (need <= 0) break;
            }
            if (need > 0) {
                buyer.sendSystemMessage(Component.literal("§c背包空间不足，还需要 " + need + " 个空位"));
                return;
            }

            // 1. 扣款
            if (!economy.removeBalance(buyer.getUUID(), buyer.getName().getString(), cost)) {
                buyer.sendSystemMessage(Component.literal("§c扣款失败，请联系管理员"));
                return;
            }

            // 2. 给物品
            ItemStack out = item.item.copy();
            out.setCount(amount);
            if (!buyer.getInventory().add(out)) {
                // 背包满，退还扣款
                economy.addBalance(buyer.getUUID(), buyer.getName().getString(), cost);
                buyer.sendSystemMessage(Component.literal("§c背包空间不足，购买失败"));
                return;
            }

            // 3. 减少库存
            be.reduceStock(index, amount);
            ModMessages.sendToPlayer(new BalanceSyncPacket(buyer.getUUID(), economy.getBalance(buyer.getUUID())), buyer);

            // 4. 处理收益
            UUID ownerId = be.getOwnerUUID();
            if (ownerId != null) {
                ServerPlayer owner = buyer.getServer().getPlayerList().getPlayer(ownerId);
                if (owner != null) {
                    economy.addBalance(ownerId, be.getOwnerName(), cost);
                    ModMessages.sendToPlayer(new BalanceSyncPacket(ownerId, economy.getBalance(ownerId)), owner);
                } else {
                    be.addEarnings(cost);
                }
            } else {
                be.addEarnings(cost);
            }

            // 5. 广播同步
            List<SyncMachinePacket.ItemData> itemDataList = new ArrayList<>();
            for (VendingMachineBE.ShopItem si : be.getItems()) {
                CompoundTag tag = (CompoundTag) si.item.save(level.registryAccess(), new CompoundTag());
                itemDataList.add(new SyncMachinePacket.ItemData(tag, si.price, si.stock));
            }
            SyncMachinePacket sync = new SyncMachinePacket(pos, itemDataList, be.getEarnings());
            ModMessages.sendToAllClients(sync);
            buyer.sendSystemMessage(Component.literal("§a购买成功，花费 " + cost + " 赤城币"));
        });
    }
}