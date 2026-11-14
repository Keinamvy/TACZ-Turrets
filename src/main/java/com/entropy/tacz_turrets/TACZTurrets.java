package com.entropy.tacz_turrets;

import com.entropy.tacz_turrets.command.TurretCommand;
import com.entropy.tacz_turrets.config.TACZTurretsConfig;
import com.entropy.tacz_turrets.config.TurretEnabledCondition;
import com.entropy.tacz_turrets.event.TurretEventHandler;
import com.entropy.tacz_turrets.network.TACZTurretsNetwork;
import com.entropy.tacz_turrets.registry.CreativeTabRegistry;
import com.entropy.tacz_turrets.registry.EntityTypeRegistry;
import com.entropy.tacz_turrets.registry.ItemRegistry;
import com.entropy.tacz_turrets.registry.MenuRegistry;
import com.entropy.tacz_turrets.registry.SoundRegistry;
import com.entropy.tacz_turrets.turret.TurretEntity;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.item.ammo.AmmoSource;
import com.tacz.guns.api.item.ammo.AmmoSourceRegistry;
import com.tacz.guns.init.ModCreativeTabs;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

public class TACZTurrets implements ModInitializer {
    public static final String MODID = "tacz_turrets";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Identifier TURRET_WORKBENCH = id("turret_workbench");

    @Override
    public void onInitialize() {
        ConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.SERVER, TACZTurretsConfig.SPEC);
        ConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.CLIENT, TACZTurretsConfig.CLIENT_SPEC);
        ConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.COMMON, TACZTurretsConfig.COMMON_SPEC);
        ModConfigEvents.loading(MODID).register(TACZTurretsConfig::onLoad);
        ModConfigEvents.reloading(MODID).register(TACZTurretsConfig::onLoad);

        EntityTypeRegistry.register();
        ItemRegistry.register();
        CreativeTabRegistry.register();
        MenuRegistry.register();
        SoundRegistry.register();

        TACZTurretsNetwork.register();
        TurretEventHandler.register();
        TurretCommand.register();

        ResourceConditions.register(TurretEnabledCondition.TYPE);

        FabricDefaultAttributeRegistry.register(EntityTypeRegistry.TURRET.get(), TurretEntity.createLivingAttributes());
        FabricDefaultAttributeRegistry.register(EntityTypeRegistry.SENTRY.get(), TurretEntity.createLivingAttributes());

        BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(ModCreativeTabs.OTHER_TAB).ifPresent(key -> {
            CreativeModeTabEvents.modifyOutputEvent(key).register(entries -> {
                ItemRegistry.TURRETS.forEach((type, item) -> {
                    if (TACZTurretsConfig.enabled(type)) entries.accept(item.get());
                });
            });
        });

        AmmoSourceRegistry.EVENT.register((shooter, gunStack) -> {
            if (shooter instanceof TurretEntity turret) {
                return new AmmoSource() {
                    @Override
                    public boolean hasAmmo(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.item.ItemStack gun) {
                        if (turret.hasInfiniteAmmo()) return true;
                        return AmmoSourceRegistry.hasAmmo(turret.getInventory(), gun);
                    }

                    @Override
                    public int consumeAmmo(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.item.ItemStack gun, int amount) {
                        if (turret.hasInfiniteAmmo()) return amount;
                        return AmmoSourceRegistry.consumeAmmo(turret.getInventory(), gun, amount);
                    }
                };
            }
            return null;
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
