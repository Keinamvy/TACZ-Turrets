package com.entropy.tacz_turrets.client.screen;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.config.TACZTurretsConfig;
import com.entropy.tacz_turrets.menu.TurretLayout;
import com.entropy.tacz_turrets.menu.TurretMenu;
import com.entropy.tacz_turrets.network.TACZTurretsNetwork;
import com.entropy.tacz_turrets.network.ToggleAllyPacket;
import com.entropy.tacz_turrets.turret.HealthBarStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

public class TurretScreen extends AbstractContainerScreen<TurretMenu> {
    private static final Identifier BACKGROUND = TACZTurrets.id("background");
    private static final Identifier SLOT_TEXTURE = TACZTurrets.id("textures/gui/sprites/slot.png");

    private static final int BAR_BACKGROUND = 0xFF101010;
    private static final int BAR_BORDER = 0xFF373737;
    private static final int ENERGY_COLOR = 0xE08010;
    private static final int ALLY_ROW_HEIGHT = 11;
    private static final int ALLY_LIST_WIDTH = 96;
    private static final int ALLY_MAX_ROWS = 8;
    private static final int ALLY_BUTTON_HEIGHT = 16;

    private final TurretLayout layout;
    private final List<Runnable> buttonRefreshers = new ArrayList<>();
    private Button allyButton;
    private boolean allyListPinned = false;

