package com.entropy.tacz_turrets;

import com.entropy.tacz_turrets.turret.TurretEntity;
import com.entropy.tacz_turrets.util.TurretEnergyStorage;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Turret Energy and Inventory Tests")
public class TurretEnergyAndInventoryTest {

    @Nested
    @DisplayName("TurretEnergyStorage Tests")
    class EnergyStorageTests {

        @Test
        @DisplayName("Initial state: getEnergyStored() == 0, getMaxEnergyStored() == capacity, canReceive() == true, canExtract() == false")
        void testInitialState() {
            TurretEnergyStorage storage = new TurretEnergyStorage(1000, 100);

            assertEquals(0, storage.getEnergyStored(), "Initial stored energy must be 0");
            assertEquals(1000, storage.getMaxEnergyStored(), "Max energy stored must equal configured capacity");
            assertTrue(storage.canReceive(), "canReceive() must be true when maxReceive > 0");
            assertFalse(storage.canExtract(), "canExtract() must be false for turret energy storage");
        }

        @Test
        @DisplayName("receiveEnergy with simulate = true does not mutate stored energy")
        void testReceiveEnergySimulate() {
            TurretEnergyStorage storage = new TurretEnergyStorage(1000, 100);

            int simulated = storage.receiveEnergy(50, true);
            assertEquals(50, simulated, "Should simulate receiving 50 energy");
            assertEquals(0, storage.getEnergyStored(), "Stored energy must not change when simulate is true");

            int simulatedOverLimit = storage.receiveEnergy(200, true);
            assertEquals(100, simulatedOverLimit, "Simulated transfer should respect maxReceive of 100");
            assertEquals(0, storage.getEnergyStored(), "Stored energy must still be 0");
        }

        @Test
        @DisplayName("receiveEnergy with simulate = false respects maxReceive and capacity ceiling")
        void testReceiveEnergyExecute() {
            TurretEnergyStorage storage = new TurretEnergyStorage(500, 100);

            int first = storage.receiveEnergy(60, false);
            assertEquals(60, first);
            assertEquals(60, storage.getEnergyStored());

            // Exceeds maxReceive per transfer (100)
            int second = storage.receiveEnergy(250, false);
            assertEquals(100, second, "Transfer must be capped at maxReceive (100)");
            assertEquals(160, storage.getEnergyStored());

            // Transfer near capacity ceiling
            storage.setEnergy(450);
            int cappedAtCeiling = storage.receiveEnergy(100, false);
            assertEquals(50, cappedAtCeiling, "Should only accept remaining capacity (500 - 450 = 50)");
            assertEquals(500, storage.getEnergyStored(), "Energy should now be at max capacity");

            // Transfer when full
            int whenFull = storage.receiveEnergy(50, false);
            assertEquals(0, whenFull, "Should accept 0 energy when full");
            assertEquals(500, storage.getEnergyStored());
        }

        @Test
        @DisplayName("consume reduces energy and clamps at 0")
        void testConsume() {
            TurretEnergyStorage storage = new TurretEnergyStorage(1000, 100);
            storage.setEnergy(200);

            storage.consume(75);
            assertEquals(125, storage.getEnergyStored());

            storage.consume(125);
            assertEquals(0, storage.getEnergyStored());

            // Consuming more than stored must clamp at 0
            storage.consume(50);
            assertEquals(0, storage.getEnergyStored(), "consume must clamp at 0");
        }

        @Test
        @DisplayName("setEnergy clamps between 0 and capacity")
        void testSetEnergy() {
            TurretEnergyStorage storage = new TurretEnergyStorage(1000, 100);

            storage.setEnergy(400);
            assertEquals(400, storage.getEnergyStored());

            // Clamping at upper bound (capacity)
            storage.setEnergy(1500);
            assertEquals(1000, storage.getEnergyStored(), "setEnergy must clamp to capacity");

            // Clamping at lower bound (0)
            storage.setEnergy(-200);
            assertEquals(0, storage.getEnergyStored(), "setEnergy must clamp to 0");
        }

        @Test
        @DisplayName("setLimits updates capacity and maxReceive and clamps existing energy if capacity shrinks")
        void testSetLimits() {
            TurretEnergyStorage storage = new TurretEnergyStorage(1000, 100);
            storage.setEnergy(800);

            // Shrink capacity below current amount
            storage.setLimits(500, 50);
            assertEquals(500, storage.getMaxEnergyStored(), "Capacity should be updated to 500");
            assertEquals(500, storage.getEnergyStored(), "Stored energy must clamp when capacity shrinks");

            // New maxReceive applies
            storage.setEnergy(0);
            int received = storage.receiveEnergy(100, false);
            assertEquals(50, received, "Transfer must respect new maxReceive of 50");
            assertEquals(50, storage.getEnergyStored());

            // Expand capacity
            storage.setLimits(2000, 200);
            assertEquals(2000, storage.getMaxEnergyStored(), "Capacity should expand to 2000");
            assertEquals(50, storage.getEnergyStored(), "Stored energy should remain unchanged on capacity expansion");

            // Disable receiving
            storage.setLimits(1000, 0);
            assertFalse(storage.canReceive(), "canReceive() must be false when maxReceive is 0");
            assertEquals(0, storage.receiveEnergy(100, false), "Should receive 0 when canReceive is false");
        }

