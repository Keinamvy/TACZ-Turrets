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

public class TurretAllies extends SavedData {
    private final Map<UUID, Set<UUID>> allies = new HashMap<>();

    public static final Codec<TurretAllies> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.CODEC.listOf()).xmap(
            map -> {
                TurretAllies data = new TurretAllies();
                map.forEach((owner, list) -> data.allies.put(owner, new HashSet<>(list)));
                return data;
            },
            data -> {
                Map<UUID, List<UUID>> out = new HashMap<>();
                data.allies.forEach((owner, set) -> {
                    if (!set.isEmpty()) out.put(owner, List.copyOf(set));
                });
                return out;
            }
    );

    public static final SavedDataType<TurretAllies> TYPE = new SavedDataType<>(TACZTurrets.id("allies"), TurretAllies::new, CODEC, null);

    public static TurretAllies get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isAlly(UUID owner, UUID other) {
        Set<UUID> trusted = allies.get(owner);
        return trusted != null && trusted.contains(other);
    }

    public Set<UUID> getAllies(UUID owner) {
        return Set.copyOf(allies.getOrDefault(owner, Set.of()));
    }

    public boolean addAlly(UUID owner, UUID ally) {
        if (owner.equals(ally)) return false;
        if (!allies.computeIfAbsent(owner, key -> new HashSet<>()).add(ally)) return false;
        setDirty();
        return true;
    }

    public boolean removeAlly(UUID owner, UUID ally) {
        Set<UUID> trusted = allies.get(owner);
        if (trusted == null || !trusted.remove(ally)) return false;
        setDirty();
        return true;
    }

    public void toggleAlly(UUID owner, UUID ally) {
        if (!addAlly(owner, ally)) removeAlly(owner, ally);
    }
}