    public TurretScreen(TurretMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, menu.getLayout().width, menu.getLayout().height);
        layout = menu.getLayout();
        inventoryLabelY = layout.inventoryLabelY;
        titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        buttonRefreshers.clear();
        int buttonWidth = (layout.barWidth - 4) / 2;
        addCycleButton("enable_type", menu::getEnableType, TurretMenu.BUTTON_ENABLE_TYPE, TurretLayout.MARGIN, layout.buttonsY, buttonWidth);
        addCycleButton("mode", menu::getMode, TurretMenu.BUTTON_MODE, TurretLayout.MARGIN + buttonWidth + 4, layout.buttonsY, buttonWidth);
        addCycleButton("player_targeting", menu::getPlayerTargeting, TurretMenu.BUTTON_PLAYER_TARGETING, TurretLayout.MARGIN, layout.secondButtonRowY, layout.barWidth);
        allyButton = addRenderableWidget(Button.builder(Component.translatable("gui.tacz_turrets.allies"), button -> allyListPinned = !allyListPinned)
                .bounds(leftPos + allyButtonX(), topPos + layout.gunSlotY + 1, allyButtonWidth(), ALLY_BUTTON_HEIGHT)
                .build());
        allyButton.active = menu.canModify() && !menu.getOwnerName().isEmpty();
    }

    private void addCycleButton(String group, Supplier<? extends Enum<?>> value, int id, int x, int y, int width) {
        Button button = addRenderableWidget(Button.builder(Component.empty(), pressed -> pressButton(id))
                .bounds(leftPos + x, topPos + y, width, TurretLayout.BUTTON_HEIGHT)
                .build());
        button.active = menu.canModify();
        buttonRefreshers.add(() -> {
            String key = "gui.tacz_turrets." + group + "." + value.get().name().toLowerCase(Locale.ROOT);
            button.setMessage(Component.translatable(key));
            button.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip")));
        });
    }

    private void pressButton(int id) {
        if (minecraft == null || minecraft.gameMode == null) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private int healthColor(float fraction) {
        if (TACZTurretsConfig.HEALTH_BAR_STYLE.get() == HealthBarStyle.COLOR) return TACZTurretsConfig.healthBarColor;
        float clamped = Mth.clamp(fraction, 0.0F, 1.0F);
        if (clamped >= 0.5F) {
            float upper = (clamped - 0.5F) * 2.0F;
            return rgb(Mth.lerp(upper, 255.0F, 70.0F), Mth.lerp(upper, 150.0F, 200.0F), Mth.lerp(upper, 30.0F, 60.0F));
        }
        float lower = clamped * 2.0F;
        return rgb(Mth.lerp(lower, 200.0F, 255.0F), Mth.lerp(lower, 30.0F, 150.0F), 30.0F);
    }

    private static int rgb(float red, float green, float blue) {
        return ((int) red << 16) | ((int) green << 8) | (int) blue;
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, BAR_BORDER);
        graphics.fill(x, y, x + width, y + height, BAR_BACKGROUND);
    }

    private void drawBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float fraction, int color) {
        drawPanel(graphics, x, y, width, height);
        int filled = Mth.clamp(Math.round(width * fraction), 0, width);
        if (filled > 0) graphics.fill(x, y, x + filled, y + height, 0xFF000000 | color);
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, imageWidth, imageHeight);

        for (Slot slot : menu.slots) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, 0.0F, 0.0F, TurretLayout.SLOT, TurretLayout.SLOT, TurretLayout.SLOT, TurretLayout.SLOT);
        }

        float healthFraction = (float) menu.getHealth() / menu.getMaxHealth();
        drawBar(graphics, leftPos + TurretLayout.MARGIN, topPos + layout.healthBarY, layout.barWidth, TurretLayout.BAR_HEIGHT, healthFraction, healthColor(healthFraction));

        if (menu.usesEnergy()) {
            float energyFraction = (float) menu.getEnergy() / menu.getMaxEnergy();
            drawBar(graphics, leftPos + TurretLayout.MARGIN, topPos + layout.energyBarY, layout.barWidth, TurretLayout.BAR_HEIGHT, energyFraction, ENERGY_COLOR);
        }
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        buttonRefreshers.forEach(Runnable::run);
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        if (allyListOpen(mouseX, mouseY)) {
            renderAllyList(graphics, mouseX, mouseY);
        }

        if (ownerNameTrimmed() && isHovering(TurretLayout.MARGIN, layout.gunSlotY + 5, ownerMaxWidth(), 9, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, ownerLabel(), mouseX, mouseY);
        }

        if (isHovering(TurretLayout.MARGIN, layout.healthBarY, layout.barWidth, TurretLayout.BAR_HEIGHT, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.tacz_turrets.health", menu.getHealth(), menu.getMaxHealth()), mouseX, mouseY);
        } else if (menu.usesEnergy() && isHovering(TurretLayout.MARGIN, layout.energyBarY, layout.barWidth, TurretLayout.BAR_HEIGHT, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.tacz_turrets.energy", menu.getEnergy(), menu.getMaxEnergy()), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component health = Component.literal(menu.getHealth() + " / " + menu.getMaxHealth());
        graphics.text(font, health, imageWidth - TurretLayout.MARGIN - font.width(health), titleLabelY, 0xFF404040, false);
        if (!menu.getOwnerName().isEmpty()) {
            graphics.text(font, trimmedOwnerLabel(), TurretLayout.MARGIN, layout.gunSlotY + 5, 0xFF404040, false);
        }
    }

    private Component ownerLabel() {
        return Component.translatable("gui.tacz_turrets.owner", menu.getOwnerName());
    }

    private int ownerMaxWidth() {
        return layout.gunSlotX - TurretLayout.MARGIN - 3;
    }

    private boolean ownerNameTrimmed() {
        return font.width(ownerLabel()) > ownerMaxWidth();
    }

    private String trimmedOwnerLabel() {
        String text = ownerLabel().getString();
        if (font.width(text) <= ownerMaxWidth()) return text;
        return font.plainSubstrByWidth(text, ownerMaxWidth() - font.width("...")) + "...";
    }

    private List<PlayerInfo> onlinePlayers() {
        if (minecraft == null || minecraft.getConnection() == null) return List.of();
        return minecraft.getConnection().getOnlinePlayers().stream()
                .filter(info -> minecraft.player == null || !info.getProfile().id().equals(minecraft.player.getUUID()))
                .filter(info -> !info.getProfile().name().equals(menu.getOwnerName()))
                .sorted(Comparator.comparing(info -> info.getProfile().name()))
                .limit(ALLY_MAX_ROWS)
                .toList();
    }

    private int allyButtonX() {
        return layout.laserSlotX + TurretLayout.SLOT + 6;
    }

    private int allyButtonWidth() {
        return layout.width - TurretLayout.MARGIN - allyButtonX();
    }

    private int allyListWidth() {
        int width = Math.max(allyButtonWidth(), ALLY_LIST_WIDTH);
        width = Math.max(width, font.width(Component.translatable("gui.tacz_turrets.allies.empty")) + 8);
        int allyTag = font.width(Component.translatable("gui.tacz_turrets.allies.ally"));
        for (PlayerInfo info : onlinePlayers()) {
            width = Math.max(width, 14 + font.width(info.getProfile().name()) + 8 + allyTag + 4);
        }
        return width;
    }

    private int allyListY() {
        return layout.gunSlotY + 1 + ALLY_BUTTON_HEIGHT;
    }

    private int allyListRows() {
        return Math.max(1, onlinePlayers().size());
    }

    private boolean allyListOpen(int mouseX, int mouseY) {
        if (!allyButton.active) return false;
        if (allyListPinned) return true;
        int height = ALLY_BUTTON_HEIGHT + 1 + allyListRows() * ALLY_ROW_HEIGHT;
        return isHovering(allyButtonX(), layout.gunSlotY + 1, allyListWidth(), height, mouseX, mouseY);
    }

    private int hoveredAllyRow(double mouseX, double mouseY) {
        int x = leftPos + allyButtonX();
        int y = topPos + allyListY();
        if (mouseX < x || mouseX >= x + allyListWidth() || mouseY < y) return -1;
        int row = (int) ((mouseY - y) / ALLY_ROW_HEIGHT);
        return row < onlinePlayers().size() ? row : -1;
    }

    private void renderAllyList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<PlayerInfo> players = onlinePlayers();
        int x = leftPos + allyButtonX();
        int y = topPos + allyListY();
        int width = allyListWidth();
        drawPanel(graphics, x, y, width, allyListRows() * ALLY_ROW_HEIGHT);

        if (players.isEmpty()) {
            graphics.text(font, Component.translatable("gui.tacz_turrets.allies.empty"), x + 3, y + 2, 0xFFA0A0A0, false);
            return;
        }

        int hovered = hoveredAllyRow(mouseX, mouseY);
        for (int index = 0; index < players.size(); index++) {
            PlayerInfo info = players.get(index);
            int rowY = y + index * ALLY_ROW_HEIGHT;
            if (index == hovered) graphics.fill(x, rowY, x + width, rowY + ALLY_ROW_HEIGHT, 0xFF303030);

            PlayerFaceExtractor.extractRenderState(graphics, info.getSkin(), x + 2, rowY + 1, 8);
            boolean ally = menu.isAlly(info.getProfile().id());
            graphics.text(font, info.getProfile().name(), x + 13, rowY + 2, ally ? 0xFF40D040 : 0xFFC0C0C0, false);
            if (ally) {
                Component tag = Component.translatable("gui.tacz_turrets.allies.ally");
                graphics.text(font, tag, x + width - 3 - font.width(tag), rowY + 2, 0xFF40D040, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int row = allyListOpen((int) event.x(), (int) event.y()) ? hoveredAllyRow(event.x(), event.y()) : -1;
        if (row >= 0) {
            UUID target = onlinePlayers().get(row).getProfile().id();
            menu.toggleAllyLocally(target);
            TACZTurretsNetwork.sendToServer(new ToggleAllyPacket(target));
            if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
