package com.entropy.tacz_turrets.util;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class RegistryFilter<T> {
    private final Supplier<Registry<T>> registry;
    private final Set<Identifier> ids = new HashSet<>();
    private final List<TagKey<T>> tags = new ArrayList<>();

    private RegistryFilter(Supplier<Registry<T>> registry) {
        this.registry = registry;
    }

    public static <T> RegistryFilter<T> of(Registry<T> registry, List<? extends String> entries) {
        return of(registry.key(), () -> registry, entries);
    }

    public static <T> RegistryFilter<T> of(ResourceKey<? extends Registry<T>> key, Supplier<Registry<T>> registry, List<? extends String> entries) {
        RegistryFilter<T> filter = new RegistryFilter<>(registry);
        for (String entry : entries) {
            String trimmed = entry.trim();
            boolean tag = trimmed.startsWith("#");
            Identifier id = Identifier.tryParse(tag ? trimmed.substring(1) : trimmed);
            if (id == null) continue;
            if (tag) {
                filter.tags.add(TagKey.create(key, id));
            } else {
                filter.ids.add(id);
            }
        }
        return filter;
    }

    public static boolean isValidEntry(Object entry) {
        if (!(entry instanceof String string) || string.isBlank()) return false;
        String trimmed = string.trim();
        return Identifier.tryParse(trimmed.startsWith("#") ? trimmed.substring(1) : trimmed) != null;
    }

    public boolean isEmpty() {
        return ids.isEmpty() && tags.isEmpty();
    }

    public boolean matches(T value) {
        Registry<T> reg = registry.get();
        return ids.contains(reg.getKey(value)) || tags.stream().anyMatch(reg.wrapAsHolder(value)::is);
    }
}
