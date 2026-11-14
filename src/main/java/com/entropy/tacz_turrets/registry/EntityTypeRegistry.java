package com.entropy.tacz_turrets.registry;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.turret.TurretEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public class EntityTypeRegistry {
    public static final Supplier<EntityType<TurretEntity>> TURRET;
    public static final Supplier<EntityType<TurretEntity>> SENTRY;

    static {
        ResourceKey<EntityType<?>> turretKey = ResourceKey.create(Registries.ENTITY_TYPE, TACZTurrets.id("turret"));
        ResourceKey<EntityType<?>> sentryKey = ResourceKey.create(Registries.ENTITY_TYPE, TACZTurrets.id("sentry_turret"));
        EntityType<TurretEntity> turret = Registry.register(BuiltInRegistries.ENTITY_TYPE, turretKey, EntityType.Builder.<TurretEntity>of(TurretEntity::new, MobCategory.MISC).sized(1f, 1f).build(turretKey));
        EntityType<TurretEntity> sentry = Registry.register(BuiltInRegistries.ENTITY_TYPE, sentryKey, EntityType.Builder.<TurretEntity>of(TurretEntity::new, MobCategory.MISC).sized(1f, 1f).build(sentryKey));
        TURRET = () -> turret;
        SENTRY = () -> sentry;
    }

    public static void register() {}
}
