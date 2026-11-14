package com.entropy.tacz_turrets;

import com.entropy.tacz_turrets.util.PlacedTurrets;
import com.entropy.tacz_turrets.util.TurretAllies;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Turret Allies and Placed Turrets Tests")
public class TurretAlliesAndPlacedTurretsTest {

    @Nested
    @DisplayName("PlacedTurrets Tests")
    class PlacedTurretsTests {

        @Test
        @DisplayName("count(owner) initially 0, add increments count, duplicate add is idempotent, isDirty() set")
        void testAddAndCount() {
            PlacedTurrets placed = new PlacedTurrets();
            UUID owner = UUID.randomUUID();
            UUID turret1 = UUID.randomUUID();
            UUID turret2 = UUID.randomUUID();

            assertEquals(0, placed.count(owner), "Initial count should be 0");
            assertFalse(placed.isDirty(), "Newly instantiated data should not be dirty");

            placed.add(owner, turret1);
            assertEquals(1, placed.count(owner), "Count should be 1 after adding turret1");
            assertTrue(placed.isDirty(), "isDirty() must be true after adding a turret");

            placed.add(owner, turret2);
            assertEquals(2, placed.count(owner), "Count should be 2 after adding turret2");

            // Duplicate add is idempotent
            placed.add(owner, turret1);
            assertEquals(2, placed.count(owner), "Duplicate add should not increase count");
        }

        @Test
        @DisplayName("remove(owner, turret) decrements count and handles non-existent turrets")
        void testRemove() {
            PlacedTurrets placed = new PlacedTurrets();
            UUID owner = UUID.randomUUID();
            UUID turret1 = UUID.randomUUID();
            UUID turret2 = UUID.randomUUID();

            placed.add(owner, turret1);
            placed.add(owner, turret2);
            assertEquals(2, placed.count(owner));

            placed.remove(owner, turret1);
            assertEquals(1, placed.count(owner), "Count should decrement to 1 after removing turret1");

            // Removing non-existent turret has no effect
            placed.remove(owner, UUID.randomUUID());
            assertEquals(1, placed.count(owner), "Removing unknown turret should keep count at 1");

            placed.remove(owner, turret2);
            assertEquals(0, placed.count(owner), "Count should decrement to 0 after removing turret2");

            // Removing from unknown owner has no effect
            placed.remove(UUID.randomUUID(), turret1);
            assertEquals(0, placed.count(owner));
        }

        @Test
        @DisplayName("Round-trip serialization via PlacedTurrets.CODEC preserves all owners and turret UUIDs")
        void testCodecRoundTrip() {
            PlacedTurrets original = new PlacedTurrets();
            UUID ownerA = UUID.randomUUID();
            UUID turretA1 = UUID.randomUUID();
            UUID turretA2 = UUID.randomUUID();

            UUID ownerB = UUID.randomUUID();
            UUID turretB1 = UUID.randomUUID();

            original.add(ownerA, turretA1);
            original.add(ownerA, turretA2);
            original.add(ownerB, turretB1);

            DataResult<JsonElement> encodeResult = PlacedTurrets.CODEC.encodeStart(JsonOps.INSTANCE, original);
            assertTrue(encodeResult.result().isPresent(), "encodeStart must succeed: " + encodeResult.error());
            JsonElement json = encodeResult.result().get();

            DataResult<PlacedTurrets> decodeResult = PlacedTurrets.CODEC.parse(JsonOps.INSTANCE, json);
            assertTrue(decodeResult.result().isPresent(), "parse must succeed: " + decodeResult.error());
            PlacedTurrets loaded = decodeResult.result().get();

            assertEquals(2, loaded.count(ownerA), "ownerA should have 2 turrets");
            assertEquals(1, loaded.count(ownerB), "ownerB should have 1 turret");
            assertEquals(0, loaded.count(UUID.randomUUID()), "unknown owner should have 0 turrets");

            // Verify the specific turret UUIDs are retained by removing them
            loaded.remove(ownerA, turretA1);
            assertEquals(1, loaded.count(ownerA));
            loaded.remove(ownerA, turretA2);
            assertEquals(0, loaded.count(ownerA));

            loaded.remove(ownerB, turretB1);
            assertEquals(0, loaded.count(ownerB));
        }
    }

    @Nested
    @DisplayName("TurretAllies Tests")
    class TurretAlliesTests {

        @Test
        @DisplayName("isAlly(owner, ally) initially false")
        void testInitiallyNotAlly() {
            TurretAllies allies = new TurretAllies();
            UUID owner = UUID.randomUUID();
            UUID ally = UUID.randomUUID();

            assertFalse(allies.isAlly(owner, ally));
            assertEquals(Set.of(), allies.getAllies(owner));
        }

