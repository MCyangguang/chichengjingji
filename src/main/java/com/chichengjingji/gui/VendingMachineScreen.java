package com.chichengjingji.gui;

import com.chichengjingji.Chichengjingji;
import com.chichengjingji.blockentity.VendingMachineBE;
import com.chichengjingji.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class VendingMachineScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Chichengjingji.MODID, "textures/gui/vending.png");
    private final BlockPos pos;
    private List<VendingMachineBE.ShopItem> items;
    private int selectedIndex = -1;
    private EditBox amountBox;
    private long lastClick = 0;
    private int imageWidth = 200, imageHeight = 220;
    private int leftPos, topPos;
    private int refreshCooldown = 0;

    private static class ButtonArea {
        int index;
        int x, y, width, height;
        ItemStack item;
        String text;
        long price;
        int stock;
        ButtonArea(int index, int x, int y, int width, int height, ItemStack item, String text, long price, int stock) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.item = item;
            this.text = text;
            this.price = price;
            this.stock = stock;
        }
    }
    private List<ButtonArea> buttonAreas = new ArrayList<>();

    public VendingMachineScreen(BlockPos pos) {
        super(Component.literal("售货机"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (width - imageWidth) / 2;
        this.topPos = (height - imageHeight) / 2;
        refreshData();
        ModMessages.sendToServer(new RequestSyncPacket(pos));
    }

    public void refreshData() {
        if (Minecraft.getInstance().level == null) return;
        if (Minecraft.getInstance().level.getBlockEntity(pos) instanceof VendingMachineBE be) {
            this.items = be.getItems();
        } else {
            this.items = List.of();
        }
        if (this.items.isEmpty()) {
            this.selectedIndex = -1;
        } else if (this.selectedIndex >= this.items.size()) {
            this.selectedIndex = -1;
        }
        buttonAreas.clear();
        if (selectedIndex == -1) {
            buildList();
        } else {
            buildPurchase();
        }
    }

    private void buildList() {
        int y = topPos + 30;
        int btnWidth = imageWidth - 10;
        int btnHeight = 24;
        for (int i = 0; i < items.size(); i++) {
            VendingMachineBE.ShopItem item = items.get(i);
            String text = item.item.getHoverName().getString() + " 单价:" + item.price + " 库存:" + item.stock;
            buttonAreas.add(new ButtonArea(i, leftPos + 5, y, btnWidth, btnHeight, item.item, text, item.price, item.stock));
            y += 26;
        }
        if (items.isEmpty()) {
            buttonAreas.add(new ButtonArea(-2, leftPos + 5, topPos + 30, btnWidth, 20, ItemStack.EMPTY, "暂无商品", 0, 0));
        }
    }

    private void buildPurchase() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) {
            selectedIndex = -1;
            refreshData();
            return;
        }
        buttonAreas.add(new ButtonArea(-3, leftPos + 8, topPos + 5, 60, 20, ItemStack.EMPTY, "返回", 0, 0));
        amountBox = new EditBox(font, leftPos + 70, topPos + 110, 60, 18, Component.literal("数量"));
        amountBox.setValue("1");
        addRenderableWidget(amountBox);
        buttonAreas.add(new ButtonArea(-4, leftPos + 60, topPos + 140, 80, 20, ItemStack.EMPTY, "购买", 0, 0));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // 标题居中
        String title = (selectedIndex == -1) ? "商品列表" : "购买商品";
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, leftPos + (imageWidth - titleWidth) / 2, topPos + 8, 0xFFFFFF, true);

        for (ButtonArea area : buttonAreas) {
            if (area.index >= 0) {
                // 商品项背景
                guiGraphics.fill(area.x, area.y, area.x + area.width, area.y + area.height, 0x33AAAAAA);
                guiGraphics.fill(area.x, area.y + area.height - 1, area.x + area.width, area.y + area.height, 0xFFFFFFFF);
                guiGraphics.renderItem(area.item, area.x + 2, area.y + 2);
                guiGraphics.drawString(font, area.text, area.x + 20, area.y + 6, 0xFFFFFF, true);
            } else if (area.index == -3) {
                // 返回按钮
                boolean hovered = isHovered(area, mouseX, mouseY);
                int bg = hovered ? 0xAAEEAA44 : 0x88CC8822;
                guiGraphics.fill(area.x, area.y, area.x + area.width, area.y + area.height, bg);
                drawBorder(guiGraphics, area);
                int tw = font.width(area.text);
                guiGraphics.drawString(font, area.text, area.x + (area.width - tw)/2, area.y + (area.height - 8)/2, 0xFFFFAA, true);
            } else if (area.index == -4) {
                // 购买按钮
                boolean hovered = isHovered(area, mouseX, mouseY);
                int bg = hovered ? 0xAA88FF88 : 0x8844DD44;
                guiGraphics.fill(area.x, area.y, area.x + area.width, area.y + area.height, bg);
                drawBorder(guiGraphics, area);
                int tw = font.width(area.text);
                guiGraphics.drawString(font, area.text, area.x + (area.width - tw)/2, area.y + (area.height - 8)/2, 0xAAFFAA, true);
            } else if (area.index == -2) {
                guiGraphics.drawString(font, area.text, area.x + 8, area.y + 5, 0x808080, false);
            }
        }

        // 购买详情页（放大图标）
        if (selectedIndex != -1 && selectedIndex < items.size()) {
            var item = items.get(selectedIndex);
            int centerX = leftPos + imageWidth / 2;
            int centerY = topPos + imageHeight / 2;
            // 放大2倍
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX - 16, centerY - 32, 0);
            guiGraphics.pose().scale(2.0F, 2.0F, 1.0F);
            guiGraphics.renderItem(item.item, 0, 0);
