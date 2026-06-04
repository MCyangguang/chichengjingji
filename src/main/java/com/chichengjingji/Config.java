package com.chichengjingji;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.LongValue STARTING_BALANCE = BUILDER
            .comment("新玩家初始余额")
            .defineInRange("starting_balance", 100L, 0, Long.MAX_VALUE);
    public static final ModConfigSpec.BooleanValue SHOW_BALANCE_HUD = BUILDER
            .comment("是否显示余额 HUD")
            .define("showBalanceHUD", true);
    public static final ModConfigSpec SPEC = BUILDER.build();
}