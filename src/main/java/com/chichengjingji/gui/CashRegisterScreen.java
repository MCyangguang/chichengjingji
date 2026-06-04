package com.chichengjingji.gui;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.network.ModMessages;
import com.chichengjingji.network.TransferToOwnerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CashRegisterScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "textures/gui/cash_register.png");
    private final BlockPos pos;
    private final String ownerName;
    private EditBox amountBox;
    private int imageWidth = 176, imageHeight = 100;
    private int leftPos, topPos;

    public CashRegisterScreen(BlockPos pos, String ownerName) {
        super(Component.literal("向 " + ownerName + " 转账"));
        this.pos = pos;
        this.ownerName = ownerName;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (width - imageWidth) / 2;
        this.topPos = (height - imageHeight) / 2;
        amountBox = new EditBox(font, leftPos + 50, topPos + 30, 80, 18, Component.literal("金额"));
        amountBox.setValue("1");
        addRenderableWidget(amountBox);
        addRenderableWidget(Button.builder(Component.literal("转账"), b -> transfer())
                .bounds(leftPos + 30, topPos + 60, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(leftPos + 95, topPos + 60, 50, 20).build());
    }

    private void transfer() {
        long amount;
        try {
            amount = Long.parseLong(amountBox.getValue().trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§c请输入有效金额"), false);
            return;
        }
        ModMessages.sendToServer(new TransferToOwnerPacket(pos, amount));
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        guiGraphics.drawString(font, "向 " + ownerName + " 转账", leftPos + 8, topPos + 10, 0x404040, false);
        guiGraphics.drawString(font, "金额:", leftPos + 15, topPos + 34, 0x404040, false);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (amountBox.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}