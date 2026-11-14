package com.entropy.tacz_turrets;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.entropy.tacz_turrets.config.TACZTurretsConfig;
import com.entropy.tacz_turrets.turret.SentryLoadout;
import com.entropy.tacz_turrets.turret.SentrySpawn;
import com.entropy.tacz_turrets.turret.TurretType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.neoforged.fml.config.IConfigSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Config, SentryLoadout Range, and SentrySpawn Tests")
public class TACZTurretsConfigAndLoadoutTest {

    @BeforeAll
    static void initConfig() {
        if (!TACZTurretsConfig.SPEC.isLoaded()) {
            CommentedConfig config = CommentedConfig.inMemory();
            TACZTurretsConfig.SPEC.acceptConfig(new IConfigSpec.ILoadedConfig() {
                @Override public CommentedConfig config() { return config; }
                @Override public void save() {}
            });
        }
        if (!TACZTurretsConfig.COMMON_SPEC.isLoaded()) {
            CommentedConfig config = CommentedConfig.inMemory();
            TACZTurretsConfig.COMMON_SPEC.acceptConfig(new IConfigSpec.ILoadedConfig() {
                @Override public CommentedConfig config() { return config; }
                @Override public void save() {}
            });
        }
    }

    @Nested
    @DisplayName("TACZTurretsConfig Tests")
    class ConfigTests {

        @Test
        @DisplayName("Config SPEC, CLIENT_SPEC, and COMMON_SPEC are non-null and initialized")
        void testConfigSpecsNonNull() {
            assertNotNull(TACZTurretsConfig.SPEC, "TACZTurretsConfig.SPEC must not be null");
            assertNotNull(TACZTurretsConfig.CLIENT_SPEC, "TACZTurretsConfig.CLIENT_SPEC must not be null");
            assertNotNull(TACZTurretsConfig.COMMON_SPEC, "TACZTurretsConfig.COMMON_SPEC must not be null");

            assertNotNull(TACZTurretsConfig.CONSUME_AMMO.getDefault());
            assertNotNull(TACZTurretsConfig.TURRETS_TAKE_DAMAGE.getDefault());
            assertNotNull(TACZTurretsConfig.HEALTH_BAR_STYLE.getDefault());
            assertNotNull(TACZTurretsConfig.RECOIL_TYPE.getDefault());
        }

        @Test
        @DisplayName("Every TurretType has non-null stats() with valid default values (> 0)")
        void testTurretTypeStatsDefaults() {
            TurretType[] types = TurretType.values();
            assertEquals(10, types.length);

            for (TurretType type : types) {
                TACZTurretsConfig.TurretStats stats = type.stats();
                assertNotNull(stats, "stats() should not be null for " + type);

                assertNotNull(stats.health, "health should not be null for " + type);
                assertTrue(stats.health.getDefault() > 0,
                        "health.getDefault() > 0 for " + type + ", got: " + stats.health.getDefault());

                assertNotNull(stats.range, "range should not be null for " + type);
                assertTrue(stats.range.getDefault() > 0,
                        "range.getDefault() > 0 for " + type + ", got: " + stats.range.getDefault());

                assertNotNull(stats.fireRate, "fireRate should not be null for " + type);
                assertTrue(stats.fireRate.getDefault() > 0.0D,
                        "fireRate.getDefault() > 0 for " + type + ", got: " + stats.fireRate.getDefault());

                assertNotNull(stats.reloadSpeed, "reloadSpeed should not be null for " + type);
                assertTrue(stats.reloadSpeed.getDefault() > 0.0D,
                        "reloadSpeed.getDefault() > 0 for " + type + ", got: " + stats.reloadSpeed.getDefault());

                assertNotNull(stats.slotRows, "slotRows should not be null for " + type);
                assertTrue(stats.slotRows.getDefault() > 0, "slotRows.getDefault() > 0 for " + type);

                assertNotNull(stats.slotLength, "slotLength should not be null for " + type);
                assertTrue(stats.slotLength.getDefault() > 0, "slotLength.getDefault() > 0 for " + type);

                assertNotNull(stats.energyCapacity, "energyCapacity should not be null for " + type);
                assertTrue(stats.energyCapacity.getDefault() > 0, "energyCapacity.getDefault() > 0 for " + type);

                assertNotNull(stats.energyTransferRate, "energyTransferRate should not be null for " + type);
                assertTrue(stats.energyTransferRate.getDefault() > 0, "energyTransferRate.getDefault() > 0 for " + type);
            }
        }
    }

    @Nested
    @DisplayName("SentryLoadout.Range Tests")
    class RangeTests {

        @Test
        @DisplayName("Range.CODEC JSON decoding and encoding round-trip")
        void testRangeCodecRoundTrip() {
            JsonObject json = new JsonObject();
            json.addProperty("min", 4);
            json.addProperty("max", 12);

            DataResult<SentryLoadout.Range> parseResult = SentryLoadout.Range.CODEC.parse(JsonOps.INSTANCE, json);
            assertTrue(parseResult.result().isPresent(), "Parsing valid Range json should succeed");
            SentryLoadout.Range range = parseResult.result().get();
            assertEquals(4, range.min());
            assertEquals(12, range.max());

            // Encoding
            DataResult<JsonElement> encodeResult = SentryLoadout.Range.CODEC.encodeStart(JsonOps.INSTANCE, range);
            assertTrue(encodeResult.result().isPresent(), "Encoding Range should succeed");
            JsonObject encodedJson = encodeResult.result().get().getAsJsonObject();
            assertEquals(4, encodedJson.get("min").getAsInt());
            assertEquals(12, encodedJson.get("max").getAsInt());

            // Round-trip parse from encoded
            DataResult<SentryLoadout.Range> roundTripResult = SentryLoadout.Range.CODEC.parse(JsonOps.INSTANCE, encodedJson);
            assertTrue(roundTripResult.result().isPresent());
            assertEquals(range, roundTripResult.result().get());
        }

