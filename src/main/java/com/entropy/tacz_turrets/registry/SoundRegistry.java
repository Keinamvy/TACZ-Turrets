package com.entropy.tacz_turrets.registry;

import com.entropy.tacz_turrets.TACZTurrets;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Supplier;

import static com.entropy.tacz_turrets.TACZTurrets.MODID;

public class SoundRegistry {
    public static final Supplier<SoundEvent> TURRET_PLACE = register("turret_place");
    public static final Supplier<SoundEvent> TURRET_PICKUP = register("turret_pickup");
    public static final Supplier<SoundEvent> TURRET_HURT = register("turret_hurt");
    public static final Supplier<SoundEvent> TURRET_LOCK_ON = register("turret_lock_on");
    public static final Supplier<SoundEvent> TURRET_PAN = register("turret_pan");

    private static Supplier<SoundEvent> register(String name) {
        SoundEvent event = Registry.register(BuiltInRegistries.SOUND_EVENT, TACZTurrets.id(name), SoundEvent.createVariableRangeEvent(TACZTurrets.id(name)));
        return () -> event;
    }

    public static void register() {}
}
