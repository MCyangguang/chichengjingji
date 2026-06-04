package com.chichengjingji.block;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.blockentity.VendingMachineBE;
import com.chichengjingji.network.BalanceSyncPacket;
import com.chichengjingji.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import com.chichengjingji.network.SyncMachinePacket;
import com.chichengjingji.economy.EconomyManager;
import java.util.ArrayList;
import java.util.List;


public class VendingMachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 19, 16);

    public VendingMachineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VendingMachineBE(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            // 客户端：打开 GUI
            if (level.getBlockEntity(pos) instanceof VendingMachineBE be) {
                if (be.getOwnerUUID() == null) {
                    be.setOwner(player.getUUID(), player.getName().getString());
                    player.displayClientMessage(Component.literal("§a售货机已绑定到你"), true);
                }
                if (be.isOwner(player.getUUID()) && !player.getMainHandItem().isEmpty()) {
                    // 使用代理打开设置界面
                    Chichengjingji.PROXY.openSetupScreen(level, pos, player, player.getMainHandItem());
                } else {
                    // 使用代理打开购买界面
                    Chichengjingji.PROXY.openVendingMachineScreen(level, pos, player);
                }
            }
            return InteractionResult.SUCCESS;
        } else {
            // 服务端：自动领取离线收益（仅所有者）
            if (level.getBlockEntity(pos) instanceof VendingMachineBE be) {
                if (be.getOwnerUUID() == null) {
                    be.setOwner(player.getUUID(), player.getName().getString());
                    player.sendSystemMessage(Component.literal("§a售货机已绑定到你"));
                }
                if (be.isOwner(player.getUUID()) && be.getEarnings() > 0) {
                    long amount = be.withdrawEarnings();
                    if (amount > 0) {
                        EconomyManager economy = EconomyManager.get(level.getServer());
                        economy.addBalance(player.getUUID(), player.getName().getString(), amount);
                        // 同步余额到客户端
                        if (player instanceof ServerPlayer sp) {
                            ModMessages.sendToPlayer(new BalanceSyncPacket(player.getUUID(), economy.getBalance(player.getUUID())), sp);
                        }
                        player.sendSystemMessage(Component.literal("§a自动领取售货机收益 " + amount + " 赤城币"));
                        // 广播售货机数据更新（收益已清零）
                        List<SyncMachinePacket.ItemData> itemDataList = new ArrayList<>();
                        for (VendingMachineBE.ShopItem si : be.getItems()) {
                            CompoundTag tag = (CompoundTag) si.item.save(level.registryAccess(), new CompoundTag());
                            itemDataList.add(new SyncMachinePacket.ItemData(tag, si.price, si.stock));
                        }
                        SyncMachinePacket sync = new SyncMachinePacket(pos, itemDataList, be.getEarnings());
                        ModMessages.sendToAllClients(sync);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 10;  // 最大亮度
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;  // 不受阴影影响
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VendingMachineBE vendingMachine) {
                vendingMachine.setOwner(player.getUUID(), player.getName().getString());
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        // 确保至少掉落方块自身（如果默认没有）
        if (drops.isEmpty()) {
            drops.add(new ItemStack(this.asItem()));
        }
        // 获取破坏位置
        Vec3 origin = builder.getParameter(LootContextParams.ORIGIN);
        BlockPos pos = BlockPos.containing(origin);
        BlockEntity be = builder.getLevel().getBlockEntity(pos);
        if (be instanceof VendingMachineBE vbe) {
            for (VendingMachineBE.ShopItem si : vbe.getItems()) {
                if (si.item.isEmpty() || si.stock <= 0) continue;
                ItemStack drop = si.item.copy();
                drop.setCount(si.stock);
                drops.add(drop);
            }
            // 清空商品列表，避免数据残留
            vbe.getItems().clear();
        }
        return drops;
    }

}