//            guiGraphics.renderItemDecorations(font, item.item, 0, 0);
            guiGraphics.pose().popPose();

            String name = item.item.getHoverName().getString();
            int nw = font.width(name);
            guiGraphics.drawString(font, name, centerX - nw/2, centerY - 48, 0xFFFFFF, true);
            guiGraphics.drawString(font, "单价: " + item.price, centerX - 40, centerY + 20, 0x00AAAA, true);
            guiGraphics.drawString(font, "库存: " + item.stock, centerX + 10, centerY + 20, 0x00AAAA, true);
        }

        long balance = ClientBalanceCache.getOwnBalance();
        guiGraphics.drawString(font, "余额: " + balance, leftPos + 8, topPos + imageHeight - 15, 0x00AA00, true);
        if (amountBox != null) amountBox.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawBorder(GuiGraphics g, ButtonArea a) {
        g.fill(a.x, a.y, a.x + a.width, a.y + 1, 0xFFFFFFFF);
        g.fill(a.x, a.y + a.height - 1, a.x + a.width, a.y + a.height, 0xFFFFFFFF);
        g.fill(a.x, a.y, a.x + 1, a.y + a.height, 0xFFFFFFFF);
        g.fill(a.x + a.width - 1, a.y, a.x + a.width, a.y + a.height, 0xFFFFFFFF);
    }

    private boolean isHovered(ButtonArea area, double mx, double my) {
        return mx >= area.x && mx <= area.x + area.width && my >= area.y && my <= area.y + area.height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (ButtonArea area : buttonAreas) {
            if (isHovered(area, mouseX, mouseY)) {
                if (area.index >= 0) {
                    selectedIndex = area.index;
                    refreshData();
                } else if (area.index == -3) {
                    selectedIndex = -1;
                    refreshData();
                } else if (area.index == -4) {
                    purchase();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void purchase() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) return;
        int amount;
        try {
            amount = Integer.parseInt(amountBox.getValue().trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§c请输入有效数量"), false);
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastClick < 500) return;
        lastClick = now;
        ModMessages.sendToServer(new PurchasePacket(pos, selectedIndex, amount));
        selectedIndex = -1;
        refreshData();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (amountBox != null && amountBox.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (amountBox != null && amountBox.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // 列表页每 1 秒刷新一次（防止不同步）
        if (selectedIndex == -1) {
            if (refreshCooldown <= 0) {
                refreshData();
                refreshCooldown = 20;
            } else {
                refreshCooldown--;
            }
        } else {
            refreshCooldown = 0;
        }
    }
}