package com.entropy.tacz_turrets;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.entropy.tacz_turrets.config.TACZTurretsConfig;
import com.entropy.tacz_turrets.menu.TurretLayout;
import com.entropy.tacz_turrets.turret.HealthBarStyle;
import com.entropy.tacz_turrets.turret.InaccuracyMode;
import com.entropy.tacz_turrets.turret.PlayerTargeting;
import com.entropy.tacz_turrets.turret.RecoilType;
import com.entropy.tacz_turrets.turret.RetaliateTargeting;
import com.entropy.tacz_turrets.turret.TurretEnableType;
import com.entropy.tacz_turrets.turret.TurretMode;
import com.entropy.tacz_turrets.turret.TurretState;
import com.entropy.tacz_turrets.turret.TurretType;
import com.entropy.tacz_turrets.util.Enums;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.config.IConfigSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Turret Layout, Type Metadata, and Enums Tests")
public class TurretLayoutAndEnumsTest {

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
    @DisplayName("TurretLayout and TurretType Tests")
    class LayoutAndTypeTests {

        @Test
        @DisplayName("TurretLayout.forType() returns valid layouts for all 10 TurretType values")
        void testLayoutForAllTenTurretTypes() {
            TurretType[] types = TurretType.values();
            assertEquals(10, types.length, "Expected exactly 10 TurretType values");

            for (TurretType type : types) {
                TurretLayout layout = TurretLayout.forType(type);
                assertNotNull(layout, "Layout should not be null for " + type);

                assertTrue(layout.rows >= 1, "rows should be >= 1 for " + type);
                assertTrue(layout.columns >= 1, "columns should be >= 1 for " + type);
                assertEquals(layout.rows * layout.columns, layout.ammoSlots,
                        "ammoSlots must equal rows * columns for " + type);

                assertTrue(layout.width > 0, "width must be positive for " + type);
                assertTrue(layout.height > 0, "height must be positive for " + type);
                assertTrue(layout.barWidth > 0, "barWidth must be positive for " + type);
                assertTrue(layout.gunSlotX > 0, "gunSlotX must be positive for " + type);
                assertTrue(layout.laserSlotX > 0, "laserSlotX must be positive for " + type);

                assertEquals(layout.gunSlotX + TurretLayout.SLOT, layout.laserSlotX,
                        "laserSlotX should be gunSlotX + SLOT for " + type);
                assertEquals(layout.width - TurretLayout.MARGIN * 2, layout.barWidth,
                        "barWidth should equal width - MARGIN * 2 for " + type);

                for (int slot = 0; slot < layout.ammoSlots; slot++) {
                    int slotX = layout.ammoSlotX(slot);
                    int slotY = layout.ammoSlotY(slot);

                    assertTrue(slotX >= 0, "ammoSlotX(" + slot + ") >= 0 for " + type);
                    assertTrue(slotX + TurretLayout.SLOT <= layout.width,
                            "ammoSlotX(" + slot + ") + SLOT <= width for " + type);
                    assertTrue(slotY >= 0, "ammoSlotY(" + slot + ") >= 0 for " + type);
                    assertTrue(slotY + TurretLayout.SLOT <= layout.height,
                            "ammoSlotY(" + slot + ") + SLOT <= height for " + type);
                }
            }
        }

