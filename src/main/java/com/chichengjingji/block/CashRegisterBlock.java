package com.chichengjingji.block;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.blockentity.CashRegisterBE;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CashRegisterBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 11, 16);

    public CashRegisterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CashRegisterBE(pos, state);
    }

    // 放置时设置所有者（服务端）
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CashRegisterBE cr) {
                cr.setOwner(player.getUUID(), player.getName().getString());
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
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

    // 右键交互：客户端根据所有者身份决定行为
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof CashRegisterBE be) {
                // 客户端根据所有者信息决定界面
                if (be.isOwner(player.getUUID())) {
                    // 所有者：显示提示，不打开转账界面
                    player.displayClientMessage(Component.literal("§e这是你的收银机，其他玩家可向你转账"), true);
                } else {
                    // 非所有者：打开转账界面
                    String ownerName = be.getOwnerName();
                    if (ownerName == null) ownerName = "未绑定";
                    Chichengjingji.PROXY.openCashRegisterScreen(level, pos, player, ownerName);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 服务端：兼容旧数据（如果所有者缺失则设置，一般不会触发）
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CashRegisterBE be) {
            if (be.getOwnerUUID() == null) {
                be.setOwner(player.getUUID(), player.getName().getString());
                level.sendBlockUpdated(pos, state, state, 3);
                player.sendSystemMessage(Component.literal("§a收银机已绑定到你"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    // 自发光
    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 10;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    // 碰撞箱
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // 确保破坏时掉落方块自身
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (drops.isEmpty()) {
            drops.add(new ItemStack(this.asItem()));
        }
        return drops;
    }
}