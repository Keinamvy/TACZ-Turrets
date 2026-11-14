package com.entropy.tacz_turrets.mixin;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.item.TurretItem;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GunSmithTableScreen.class)
public abstract class GunSmithTableScreenMixin extends AbstractContainerScreen<GunSmithTableMenu> {
    private GunSmithTableScreenMixin(GunSmithTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderPackInfo", at = @At("HEAD"), cancellable = true, remap = false)
    private void turretPackInfo(GuiGraphicsExtractor gui, GunSmithTableRecipe recipe, CallbackInfo ci) {
        if (recipe == null || !(recipe.getOutput().getItem() instanceof TurretItem)) return;
        PackInfo info = ClientAssetsManager.INSTANCE.getPackInfo(TACZTurrets.MODID);
        if (info == null) return;
        ci.cancel();

        Matrix3x2fStack pose = gui.pose();
        int darkGray = 0xFF555555;
        Component name = Component.translatable(info.getName());
        pose.pushMatrix();
        pose.scale(0.75F, 0.75F);
        gui.text(font, name, (int) ((leftPos + 6) / 0.75F), (int) ((topPos + 122) / 0.75F), darkGray, false);
        pose.popMatrix();

        pose.pushMatrix();
        pose.scale(0.5F, 0.5F);
        int x = (leftPos + 6) * 2;
        int y = (topPos + 123) * 2;
        gui.text(font, Component.literal("v" + info.getVersion()).withStyle(ChatFormatting.UNDERLINE), (int) (x + font.width(name) * 1.5F + 5), y, darkGray, false);
        y += 14;
        for (FormattedCharSequence line : font.split(Component.translatable(info.getDescription()), 245)) {
            gui.text(font, line, x, y, darkGray, false);
            y += font.lineHeight;
        }
        y += 3;
        gui.text(font, Component.translatable("gui.tacz.gun_smith_table.license").append(Component.literal(info.getLicense()).withStyle(ChatFormatting.DARK_GRAY)), x, y, darkGray, false);
        y += 12;
        gui.text(font, Component.translatable("gui.tacz.gun_smith_table.authors").append(Component.literal(String.join(", ", info.getAuthors())).withStyle(ChatFormatting.DARK_GRAY)), x, y, darkGray, false);
        pose.popMatrix();
    }
}