        @Test
        @DisplayName("Range.roll(RandomSource.create(42L)) always returns values within [min, max]")
        void testRangeRollWithinBounds() {
            SentryLoadout.Range range = new SentryLoadout.Range(5, 15);
            RandomSource random = RandomSource.create(42L);

            for (int i = 0; i < 200; i++) {
                int rolled = range.roll(random);
                assertTrue(rolled >= 5 && rolled <= 15,
                        "Rolled value " + rolled + " outside expected range [5, 15] on iteration " + i);
            }

            // Single value range always rolls that value
            SentryLoadout.Range single = new SentryLoadout.Range(7, 7);
            for (int i = 0; i < 50; i++) {
                assertEquals(7, single.roll(random), "Single-value range should always roll 7");
            }

            // Inverted bounds (min > max): roll() uses Math.max(min, max) so min <= rolled <= min
            SentryLoadout.Range inverted = new SentryLoadout.Range(20, 10);
            for (int i = 0; i < 50; i++) {
                assertEquals(20, inverted.roll(random), "Inverted range [20, 10] should evaluate Math.max(min, max) = 20");
            }
        }
    }

    @Nested
    @DisplayName("SentrySpawn.CODEC Tests")
    class SentrySpawnTests {

        @Test
        @DisplayName("SentrySpawn.CODEC parses valid JSON for structure, surface, and cave sentry spawn rules")
        void testParseValidWhereValues() {
            // Structure
            JsonObject structureJson = new JsonObject();
            structureJson.addProperty("where", "structure");
            DataResult<SentrySpawn> structureResult = SentrySpawn.CODEC.parse(JsonOps.INSTANCE, structureJson);
            assertTrue(structureResult.result().isPresent(), "Failed to parse structure: " + structureResult.error());
            SentrySpawn structureSpawn = structureResult.result().get();
            assertEquals(SentrySpawn.Where.STRUCTURE, structureSpawn.where());
            assertEquals(1.0F, structureSpawn.chance());
            assertEquals(new SentryLoadout.Range(1, 1), structureSpawn.count());

            // Surface
            JsonObject surfaceJson = new JsonObject();
            surfaceJson.addProperty("where", "surface");
            DataResult<SentrySpawn> surfaceResult = SentrySpawn.CODEC.parse(JsonOps.INSTANCE, surfaceJson);
            assertTrue(surfaceResult.result().isPresent(), "Failed to parse surface: " + surfaceResult.error());
            SentrySpawn surfaceSpawn = surfaceResult.result().get();
            assertEquals(SentrySpawn.Where.SURFACE, surfaceSpawn.where());

            // Cave
            JsonObject caveJson = new JsonObject();
            caveJson.addProperty("where", "cave");
            DataResult<SentrySpawn> caveResult = SentrySpawn.CODEC.parse(JsonOps.INSTANCE, caveJson);
            assertTrue(caveResult.result().isPresent(), "Failed to parse cave: " + caveResult.error());
            SentrySpawn caveSpawn = caveResult.result().get();
            assertEquals(SentrySpawn.Where.CAVE, caveSpawn.where());
        }

        @Test
        @DisplayName("SentrySpawn.CODEC parses full sentry spawn configuration")
        void testParseFullSentrySpawnConfig() {
            JsonObject json = new JsonObject();
            json.addProperty("where", "structure");
            json.addProperty("loadout", "tacz_turrets:custom_loadout");

            JsonArray structures = new JsonArray();
            structures.add("minecraft:pillager_outpost");
            json.add("structures", structures);

            JsonArray biomes = new JsonArray();
            biomes.add("minecraft:plains");
            json.add("biomes", biomes);

            json.addProperty("chance", 0.75F);

            JsonObject count = new JsonObject();
            count.addProperty("min", 2);
            count.addProperty("max", 5);
            json.add("count", count);

            json.addProperty("ceiling_chance", 0.15F);
            json.addProperty("wall_chance", 0.30F);

            DataResult<SentrySpawn> result = SentrySpawn.CODEC.parse(JsonOps.INSTANCE, json);
            assertTrue(result.result().isPresent(), "Failed to parse full SentrySpawn: " + result.error());
            SentrySpawn spawn = result.result().get();

            assertEquals(SentrySpawn.Where.STRUCTURE, spawn.where());
            assertEquals(Identifier.parse("tacz_turrets:custom_loadout"), spawn.loadout());
            assertEquals(List.of("minecraft:pillager_outpost"), spawn.structures());
            assertEquals(List.of("minecraft:plains"), spawn.biomes());
            assertEquals(0.75F, spawn.chance(), 1e-4F);
            assertEquals(2, spawn.count().min());
            assertEquals(5, spawn.count().max());
            assertEquals(0.15F, spawn.ceilingChance(), 1e-4F);
            assertEquals(0.30F, spawn.wallChance(), 1e-4F);
        }

        @Test
        @DisplayName("SentrySpawn.CODEC rejects invalid where values")
        void testRejectsInvalidWhereValues() {
            String[] invalidWheres = {"sky", "nether", "void", "space", "unknown", "", "SURFACE_AND_CAVE"};

            for (String invalid : invalidWheres) {
                JsonObject json = new JsonObject();
                json.addProperty("where", invalid);

                DataResult<SentrySpawn> result = SentrySpawn.CODEC.parse(JsonOps.INSTANCE, json);
                assertTrue(result.error().isPresent(),
                        "Should produce error for invalid where value '" + invalid + "'");
                assertTrue(result.result().isEmpty(),
                        "Result should be empty for invalid where value '" + invalid + "'");
            }
        }
    }
}
