package com.entropy.tacz_turrets.util;

import com.entropy.tacz_turrets.TACZTurrets;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlacedTurrets extends SavedData {
    private final Map<UUID, Set<UUID>> turrets = new HashMap<>();

    public static final Codec<PlacedTurrets> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.CODEC.listOf()).xmap(
            map -> {
                PlacedTurrets data = new PlacedTurrets();
                map.forEach((owner, list) -> data.turrets.put(owner, new HashSet<>(list)));
                return data;
            },
            data -> {
                Map<UUID, List<UUID>> out = new HashMap<>();
                data.turrets.forEach((owner, set) -> {
                    if (!set.isEmpty()) out.put(owner, List.copyOf(set));
                });
                return out;
            }
    );

    public static final SavedDataType<PlacedTurrets> TYPE = new SavedDataType<>(TACZTurrets.id("placed"), PlacedTurrets::new, CODEC, null);

    public static PlacedTurrets get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int count(UUID owner) {
        return turrets.getOrDefault(owner, Set.of()).size();
    }

    public void add(UUID owner, UUID turret) {
        if (turrets.computeIfAbsent(owner, key -> new HashSet<>()).add(turret)) setDirty();
    }

    public void remove(UUID owner, UUID turret) {
        Set<UUID> placed = turrets.get(owner);
        if (placed != null && placed.remove(turret)) setDirty();
    }
}
