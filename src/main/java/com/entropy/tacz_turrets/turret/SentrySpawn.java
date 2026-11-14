package com.entropy.tacz_turrets.turret;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.config.TACZTurretsConfig;
import com.entropy.tacz_turrets.registry.EntityTypeRegistry;
import com.entropy.tacz_turrets.util.RegistryFilter;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record SentrySpawn(Where where, Identifier loadout, List<String> structures, List<String> biomes, float chance, SentryLoadout.Range count, float ceilingChance, float wallChance) {
    private static final int ATTEMPTS = 16;
    private static final int CAVE_SCAN = 32;
    private static final List<Direction> FLOOR = List.of(Direction.DOWN);
    private static final Codec<Float> CHANCE = Codec.floatRange(0.0F, 1.0F);
    public static final Codec<SentrySpawn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.comapFlatMap(Where::parse, where -> where.name().toLowerCase(Locale.ROOT)).fieldOf("where").forGetter(SentrySpawn::where),
            Identifier.CODEC.optionalFieldOf("loadout", SentryLoadout.DEFAULT).forGetter(SentrySpawn::loadout),
            Codec.STRING.listOf().optionalFieldOf("structures", List.of()).forGetter(SentrySpawn::structures),
            Codec.STRING.listOf().optionalFieldOf("biomes", List.of()).forGetter(SentrySpawn::biomes),
            CHANCE.optionalFieldOf("chance", 1.0F).forGetter(SentrySpawn::chance),
            SentryLoadout.Range.CODEC.optionalFieldOf("count", new SentryLoadout.Range(1, 1)).forGetter(SentrySpawn::count),
            CHANCE.optionalFieldOf("ceiling_chance", 0.0F).forGetter(SentrySpawn::ceilingChance),
            CHANCE.optionalFieldOf("wall_chance", 0.0F).forGetter(SentrySpawn::wallChance)
    ).apply(instance, SentrySpawn::new));
    private static volatile Map<Identifier, SentrySpawn> spawns = Map.of();

    public enum Where {
        STRUCTURE, SURFACE, CAVE;

        static DataResult<Where> parse(String name) {
            for (Where where : values()) if (where.name().equalsIgnoreCase(name)) return DataResult.success(where);
            return DataResult.error(() -> "Unknown sentry spawn place " + name);
        }
    }

    @SuppressWarnings("unchecked")
    public static void placeInStructure(WorldGenLevel level, StructureStart start, BoundingBox chunkBox) {
        if (spawns.isEmpty() || start.getPieces().isEmpty()) return;
        Registry<Structure> registry = (Registry<Structure>) (Object) level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        spawns.forEach((id, spawn) -> {
            if (spawn.where != Where.STRUCTURE || !matches(registry, spawn.structures, start.getStructure())) return;
            RandomSource random = RandomSource.create(level.getSeed() ^ start.getChunkPos().pack() * 31L ^ id.hashCode());
            if (random.nextFloat() >= spawn.chance) return;
            for (int count = spawn.count.roll(random); count > 0; count--) {
                BoundingBox box = start.getPieces().get(random.nextInt(start.getPieces().size())).getBoundingBox();
                int x = Mth.randomBetweenInclusive(random, box.minX(), box.maxX());
                int z = Mth.randomBetweenInclusive(random, box.minZ(), box.maxZ());
                float side = random.nextFloat();
                RandomSource retry = RandomSource.create(random.nextLong());
                if (x < chunkBox.minX() || x > chunkBox.maxX() || z < chunkBox.minZ() || z > chunkBox.maxZ()) continue;
                List<Direction> faces = spawn.faces(side, retry);
                int minX = Math.max(box.minX(), chunkBox.minX()), maxX = Math.min(box.maxX(), chunkBox.maxX());
                int minZ = Math.max(box.minZ(), chunkBox.minZ()), maxZ = Math.min(box.maxZ(), chunkBox.maxZ());
                for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                    Spot spot = scan(level, x, z, box.maxY(), box.minY(), faces);
                    if (spot != null && spawn.place(level, spot, retry, EntitySpawnReason.STRUCTURE)) break;
                    x = Mth.randomBetweenInclusive(retry, minX, maxX);
                    z = Mth.randomBetweenInclusive(retry, minZ, maxZ);
                }
            }
        });
    }

    public static void placeNaturally(WorldGenLevel level, ChunkPos chunkPos) {
        if (spawns.isEmpty() || isProtected(level, chunkPos)) return;
        spawns.forEach((id, spawn) -> {
            if (spawn.where == Where.STRUCTURE || !(spawn.where == Where.SURFACE ? TACZTurretsConfig.SURFACE_SENTRY_SPAWNS : TACZTurretsConfig.CAVE_SENTRY_SPAWNS).get()) return;
            RandomSource random = RandomSource.create(level.getSeed() ^ chunkPos.pack() * 31L ^ id.hashCode());
            if (random.nextFloat() >= spawn.chance) return;
            for (int count = spawn.count.roll(random); count > 0; count--) {
                for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                    int x = chunkPos.getMinBlockX() + random.nextInt(16);
                    int z = chunkPos.getMinBlockZ() + random.nextInt(16);
                    Spot spot = spawn.where == Where.SURFACE ? surface(level, x, z) : cave(level, random, x, z, spawn.faces(random.nextFloat(), random));
                    if (spot != null && spawn.place(level, spot, random, EntitySpawnReason.CHUNK_GENERATION)) break;
                }
            }
        });
    }

    private static boolean isProtected(WorldGenLevel level, ChunkPos chunkPos) {
        int radius = TACZTurretsConfig.SENTRY_SPAWN_PROTECTION.get();
        if (radius <= 0 || level.getLevel().dimension() != Level.OVERWORLD) return false;
        BlockPos spawnPos = level.getLevel().getRespawnData().pos();
        double dx = chunkPos.getMiddleBlockX() - spawnPos.getX(), dz = chunkPos.getMiddleBlockZ() - spawnPos.getZ();
        return dx * dx + dz * dz < (double) radius * radius;
    }

    private List<Direction> faces(float side, RandomSource random) {
        if (side < wallChance) return Direction.Plane.HORIZONTAL.shuffledCopy(random);
        return side < wallChance + ceilingChance ? List.of(Direction.UP) : FLOOR;
    }

    @Nullable
    private static Spot surface(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return scan(level, x, z, y, y, FLOOR);
    }

    @Nullable
    private static Spot cave(WorldGenLevel level, RandomSource random, int x, int z, List<Direction> faces) {
        int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 8;
        int bottom = level.getMinY() + 8;
        if (top <= bottom) return null;
        int y = Mth.randomBetweenInclusive(random, bottom, top);
        return scan(level, x, z, y, faces.get(0) == Direction.UP ? Math.min(top, y + CAVE_SCAN) : Math.max(bottom, y - CAVE_SCAN), faces);
    }

    @Nullable
    private static Spot scan(WorldGenLevel level, int x, int z, int from, int to, List<Direction> faces) {
        int step = from <= to ? 1 : -1;
        for (int y = from; y != to + step; y += step) {
            BlockPos pos = new BlockPos(x, y, z);
            for (Direction face : faces) if (isSpot(level, pos, face)) return new Spot(pos, face);
        }
        return null;
    }

    private static boolean isSpot(WorldGenLevel level, BlockPos pos, Direction face) {
        BlockPos support = pos.relative(face);
        return isOpen(level, pos) && isOpen(level, pos.relative(face.getOpposite())) && level.getBlockState(support).isFaceSturdy(level, support, face.getOpposite());
    }

    private static boolean isOpen(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    private static <T> boolean matches(Registry<T> registry, List<String> entries, T value) {
        return entries.isEmpty() || RegistryFilter.of(registry, entries).matches(value);
    }

    @SuppressWarnings("unchecked")
    private boolean place(WorldGenLevel level, Spot spot, RandomSource random, EntitySpawnReason spawnType) {
        BlockPos pos = spot.pos();
        if (SentryLoadout.get(loadout).map(sentry -> sentry.enabledTypes().isEmpty()).orElse(true)) return false;
        if (!matches((Registry<Biome>) (Object) level.registryAccess().lookupOrThrow(Registries.BIOME), biomes, level.getBiome(pos).value())) return false;
        TurretEntity sentry = EntityTypeRegistry.SENTRY.get().create(level.getLevel(), spawnType);
        if (sentry == null) return false;
        sentry.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        sentry.setYRot(random.nextFloat() * 360.0F);
        sentry.setXRot(0.0F);
        sentry.setYHeadRot(sentry.getYRot());
        sentry.setYBodyRot(sentry.getYRot());
        sentry.setAttachFace(spot.face());
        sentry.setLoadout(loadout);
        sentry.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), spawnType, null);
        level.addFreshEntityWithPassengers(sentry);
        return true;
    }

    private record Spot(BlockPos pos, Direction face) {
    }

    public static class Listener extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> implements net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener {
        @Override
        public Identifier getFabricId() {
            return TACZTurrets.id("sentry_spawns");
        }

        @Override
        protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
            Map<Identifier, JsonElement> map = new HashMap<>();
            String directory = "sentry_spawns";
            for (Map.Entry<Identifier, Resource> entry : manager.listResources(directory, file -> file.getPath().endsWith(".json")).entrySet()) {
                Identifier fileId = entry.getKey();
                String path = fileId.getPath();
                String subPath = path.substring(directory.length() + 1, path.length() - ".json".length());
                Identifier id = Identifier.fromNamespaceAndPath(fileId.getNamespace(), subPath);
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    map.put(id, json);
                } catch (Exception e) {
                    TACZTurrets.LOGGER.error("Couldn't parse sentry spawn {}", fileId, e);
                }
            }
            return map;
        }

        @Override
        protected void apply(@NotNull Map<Identifier, JsonElement> files, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
            Map<Identifier, SentrySpawn> loaded = new HashMap<>();
            files.forEach((id, json) -> {
                DataResult<SentrySpawn> result = CODEC.parse(JsonOps.INSTANCE, json);
                result.result().ifPresent(spawn -> loaded.put(id, spawn));
                result.error().ifPresent(error -> TACZTurrets.LOGGER.error("Couldn't load sentry spawn {}: {}", id, error.message()));
            });
            spawns = loaded;
        }
    }
}