        @Test
        @DisplayName("TurretType metadata validation for all 10 types across all states")
        void testTurretTypeMetadata() {
            TurretType[] types = TurretType.values();
            assertEquals(10, types.length);

            TurretState[] allStates = TurretState.values();
            assertEquals(5, allStates.length);
            assertArrayEquals(new TurretState[]{
                    TurretState.ACTIVE, TurretState.RELOADING, TurretState.NO_AMMO,
                    TurretState.NO_GUN, TurretState.DISABLED
            }, allStates);

            for (TurretType type : types) {
                assertNotNull(type.id(), "id should not be null for " + type);
                assertFalse(type.id().isBlank(), "id should not be blank for " + type);
                assertEquals(type.name().toLowerCase(Locale.ROOT), type.id());

                assertTrue(type.defaultHealth > 0, "defaultHealth > 0 for " + type);
                assertTrue(type.defaultRange > 0, "defaultRange > 0 for " + type);
                assertTrue(type.defaultArmor >= 0, "defaultArmor >= 0 for " + type);
                assertTrue(type.defaultReloadSpeed > 0, "defaultReloadSpeed > 0 for " + type);
                assertTrue(type.defaultFireRate > 0, "defaultFireRate > 0 for " + type);
                assertTrue(type.defaultInaccuracy >= 0, "defaultInaccuracy >= 0 for " + type);

                Identifier model = type.model();
                assertNotNull(model, "model should not be null for " + type);
                assertTrue(model.getPath().startsWith("entity/turret_"), "model path starts with entity/turret_ for " + type);
                assertFalse(model.getPath().endsWith(".geo.json"), "model path must not end with .geo.json in GeckoLib 5.5.7 for " + type);

                Identifier paintMask = type.paintMask();
                assertNotNull(paintMask, "paintMask should not be null for " + type);
                assertTrue(paintMask.getPath().startsWith("textures/entity/"), "paintMask path starts with textures/entity/ for " + type);
                assertTrue(paintMask.getPath().endsWith("_paint.png"), "paintMask path ends with _paint.png for " + type);

                for (TurretState state : allStates) {
                    Identifier texture = type.texture(state);
                    assertNotNull(texture, "texture should not be null for state " + state + " of " + type);
                    assertTrue(texture.getPath().startsWith("textures/entity/"),
                            "texture path should start with textures/entity/ for state " + state + " of " + type);
                    assertTrue(texture.getPath().endsWith(".png"),
                            "texture path should end with .png for state " + state + " of " + type);
                    assertTrue(texture.getPath().contains(state.name().toLowerCase(Locale.ROOT)),
                            "texture path should contain state name for " + state + " of " + type);
                }
            }
        }

        @Test
        @DisplayName("TurretLayout constructor clamps invalid dimensions and computes consistent layout")
        void testLayoutClampingAndCustomDimensions() {
            TurretLayout clamped = new TurretLayout(0, -3);
            assertEquals(1, clamped.rows);
            assertEquals(1, clamped.columns);
            assertEquals(1, clamped.ammoSlots);
            assertTrue(clamped.width >= 176);
            assertTrue(clamped.height > 0);

            TurretLayout custom = new TurretLayout(3, 6);
            assertEquals(3, custom.rows);
            assertEquals(6, custom.columns);
            assertEquals(18, custom.ammoSlots);
            assertEquals(custom.ammoStartX + 0 * TurretLayout.SLOT, custom.ammoSlotX(0));
            assertEquals(custom.ammoStartX + 5 * TurretLayout.SLOT, custom.ammoSlotX(5));
            assertEquals(custom.ammoStartX + 0 * TurretLayout.SLOT, custom.ammoSlotX(6));
            assertEquals(custom.ammoStartY + 0 * TurretLayout.SLOT, custom.ammoSlotY(0));
            assertEquals(custom.ammoStartY + 1 * TurretLayout.SLOT, custom.ammoSlotY(6));
            assertEquals(custom.ammoStartY + 2 * TurretLayout.SLOT, custom.ammoSlotY(17));
        }
    }

    @Nested
    @DisplayName("Enums Utility and Enum Cycling Tests")
    class EnumCyclingTests {

        @Test
        @DisplayName("TurretMode cycling: AGGRESSIVE -> CONSERVATIVE -> ADAPTIVE -> AGGRESSIVE")
        void testTurretMode() {
            assertEquals(TurretMode.CONSERVATIVE, Enums.next(TurretMode.AGGRESSIVE));
            assertEquals(TurretMode.ADAPTIVE, Enums.next(TurretMode.CONSERVATIVE));
            assertEquals(TurretMode.AGGRESSIVE, Enums.next(TurretMode.ADAPTIVE));

            assertEquals(TurretMode.AGGRESSIVE, Enums.byOrdinal(TurretMode.class, 0));
            assertEquals(TurretMode.CONSERVATIVE, Enums.byOrdinal(TurretMode.class, 1));
            assertEquals(TurretMode.ADAPTIVE, Enums.byOrdinal(TurretMode.class, 2));
            assertEquals(TurretMode.AGGRESSIVE, Enums.byOrdinal(TurretMode.class, 3));
            assertEquals(TurretMode.ADAPTIVE, Enums.byOrdinal(TurretMode.class, -1));

            assertEquals(TurretMode.AGGRESSIVE, Enums.byName("AGGRESSIVE", TurretMode.CONSERVATIVE));
            assertEquals(TurretMode.CONSERVATIVE, Enums.byName("CONSERVATIVE", TurretMode.AGGRESSIVE));
            assertEquals(TurretMode.ADAPTIVE, Enums.byName("ADAPTIVE", TurretMode.AGGRESSIVE));
            assertEquals(TurretMode.CONSERVATIVE, Enums.byName("INVALID", TurretMode.CONSERVATIVE));
        }

