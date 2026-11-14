package com.entropy.tacz_turrets.registry;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.menu.TurretMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public class MenuRegistry {
    public static final Supplier<MenuType<TurretMenu>> TURRET;

    static {
        MenuType<TurretMenu> menu = Registry.register(
                BuiltInRegistries.MENU,
                TACZTurrets.id("turret"),
                new ExtendedMenuType<>(
                        TurretMenu::new,
                        StreamCodec.of(
                                (out, buf) -> out.writeBytes(buf, buf.readerIndex(), buf.readableBytes()),
                                in -> new FriendlyByteBuf(in.readBytes(in.readableBytes()))
                        )
                )
        );
        TURRET = () -> menu;
    }

    public static void register() {}
}