        @Test
        @DisplayName("extractEnergy returns 0 because canExtract() is false")
        void testExtractEnergy() {
            TurretEnergyStorage storage = new TurretEnergyStorage(1000, 100);
            storage.setEnergy(500);

            assertEquals(0, storage.extractEnergy(100, true));
            assertEquals(0, storage.extractEnergy(100, false));
            assertEquals(500, storage.getEnergyStored(), "Stored energy must not decrease via extractEnergy");
        }
    }

    @Nested
    @DisplayName("TurretEntity.ItemStackHandler Tests")
    class ItemStackHandlerTests {

        @Test
        @DisplayName("Slot count initialization and initial empty slots")
        void testSlotCountInitialization() {
            TurretEntity.ItemStackHandler handler = new TurretEntity.ItemStackHandler(8);
            assertEquals(8, handler.getSlots(), "getSlots() should match constructor size");

            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                assertNotNull(stack, "Stack at slot " + i + " should not be null");
                assertTrue(stack.isEmpty(), "Stack at slot " + i + " should be empty");
            }

            assertEquals(64, handler.getSlotLimit(0), "Slot limit should be 64");
        }

        @Test
        @DisplayName("setSize() resizes while preserving existing slots and handles growing / shrinking")
        void testSetSizeResizing() {
            TurretEntity.ItemStackHandler handler = new TurretEntity.ItemStackHandler(6);
            assertEquals(6, handler.getSlots());

            // Expand
            handler.setSize(12);
            assertEquals(12, handler.getSlots(), "Handler should resize to 12 slots");
            for (int i = 0; i < 12; i++) {
                assertTrue(handler.getStackInSlot(i).isEmpty());
            }

            // Shrink
            handler.setSize(4);
            assertEquals(4, handler.getSlots(), "Handler should shrink to 4 slots");
            for (int i = 0; i < 4; i++) {
                assertTrue(handler.getStackInSlot(i).isEmpty());
            }

            // Same size is a no-op
            handler.setSize(4);
            assertEquals(4, handler.getSlots());
        }

        @Test
        @DisplayName("Out-of-bounds slot index validation throws RuntimeException")
        void testOutOfBoundsSlotIndexValidation() {
            TurretEntity.ItemStackHandler handler = new TurretEntity.ItemStackHandler(5);

            // Negative index
            assertThrows(RuntimeException.class, () -> handler.getStackInSlot(-1),
                    "getStackInSlot(-1) must throw RuntimeException");
            assertThrows(RuntimeException.class, () -> handler.setStackInSlot(-1, ItemStack.EMPTY),
                    "setStackInSlot(-1) must throw RuntimeException");
            assertThrows(RuntimeException.class, () -> handler.insertItem(-1, ItemStack.EMPTY, false),
                    "insertItem(-1) must throw RuntimeException");
            assertThrows(RuntimeException.class, () -> handler.extractItem(-1, 1, false),
                    "extractItem(-1) must throw RuntimeException");

            // Equal to size (out-of-bounds)
            assertThrows(RuntimeException.class, () -> handler.getStackInSlot(5),
                    "getStackInSlot(5) must throw RuntimeException for size 5");
            assertThrows(RuntimeException.class, () -> handler.setStackInSlot(5, ItemStack.EMPTY),
                    "setStackInSlot(5) must throw RuntimeException for size 5");
            assertThrows(RuntimeException.class, () -> handler.extractItem(5, 1, false),
                    "extractItem(5) must throw RuntimeException for size 5");

            // Greater than size
            assertThrows(RuntimeException.class, () -> handler.getStackInSlot(100),
                    "getStackInSlot(100) must throw RuntimeException");
            assertThrows(RuntimeException.class, () -> handler.setStackInSlot(100, ItemStack.EMPTY),
                    "setStackInSlot(100) must throw RuntimeException");
            assertThrows(RuntimeException.class, () -> handler.extractItem(100, 1, false),
                    "extractItem(100) must throw RuntimeException");
        }

        @Test
        @DisplayName("ItemStackHandler implements cn.sh1rocu.tacz.util.itemhandler.IItemHandler and satisfies contract")
        void testIItemHandlerContract() {
            TurretEntity.ItemStackHandler handler = new TurretEntity.ItemStackHandler(10);
            assertTrue(handler instanceof cn.sh1rocu.tacz.util.itemhandler.IItemHandler,
                    "TurretEntity.ItemStackHandler must implement cn.sh1rocu.tacz.util.itemhandler.IItemHandler");

            cn.sh1rocu.tacz.util.itemhandler.IItemHandler itemHandler = handler;
            assertEquals(10, itemHandler.getSlots());
            assertEquals(64, itemHandler.getSlotLimit(0));
            assertTrue(itemHandler.isItemValid(0, ItemStack.EMPTY));
        }
    }
}
