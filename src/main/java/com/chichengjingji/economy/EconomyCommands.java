package com.chichengjingji.economy;

import com.chichengjingji.network.BalanceSyncPacket;
import com.chichengjingji.network.ModMessages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class EconomyCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chicheng")
                .then(Commands.literal("balance").executes(ctx -> {
                    if (ctx.getSource().getEntity() instanceof ServerPlayer p) {
                        long bal = EconomyManager.get(p.getServer()).getBalance(p.getUUID());
                        ctx.getSource().sendSuccess(() -> Component.literal("你的余额: " + bal + " 赤城币"), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            EconomyManager eco = EconomyManager.get(p.getServer());
                                            eco.addBalance(p.getUUID(), p.getName().getString(), amount);
                                            ModMessages.sendToPlayer(new BalanceSyncPacket(p.getUUID(), eco.getBalance(p.getUUID())), p);
                                            ctx.getSource().sendSuccess(() -> Component.literal("给予 " + p.getName().getString() + " " + amount + " 赤城币"), true);
                                            p.sendSystemMessage(Component.literal("§a收到 " + amount + " 赤城币"));
                                            return 1;
                                        }))))
                .then(Commands.literal("take").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            EconomyManager eco = EconomyManager.get(p.getServer());
                                            if (eco.removeBalance(p.getUUID(), p.getName().getString(), amount)) {
                                                ModMessages.sendToPlayer(new BalanceSyncPacket(p.getUUID(), eco.getBalance(p.getUUID())), p);
                                                ctx.getSource().sendSuccess(() -> Component.literal("扣除 " + p.getName().getString() + " " + amount + " 赤城币"), true);
                                                p.sendSystemMessage(Component.literal("§c被扣除 " + amount + " 赤城币"));
                                            } else {
                                                ctx.getSource().sendFailure(Component.literal("余额不足"));
                                            }
                                            return 1;
                                        }))))
                .then(Commands.literal("set").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            EconomyManager eco = EconomyManager.get(p.getServer());
                                            eco.setBalance(p.getUUID(), p.getName().getString(), amount);
                                            ModMessages.sendToPlayer(new BalanceSyncPacket(p.getUUID(), eco.getBalance(p.getUUID())), p);
                                            ctx.getSource().sendSuccess(() -> Component.literal("设置 " + p.getName().getString() + " 余额为 " + amount + " 赤城币"), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer from)) {
                                                ctx.getSource().sendFailure(Component.literal("只有玩家可以执行"));
                                                return 0;
                                            }
                                            ServerPlayer to = EntityArgument.getPlayer(ctx, "target");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            if (from.getUUID().equals(to.getUUID())) {
                                                ctx.getSource().sendFailure(Component.literal("不能转账给自己"));
                                                return 0;
                                            }
                                            EconomyManager eco = EconomyManager.get(from.getServer());
                                            if (eco.transfer(from.getUUID(), from.getName().getString(),
                                                    to.getUUID(), to.getName().getString(), amount)) {
                                                long newFrom = eco.getBalance(from.getUUID());
                                                long newTo = eco.getBalance(to.getUUID());
                                                ModMessages.sendToPlayer(new BalanceSyncPacket(from.getUUID(), newFrom), from);
                                                ModMessages.sendToPlayer(new BalanceSyncPacket(to.getUUID(), newTo), to);
                                                from.sendSystemMessage(Component.literal("§a成功转账 " + amount + " 赤城币给 " + to.getName().getString()));
                                                to.sendSystemMessage(Component.literal("§a收到来自 " + from.getName().getString() + " 的转账 " + amount + " 赤城币"));
                                            } else {
                                                ctx.getSource().sendFailure(Component.literal("转账失败，余额不足"));
                                            }
                                            return 1;
                                        })))));
    }
}