        @Test
        @DisplayName("TurretEnableType cycling: ALWAYS_ON -> REDSTONE_ON -> REDSTONE_OFF -> ALWAYS_OFF -> ALWAYS_ON")
        void testTurretEnableType() {
            assertEquals(TurretEnableType.REDSTONE_ON, Enums.next(TurretEnableType.ALWAYS_ON));
            assertEquals(TurretEnableType.REDSTONE_OFF, Enums.next(TurretEnableType.REDSTONE_ON));
            assertEquals(TurretEnableType.ALWAYS_OFF, Enums.next(TurretEnableType.REDSTONE_OFF));
            assertEquals(TurretEnableType.ALWAYS_ON, Enums.next(TurretEnableType.ALWAYS_OFF));

            assertEquals(TurretEnableType.ALWAYS_ON, Enums.byOrdinal(TurretEnableType.class, 0));
            assertEquals(TurretEnableType.REDSTONE_ON, Enums.byOrdinal(TurretEnableType.class, 1));
            assertEquals(TurretEnableType.REDSTONE_OFF, Enums.byOrdinal(TurretEnableType.class, 2));
            assertEquals(TurretEnableType.ALWAYS_OFF, Enums.byOrdinal(TurretEnableType.class, 3));
            assertEquals(TurretEnableType.ALWAYS_ON, Enums.byOrdinal(TurretEnableType.class, 4));
            assertEquals(TurretEnableType.ALWAYS_OFF, Enums.byOrdinal(TurretEnableType.class, -1));

            assertEquals(TurretEnableType.ALWAYS_ON, Enums.byName("ALWAYS_ON", TurretEnableType.ALWAYS_OFF));
            assertEquals(TurretEnableType.REDSTONE_ON, Enums.byName("REDSTONE_ON", TurretEnableType.ALWAYS_ON));
            assertEquals(TurretEnableType.REDSTONE_OFF, Enums.byName("REDSTONE_OFF", TurretEnableType.ALWAYS_ON));
            assertEquals(TurretEnableType.ALWAYS_OFF, Enums.byName("ALWAYS_OFF", TurretEnableType.ALWAYS_ON));
            assertEquals(TurretEnableType.ALWAYS_ON, Enums.byName("NON_EXISTENT", TurretEnableType.ALWAYS_ON));
        }

        @Test
        @DisplayName("PlayerTargeting cycling: RETALIATE -> ALL -> NEVER -> RETALIATE")
        void testPlayerTargeting() {
            assertEquals(PlayerTargeting.ALL, Enums.next(PlayerTargeting.RETALIATE));
            assertEquals(PlayerTargeting.NEVER, Enums.next(PlayerTargeting.ALL));
            assertEquals(PlayerTargeting.RETALIATE, Enums.next(PlayerTargeting.NEVER));

            assertEquals(PlayerTargeting.NEVER, Enums.byOrdinal(PlayerTargeting.class, 0));
            assertEquals(PlayerTargeting.RETALIATE, Enums.byOrdinal(PlayerTargeting.class, 1));
            assertEquals(PlayerTargeting.ALL, Enums.byOrdinal(PlayerTargeting.class, 2));
            assertEquals(PlayerTargeting.NEVER, Enums.byOrdinal(PlayerTargeting.class, 3));
            assertEquals(PlayerTargeting.ALL, Enums.byOrdinal(PlayerTargeting.class, -1));

            assertEquals(PlayerTargeting.NEVER, Enums.byName("NEVER", PlayerTargeting.ALL));
            assertEquals(PlayerTargeting.RETALIATE, Enums.byName("RETALIATE", PlayerTargeting.NEVER));
            assertEquals(PlayerTargeting.ALL, Enums.byName("ALL", PlayerTargeting.NEVER));
            assertEquals(PlayerTargeting.NEVER, Enums.byName("UNKNOWN", PlayerTargeting.NEVER));
        }