        @Test
        @DisplayName("addAlly(owner, owner) returns false (cannot ally with self)")
        void testCannotAllyWithSelf() {
            TurretAllies allies = new TurretAllies();
            UUID owner = UUID.randomUUID();

            assertFalse(allies.addAlly(owner, owner), "Owner cannot add self as ally");
            assertFalse(allies.isAlly(owner, owner));
            assertEquals(Set.of(), allies.getAllies(owner));
        }

        @Test
        @DisplayName("addAlly(owner, ally) returns true, sets isAlly, duplicate add returns false")
        void testAddAllyAndDuplicate() {
            TurretAllies allies = new TurretAllies();
            UUID owner = UUID.randomUUID();
            UUID ally = UUID.randomUUID();

            assertTrue(allies.addAlly(owner, ally), "First addAlly should return true");
            assertTrue(allies.isAlly(owner, ally), "isAlly must return true after addition");
            assertTrue(allies.isDirty(), "isDirty() must be true after modifying allies");
            assertEquals(Set.of(ally), allies.getAllies(owner));

            // Duplicate add
            assertFalse(allies.addAlly(owner, ally), "Duplicate addAlly should return false");
            assertEquals(Set.of(ally), allies.getAllies(owner));
        }

        @Test
        @DisplayName("removeAlly(owner, ally) returns true, sets isAlly false, duplicate remove returns false")
        void testRemoveAlly() {
            TurretAllies allies = new TurretAllies();
            UUID owner = UUID.randomUUID();
            UUID ally = UUID.randomUUID();

            allies.addAlly(owner, ally);
            assertTrue(allies.isAlly(owner, ally));

            assertTrue(allies.removeAlly(owner, ally), "First removeAlly should return true");
            assertFalse(allies.isAlly(owner, ally), "isAlly must return false after removal");
            assertEquals(Set.of(), allies.getAllies(owner));

            // Duplicate remove
            assertFalse(allies.removeAlly(owner, ally), "Duplicate removeAlly should return false");

            // Non-existent ally on another owner
            assertFalse(allies.removeAlly(UUID.randomUUID(), ally), "Removing ally from unknown owner returns false");
        }

        @Test
        @DisplayName("toggleAlly(owner, ally) toggles membership on and off")
        void testToggleAlly() {
            TurretAllies allies = new TurretAllies();
            UUID owner = UUID.randomUUID();
            UUID ally = UUID.randomUUID();

            assertFalse(allies.isAlly(owner, ally));

            // Toggle on
            allies.toggleAlly(owner, ally);
            assertTrue(allies.isAlly(owner, ally), "First toggle should add ally");

            // Toggle off
            allies.toggleAlly(owner, ally);
            assertFalse(allies.isAlly(owner, ally), "Second toggle should remove ally");

            // Toggle on again
            allies.toggleAlly(owner, ally);
            assertTrue(allies.isAlly(owner, ally), "Third toggle should re-add ally");
        }

        @Test
        @DisplayName("Round-trip serialization via TurretAllies.CODEC preserves all ally mappings")
        void testCodecRoundTrip() {
            TurretAllies original = new TurretAllies();
            UUID owner1 = UUID.randomUUID();
            UUID ally1A = UUID.randomUUID();
            UUID ally1B = UUID.randomUUID();

            UUID owner2 = UUID.randomUUID();
            UUID ally2A = UUID.randomUUID();

            original.addAlly(owner1, ally1A);
            original.addAlly(owner1, ally1B);
            original.addAlly(owner2, ally2A);

            DataResult<JsonElement> encodeResult = TurretAllies.CODEC.encodeStart(JsonOps.INSTANCE, original);
            assertTrue(encodeResult.result().isPresent(), "encodeStart must succeed: " + encodeResult.error());
            JsonElement json = encodeResult.result().get();

            DataResult<TurretAllies> decodeResult = TurretAllies.CODEC.parse(JsonOps.INSTANCE, json);
            assertTrue(decodeResult.result().isPresent(), "parse must succeed: " + decodeResult.error());
            TurretAllies loaded = decodeResult.result().get();

            assertTrue(loaded.isAlly(owner1, ally1A));
            assertTrue(loaded.isAlly(owner1, ally1B));
            assertFalse(loaded.isAlly(owner1, ally2A));

            assertTrue(loaded.isAlly(owner2, ally2A));
            assertFalse(loaded.isAlly(owner2, ally1A));

            assertEquals(Set.of(ally1A, ally1B), loaded.getAllies(owner1));
            assertEquals(Set.of(ally2A), loaded.getAllies(owner2));
            assertEquals(Set.of(), loaded.getAllies(UUID.randomUUID()));
        }
    }
}
