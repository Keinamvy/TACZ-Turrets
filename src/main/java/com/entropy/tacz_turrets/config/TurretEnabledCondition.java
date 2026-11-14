package com.entropy.tacz_turrets.config;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.turret.TurretType;
import com.entropy.tacz_turrets.util.Enums;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public record TurretEnabledCondition(TurretType type, Optional<Boolean> identical) implements ResourceCondition {
    public static final Identifier ID = TACZTurrets.id("turret_enabled");

    public static TurretEnabledCondition of(String turret, Optional<Boolean> identical) {
        return new TurretEnabledCondition(Enums.byName(turret.toUpperCase(Locale.ROOT), TurretType.MODERN), identical);
    }

    @Override
    public boolean test(@Nullable RegistryOps.RegistryInfoLookup registryLookup) {
        return TACZTurretsConfig.enabled(type) && identical.map(value -> value == TACZTurretsConfig.IDENTICAL_TURRETS.get()).orElse(true);
    }

    public static final MapCodec<TurretEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("turret").forGetter(condition -> condition.type.id()),
            Codec.BOOL.optionalFieldOf("identical").forGetter(TurretEnabledCondition::identical)
    ).apply(instance, TurretEnabledCondition::of));

    public static final ResourceConditionType<TurretEnabledCondition> TYPE = ResourceConditionType.create(ID, CODEC);

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }
}