        @Test
        @DisplayName("RetaliateTargeting cycling and lookup")
        void testRetaliateTargeting() {
            assertEquals(RetaliateTargeting.CLEAR_ON_DEATH, Enums.next(RetaliateTargeting.CONTINUE_TARGETING));
            assertEquals(RetaliateTargeting.CONTINUE_TARGETING, Enums.next(RetaliateTargeting.CLEAR_ON_DEATH));

            assertEquals(RetaliateTargeting.CONTINUE_TARGETING, Enums.byOrdinal(RetaliateTargeting.class, 0));
            assertEquals(RetaliateTargeting.CLEAR_ON_DEATH, Enums.byOrdinal(RetaliateTargeting.class, 1));
            assertEquals(RetaliateTargeting.CONTINUE_TARGETING, Enums.byOrdinal(RetaliateTargeting.class, 2));
            assertEquals(RetaliateTargeting.CLEAR_ON_DEATH, Enums.byOrdinal(RetaliateTargeting.class, -1));

            assertEquals(RetaliateTargeting.CONTINUE_TARGETING,
                    Enums.byName("CONTINUE_TARGETING", RetaliateTargeting.CLEAR_ON_DEATH));
            assertEquals(RetaliateTargeting.CLEAR_ON_DEATH,
                    Enums.byName("CLEAR_ON_DEATH", RetaliateTargeting.CONTINUE_TARGETING));
            assertEquals(RetaliateTargeting.CONTINUE_TARGETING,
                    Enums.byName("UNKNOWN", RetaliateTargeting.CONTINUE_TARGETING));
        }

        @Test
        @DisplayName("RecoilType cycling and lookup")
        void testRecoilType() {
            assertEquals(RecoilType.PUSH, Enums.next(RecoilType.BOUNCE));
            assertEquals(RecoilType.BOUNCE, Enums.next(RecoilType.PUSH));

            assertEquals(RecoilType.BOUNCE, Enums.byOrdinal(RecoilType.class, 0));
            assertEquals(RecoilType.PUSH, Enums.byOrdinal(RecoilType.class, 1));
            assertEquals(RecoilType.BOUNCE, Enums.byOrdinal(RecoilType.class, 2));
            assertEquals(RecoilType.PUSH, Enums.byOrdinal(RecoilType.class, -1));

            assertEquals(RecoilType.BOUNCE, Enums.byName("BOUNCE", RecoilType.PUSH));
            assertEquals(RecoilType.PUSH, Enums.byName("PUSH", RecoilType.BOUNCE));
            assertEquals(RecoilType.PUSH, Enums.byName("NONE", RecoilType.PUSH));
        }

        @Test
        @DisplayName("InaccuracyMode cycling and lookup")
        void testInaccuracyMode() {
            assertEquals(InaccuracyMode.RANDOM, Enums.next(InaccuracyMode.DISTANCE));
            assertEquals(InaccuracyMode.DISTANCE, Enums.next(InaccuracyMode.RANDOM));

            assertEquals(InaccuracyMode.DISTANCE, Enums.byOrdinal(InaccuracyMode.class, 0));
            assertEquals(InaccuracyMode.RANDOM, Enums.byOrdinal(InaccuracyMode.class, 1));
            assertEquals(InaccuracyMode.DISTANCE, Enums.byOrdinal(InaccuracyMode.class, 2));
            assertEquals(InaccuracyMode.RANDOM, Enums.byOrdinal(InaccuracyMode.class, -1));

            assertEquals(InaccuracyMode.DISTANCE, Enums.byName("DISTANCE", InaccuracyMode.RANDOM));
            assertEquals(InaccuracyMode.RANDOM, Enums.byName("RANDOM", InaccuracyMode.DISTANCE));
            assertEquals(InaccuracyMode.DISTANCE, Enums.byName("OTHER", InaccuracyMode.DISTANCE));
        }

        @Test
        @DisplayName("HealthBarStyle cycling and lookup")
        void testHealthBarStyle() {
            assertEquals(HealthBarStyle.GREEN_TO_RED, Enums.next(HealthBarStyle.COLOR));
            assertEquals(HealthBarStyle.COLOR, Enums.next(HealthBarStyle.GREEN_TO_RED));

            assertEquals(HealthBarStyle.COLOR, Enums.byOrdinal(HealthBarStyle.class, 0));
            assertEquals(HealthBarStyle.GREEN_TO_RED, Enums.byOrdinal(HealthBarStyle.class, 1));
            assertEquals(HealthBarStyle.COLOR, Enums.byOrdinal(HealthBarStyle.class, 2));
            assertEquals(HealthBarStyle.GREEN_TO_RED, Enums.byOrdinal(HealthBarStyle.class, -1));

            assertEquals(HealthBarStyle.COLOR, Enums.byName("COLOR", HealthBarStyle.GREEN_TO_RED));
            assertEquals(HealthBarStyle.GREEN_TO_RED, Enums.byName("GREEN_TO_RED", HealthBarStyle.COLOR));
            assertEquals(HealthBarStyle.GREEN_TO_RED, Enums.byName("MISSING", HealthBarStyle.GREEN_TO_RED));
        }
    }
}
