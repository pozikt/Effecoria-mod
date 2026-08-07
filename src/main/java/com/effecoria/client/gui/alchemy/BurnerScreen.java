package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.BurnerMenu;
import com.effecoria.core.alchemy.HeatLevel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Burner GUI — slots at fuel(56,35) / catalyst(80,17); player inv vanilla y=84/142. */
public final class BurnerScreen extends AbstractContainerScreen<BurnerMenu> {
    public BurnerScreen(BurnerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;
        addRenderableWidget(tempButton(x + 103, y + 14, BurnerMenu.BUTTON_TEMP_LOW, "gui.effecoria.burner.temp.low"));
        addRenderableWidget(tempButton(x + 103, y + 32, BurnerMenu.BUTTON_TEMP_MED, "gui.effecoria.burner.temp.med"));
        addRenderableWidget(tempButton(x + 103, y + 50, BurnerMenu.BUTTON_TEMP_HIGH, "gui.effecoria.burner.temp.high"));
    }

    private Button tempButton(int x, int y, int id, String key) {
        return Button.builder(Component.translatable(key), b -> sendTemp(id))
                .bounds(x, y, 54, 16)
                .build();
    }

    private void sendTemp(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.BURNER, leftPos, topPos);
        HeatLevel temp = menu.selectedTemp();
        int color = switch (temp) {
            case NONE -> 0xFF555555;
            case LOW -> 0xFF55AAFF;
            case HIGH -> 0xFFFFAA33;
            case MEDIUM -> 0xFF88DDFF;
        };
        int by = switch (temp) {
            case LOW -> 14;
            case HIGH -> 50;
            default -> 32;
        };
        graphics.fill(leftPos + 102, topPos + by, leftPos + 104, topPos + by + 16, color);
        float fuel = Math.min(1f, menu.fuelTicks() / 800f);
        if (fuel > 0 && menu.lit()) {
            int h = Math.max(1, Math.round(14 * fuel));
            graphics.fill(leftPos + 57, topPos + 68 - h, leftPos + 70, topPos + 68, color);
        }
        if (menu.overheatCooldown() > 0) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.burner.overheated"),
                    leftPos + 8,
                    topPos + 18,
                    0xAA3333,
                    false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(56, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font, Component.translatable("gui.effecoria.burner.fuel", menu.fuelTicks()), mouseX, mouseY);
        } else if (isHovering(80, 17, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.burner.catalyst"), mouseX, mouseY);
        }
    }
}
