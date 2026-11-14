package com.entropy.tacz_turrets.client;

import com.entropy.tacz_turrets.client.renderer.TurretRenderer;
import com.entropy.tacz_turrets.client.screen.TurretScreen;
import com.entropy.tacz_turrets.registry.EntityTypeRegistry;
import com.entropy.tacz_turrets.registry.ItemRegistry;
import com.entropy.tacz_turrets.registry.MenuRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

import static com.entropy.tacz_turrets.TACZTurrets.MODID;

public class RenderRegistry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(EntityTypeRegistry.TURRET.get(), TurretRenderer::new);
        EntityRendererRegistry.register(EntityTypeRegistry.SENTRY.get(), TurretRenderer::new);
        MenuScreens.register(MenuRegistry.TURRET.get(), TurretScreen::new);
        TurretReloadSound.registerClient();
    }
}
