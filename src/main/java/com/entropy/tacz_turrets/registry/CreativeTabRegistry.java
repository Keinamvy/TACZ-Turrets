package com.entropy.tacz_turrets.registry;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.config.TACZTurretsConfig;
import com.entropy.tacz_turrets.item.TurretItem;
import com.entropy.tacz_turrets.turret.TurretType;
import com.tacz.guns.api.item.builder.BlockItemBuilder;
import com.tacz.guns.init.ModItems;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

import static com.entropy.tacz_turrets.TACZTurrets.MODID;

public class CreativeTabRegistry {
    public static final Supplier<CreativeModeTab> TURRETS;

    static {
        CreativeModeTab tab = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TACZTurrets.id(MODID), FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.tacz_turrets"))
                .icon(() -> new ItemStack(ItemRegistry.turret(TurretType.MODERN)))
                .displayItems((parameters, output) -> {
                    output.accept(BlockItemBuilder.create(ModItems.WORKBENCH_211).setId(TACZTurrets.id("turret_workbench")).build());
                    ItemRegistry.ITEMS.getEntries().forEach(item -> {
                        if (!(item.get() instanceof TurretItem turret) || TACZTurretsConfig.enabled(turret.getType())) output.accept(item.get());
                    });
                })
                .build());
        TURRETS = () -> tab;
    }

    public static void register() {}
}
