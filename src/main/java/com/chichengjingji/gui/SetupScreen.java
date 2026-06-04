package com.chichengjingji.gui;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.network.ModMessages;
import com.chichengjingji.network.SetupItemPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class SetupScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "textures/gui/vending.png");
    private final ItemStack item;
    private final BlockPos pos;
    private EditBox priceBox, amountBox;
    private int imageWidth = 200, imageHeight = 150;
    private int leftPos, topPos;

    public SetupScreen(ItemStack item, BlockPos pos) {
        super(Component.literal("设置商品"));
        this.item = item;
        this.pos = pos;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (width - imageWidth) / 2;
        this.topPos = (height - imageHeight) / 2;

        // 调整输入框位置，确保在纹理内部（纹理宽200，左侧留10，右侧留10）
        priceBox = new EditBox(font, leftPos + 80, topPos + 35, 110, 18, Component.literal("单价"));
        amountBox = new EditBox(font, leftPos + 80, topPos + 60, 110, 18, Component.literal("数量"));
        priceBox.setValue("1");
        amountBox.setValue("1");
        addRenderableWidget(priceBox);
        addRenderableWidget(amountBox);

        addRenderableWidget(Button.builder(Component.literal("确认上架"), b -> setup())
                .bounds(leftPos + 40, topPos + 90, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(leftPos + 40, topPos + 120, 120, 20).build());
    }

    private void setup() {
        long price;
        int amount;
        try {
            price = Long.parseLong(priceBox.getValue().trim());
            amount = Integer.parseInt(amountBox.getValue().trim());
            if (price <= 0 || amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§c请输入有效数字"), false);
            return;
        }
        SetupItemPacket packet = new SetupItemPacket(pos, item, price, amount, Minecraft.getInstance().level.registryAccess());
        ModMessages.sendToServer(packet);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 不调用 renderBackground，避免半透明遮罩导致模糊
        // renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // 绘制背景纹理
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // 绘制物品图标（左上方，尺寸16x16）
        guiGraphics.renderItem(item, leftPos + 10, topPos + 10);
        guiGraphics.renderItemDecorations(font, item, leftPos + 10, topPos + 10);

        // 绘制物品名称（使用亮色，清晰可见）
        guiGraphics.drawString(font, "商品: " + item.getHoverName().getString(), leftPos + 30, topPos + 12, 0xFFFFFF, true);

        // 绘制标签（使用亮色，带阴影）
        guiGraphics.drawString(font, "单价:", leftPos + 15, topPos + 38, 0xFFFFFF, true);
        guiGraphics.drawString(font, "数量:", leftPos + 15, topPos + 63, 0xFFFFFF, true);

        // 注意：不要调用 super.render，因为我们已经手动渲染了所有内容
        // 但需要单独渲染输入框（它们已经通过 addRenderableWidget 注册，通常由 super.render 自动处理）
        // 由于我们跳过了 super.render，需要手动渲染子组件
        priceBox.render(guiGraphics, mouseX, mouseY, partialTick);
        amountBox.render(guiGraphics, mouseX, mouseY, partialTick);

        // 手动渲染按钮（因为跳过了 super.render）
        for (var widget : renderables) {
            if (widget instanceof Button button) {
                button.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (priceBox.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (amountBox.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (priceBox.charTyped(codePoint, modifiers)) return true;
        if (amountBox.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}