package com.entropy.tacz_turrets.testmod;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.config.TACZTurretsConfig;
import com.entropy.tacz_turrets.config.TurretEnabledCondition;
import com.entropy.tacz_turrets.item.TurretItem;
import com.entropy.tacz_turrets.menu.TurretContainer;
import com.entropy.tacz_turrets.menu.TurretLayout;
import com.entropy.tacz_turrets.registry.CreativeTabRegistry;
import com.entropy.tacz_turrets.registry.EntityTypeRegistry;
import com.entropy.tacz_turrets.registry.ItemRegistry;
import com.entropy.tacz_turrets.registry.MenuRegistry;
import com.entropy.tacz_turrets.registry.SoundRegistry;
import com.entropy.tacz_turrets.turret.PlayerTargeting;
import com.entropy.tacz_turrets.turret.SentryLoadout;
import com.entropy.tacz_turrets.turret.TurretEnableType;
import com.entropy.tacz_turrets.turret.TurretEntity;
import com.entropy.tacz_turrets.turret.TurretMode;
import com.entropy.tacz_turrets.turret.TurretState;
import com.entropy.tacz_turrets.turret.TurretType;
import com.entropy.tacz_turrets.util.PlacedTurrets;
import com.entropy.tacz_turrets.util.TurretAllies;
import com.entropy.tacz_turrets.util.TurretEnergyStorage;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.tslat.smartbrainlib.util.BrainUtil;
import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class TACZTurretsPreProdTestMod implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("TACZTurretsPreProdTestMod");
    private int ticks = 0;
    private boolean executed = false;

    @Override
    public void onInitialize() {
        LOGGER.info("[TACZ-Turrets-TestMod] Pre-prod server test mod initialized. Waiting for server tick...");
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        if (executed) {
            return;
        }
        ticks++;
        ServerLevel level = server.overworld();
        if (ticks == 1) {
            level.setChunkForced(0, 0, true);
            level.setChunkForced(1, 1, true);
            return;
        }
        if (ticks < 40) {
            return;
        }
        executed = true;
        int passed = 0;
        int total = 8;

        try {
            LOGGER.info("=== STARTING TACZ TURRETS 26.3 PRE-PROD SERVER TESTS (8 CATEGORIES) ===");

            // =========================================================================
            // Cat 1: Cat1_MixinAuditAndRegistries
            // =========================================================================
            LOGGER.info("[Cat 1/8: MixinAuditAndRegistries] Auditing mixins and validating all registries...");
            MixinEnvironment.getCurrentEnvironment().audit();

            assertCondition(BuiltInRegistries.ENTITY_TYPE.containsKey(TACZTurrets.id("turret")), "TURRET entity type not registered");
            assertCondition(BuiltInRegistries.ENTITY_TYPE.containsKey(TACZTurrets.id("sentry_turret")), "SENTRY entity type not registered");
            assertCondition(EntityTypeRegistry.TURRET.get() != null, "EntityTypeRegistry.TURRET returned null");
            assertCondition(EntityTypeRegistry.SENTRY.get() != null, "EntityTypeRegistry.SENTRY returned null");

            for (TurretType type : TurretType.values()) {
                assertCondition(ItemRegistry.turret(type) != null, "ItemRegistry.turret for " + type + " is null");
                assertCondition(BuiltInRegistries.ITEM.containsKey(TACZTurrets.id(type.id() + "_turret")), "Turret item for " + type + " not registered in ITEM");
            }

            for (String camo : ItemRegistry.CAMOS) {
                Identifier camoId = TACZTurrets.id(camo + "_camo");
                assertCondition(BuiltInRegistries.ITEM.containsKey(camoId), "Camo item " + camoId + " not registered in ITEM");
            }

            assertCondition(ItemRegistry.SENTRY_SPAWN_EGG.get() != null, "ItemRegistry.SENTRY_SPAWN_EGG is null");
            assertCondition(BuiltInRegistries.ITEM.containsKey(TACZTurrets.id("sentry_turret_spawn_egg")), "sentry_turret_spawn_egg not registered in ITEM");

            assertCondition(CreativeTabRegistry.TURRETS.get() != null, "CreativeTabRegistry.TURRETS is null");
            assertCondition(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(TACZTurrets.id(TACZTurrets.MODID)), "Creative mode tab not registered");

            assertCondition(MenuRegistry.TURRET.get() != null, "MenuRegistry.TURRET is null");
            assertCondition(BuiltInRegistries.MENU.containsKey(TACZTurrets.id("turret")), "turret menu not registered");

            assertCondition(SoundRegistry.TURRET_PLACE.get() != null && BuiltInRegistries.SOUND_EVENT.containsKey(TACZTurrets.id("turret_place")), "TURRET_PLACE not registered");
            assertCondition(SoundRegistry.TURRET_PICKUP.get() != null && BuiltInRegistries.SOUND_EVENT.containsKey(TACZTurrets.id("turret_pickup")), "TURRET_PICKUP not registered");
            assertCondition(SoundRegistry.TURRET_HURT.get() != null && BuiltInRegistries.SOUND_EVENT.containsKey(TACZTurrets.id("turret_hurt")), "TURRET_HURT not registered");
            assertCondition(SoundRegistry.TURRET_LOCK_ON.get() != null && BuiltInRegistries.SOUND_EVENT.containsKey(TACZTurrets.id("turret_lock_on")), "TURRET_LOCK_ON not registered");
            assertCondition(SoundRegistry.TURRET_PAN.get() != null && BuiltInRegistries.SOUND_EVENT.containsKey(TACZTurrets.id("turret_pan")), "TURRET_PAN not registered");

            passed++;
            LOGGER.info("[Cat 1/8: MixinAuditAndRegistries] PASS: Mixins audited cleanly and all entities/items/sounds/menus registered.");

            // =========================================================================
            // Cat 2: Cat2_ConfigsAndConditions
            // =========================================================================
            LOGGER.info("[Cat 2/8: ConfigsAndConditions] Verifying config files, standard enabled types, brass availability, and conditions...");
            Path configDir = FabricLoader.getInstance().getConfigDir();
            Path clientConfig = configDir.resolve("tacz_turrets-client.toml");
            Path commonConfig = configDir.resolve("tacz_turrets-common.toml");
            Path serverConfig1 = configDir.resolve("tacz_turrets-server.toml");
            Path serverConfig2 = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/tacz_turrets-server.toml");

            assertCondition(Files.exists(clientConfig) || TACZTurretsConfig.CLIENT_SPEC.isLoaded(), "tacz_turrets-client.toml missing in " + configDir);
            assertCondition(Files.exists(commonConfig), "tacz_turrets-common.toml missing in " + configDir);
            assertCondition(Files.exists(serverConfig1) || Files.exists(serverConfig2), "tacz_turrets-server.toml missing in config/ or world serverconfig/");

            assertCondition(TACZTurretsConfig.enabled(TurretType.MODERN), "TurretType.MODERN must be enabled");
            assertCondition(TACZTurretsConfig.enabled(TurretType.INDUSTRIAL), "TurretType.INDUSTRIAL must be enabled");
            assertCondition(TACZTurretsConfig.enabled(TurretType.SCRAP), "TurretType.SCRAP must be enabled");
            assertCondition(TACZTurretsConfig.enabled(TurretType.TACTICAL), "TurretType.TACTICAL must be enabled");
            assertCondition(!TurretType.BRASS.isAvailable(), "TurretType.BRASS must not be available when Create is absent");

            TurretEnabledCondition condModern = new TurretEnabledCondition(TurretType.MODERN, Optional.of(false));
            assertCondition(condModern.test(null), "TurretEnabledCondition for MODERN must evaluate true");

            TurretEnabledCondition condBrass = new TurretEnabledCondition(TurretType.BRASS, Optional.empty());
            assertCondition(!condBrass.test(null), "TurretEnabledCondition for BRASS must evaluate false when Create is absent");

            DataResult<JsonElement> encoded = TurretEnabledCondition.CODEC.codec().encodeStart(JsonOps.INSTANCE, condModern);
            assertCondition(encoded.result().isPresent(), "TurretEnabledCondition encodeStart failed: " + encoded.error().map(DataResult.Error::message).orElse(""));
            DataResult<Pair<TurretEnabledCondition, JsonElement>> decoded = TurretEnabledCondition.CODEC.codec().decode(JsonOps.INSTANCE, encoded.result().get());
            assertCondition(decoded.result().isPresent(), "TurretEnabledCondition decode failed: " + decoded.error().map(DataResult.Error::message).orElse(""));
            assertCondition(condModern.equals(decoded.result().get().getFirst()), "TurretEnabledCondition codec round-trip value mismatch");

            passed++;
            LOGGER.info("[Cat 2/8: ConfigsAndConditions] PASS: Config files, standard turret types, brass condition, and codec round-trip verified.");

            // =========================================================================
            // Cat 3: Cat3_TurretSpawningMountingAndLimits
            // =========================================================================
            LOGGER.info("[Cat 3/8: TurretSpawningMountingAndLimits] Building arena, spawning floor/ceiling/wall turrets, and verifying PlacedTurrets limits...");
            BlockPos center = new BlockPos(0, 100, 0);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    level.setBlockAndUpdate(center.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(center.offset(dx, 4, dz), Blocks.STONE.defaultBlockState());
                    for (int dy = 0; dy <= 3; dy++) {
                        level.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                    }
                }
            }
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = 0; dy <= 3; dy++) {
                    level.setBlockAndUpdate(center.offset(dx, dy, -3), Blocks.STONE.defaultBlockState());
                }
            }

            UUID playerUuid = UUID.randomUUID();
            PlacedTurrets placedTurrets = PlacedTurrets.get(server);
            assertCondition(placedTurrets.count(playerUuid) == 0, "Initial player turret count must be 0");

            // Floor mount: Direction.DOWN -> isNoGravity() == false
            TurretEntity turretFloor = new TurretEntity(EntityTypeRegistry.TURRET.get(), level);
            turretFloor.owner = playerUuid;
            turretFloor.setTurretType(TurretType.MODERN);
            turretFloor.setPos(0.5, 100.0, 0.5);
            turretFloor.setAttachFace(Direction.DOWN);
            assertCondition(!turretFloor.isNoGravity(), "Floor mounted turret must have gravity (isNoGravity() == false)");
            level.addFreshEntity(turretFloor);
            turretFloor.trackPlacement();
            assertCondition(placedTurrets.count(playerUuid) == 1, "Player turret count must be 1 after floor turret placed");

            // Ceiling mount: Direction.UP -> isNoGravity() == true
            TurretEntity turretCeiling = new TurretEntity(EntityTypeRegistry.TURRET.get(), level);
            turretCeiling.owner = playerUuid;
            turretCeiling.setTurretType(TurretType.INDUSTRIAL);
            turretCeiling.setPos(0.5, 103.0, 0.5);
            turretCeiling.setAttachFace(Direction.UP);
            assertCondition(turretCeiling.isNoGravity(), "Ceiling mounted turret must have no gravity (isNoGravity() == true)");
            level.addFreshEntity(turretCeiling);
            turretCeiling.trackPlacement();
            assertCondition(placedTurrets.count(playerUuid) == 2, "Player turret count must be 2 after ceiling turret placed");

            // Wall mount: Direction.NORTH -> isNoGravity() == true
            TurretEntity turretWall = new TurretEntity(EntityTypeRegistry.TURRET.get(), level);
            turretWall.owner = playerUuid;
            turretWall.setTurretType(TurretType.TACTICAL);
            turretWall.setPos(0.5, 101.0, -2.5);
            turretWall.setAttachFace(Direction.NORTH);
            assertCondition(turretWall.isNoGravity(), "Wall mounted turret must have no gravity (isNoGravity() == true)");
            level.addFreshEntity(turretWall);
            turretWall.trackPlacement();
            assertCondition(placedTurrets.count(playerUuid) == 3, "Player turret count must be 3 after wall turret placed");

            // Decrement tracking on removal
            turretFloor.remove(Entity.RemovalReason.KILLED);
            assertCondition(placedTurrets.count(playerUuid) == 2, "Player turret count must decrement to 2 after killing floor turret");

            turretCeiling.remove(Entity.RemovalReason.KILLED);
            assertCondition(placedTurrets.count(playerUuid) == 1, "Player turret count must decrement to 1 after killing ceiling turret");

            turretWall.remove(Entity.RemovalReason.KILLED);
            assertCondition(placedTurrets.count(playerUuid) == 0, "Player turret count must decrement to 0 after killing wall turret");

            passed++;
            LOGGER.info("[Cat 3/8: TurretSpawningMountingAndLimits] PASS: Floor/ceiling/wall attachments, gravity states, and PlacedTurrets tracking verified.");

            // =========================================================================
            // Cat 4: Cat4_PaintCamoAndBanners
            // =========================================================================
            LOGGER.info("[Cat 4/8: PaintCamoAndBanners] Applying dye, banner, and camo to turret and testing paint persistence...");
            ItemStack redDye = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("red_dye")));
            ItemStack blueBanner = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("blue_banner")));
            Item forestCamoItem = BuiltInRegistries.ITEM.getValue(TACZTurrets.id("forest_camo"));
            assertCondition(forestCamoItem != null, "forest_camo item not found in ITEM registry");
            ItemStack forestCamo = new ItemStack(forestCamoItem);

            assertCondition(TurretEntity.isPaint(redDye), "Items.RED_DYE must be recognized by isPaint");
            assertCondition(TurretEntity.isPaint(blueBanner), "Items.BLUE_BANNER must be recognized by isPaint");
            assertCondition(TurretEntity.isPaint(forestCamo), "forest_camo must be recognized by isPaint");

            TurretEntity paintTurret = new TurretEntity(EntityTypeRegistry.TURRET.get(), level);
            paintTurret.setTurretType(TurretType.MODERN);
            paintTurret.setPos(0.5, 100.0, 0.5);

            paintTurret.setPaint(redDye);
            assertCondition(ItemStack.isSameItemSameComponents(paintTurret.getPaint(), redDye), "getPaint() did not match red dye");
            ItemStack itemDye = paintTurret.toItem();
            assertCondition(!itemDye.isEmpty() && itemDye.getItem() instanceof TurretItem, "toItem() with dye failed");

            paintTurret.setPaint(blueBanner);
            assertCondition(ItemStack.isSameItemSameComponents(paintTurret.getPaint(), blueBanner), "getPaint() did not match blue banner");
            ItemStack itemBanner = paintTurret.toItem();
            assertCondition(!itemBanner.isEmpty() && itemBanner.getItem() instanceof TurretItem, "toItem() with banner failed");

            paintTurret.setPaint(forestCamo);
            assertCondition(ItemStack.isSameItemSameComponents(paintTurret.getPaint(), forestCamo), "getPaint() did not match forest camo");
            ItemStack itemCamo = paintTurret.toItem();
            assertCondition(!itemCamo.isEmpty() && itemCamo.getItem() instanceof TurretItem, "toItem() with camo failed");

            paintTurret.discard();
            passed++;
            LOGGER.info("[Cat 4/8: PaintCamoAndBanners] PASS: Dye, banner, and camo paints applied and verified with toItem().");

            // =========================================================================
            // Cat 5: Cat5_AlliesAndCommands
            // =========================================================================
            LOGGER.info("[Cat 5/8: AlliesAndCommands] Verifying TurretAllies storage and /turret command registrations...");
            TurretAllies allies = TurretAllies.get(server);
            UUID owner5 = UUID.randomUUID();
            UUID ally5 = UUID.randomUUID();

            assertCondition(!allies.isAlly(owner5, ally5), "ally5 should not be an ally initially");
            assertCondition(allies.addAlly(owner5, ally5), "addAlly must return true on new ally");
            assertCondition(allies.isAlly(owner5, ally5), "isAlly must return true after addAlly");

            allies.toggleAlly(owner5, ally5);
            assertCondition(!allies.isAlly(owner5, ally5), "isAlly must return false after toggleAlly (toggling off)");

            allies.toggleAlly(owner5, ally5);
            assertCondition(allies.isAlly(owner5, ally5), "isAlly must return true after toggleAlly (toggling on)");

            assertCondition(allies.removeAlly(owner5, ally5), "removeAlly must return true");
            assertCondition(!allies.isAlly(owner5, ally5), "isAlly must return false after removeAlly");

            var turretCommandNode = server.getCommands().getDispatcher().getRoot().getChild("turret");
            assertCondition(turretCommandNode != null, "/turret command node not registered in command dispatcher");
            assertCondition(turretCommandNode.getChild("trust") != null, "/turret trust sub-command not registered");
            assertCondition(turretCommandNode.getChild("untrust") != null, "/turret untrust sub-command not registered");
            assertCondition(turretCommandNode.getChild("trusted") != null, "/turret trusted sub-command not registered");

            passed++;
            LOGGER.info("[Cat 5/8: AlliesAndCommands] PASS: TurretAllies methods and /turret trust/untrust/trusted commands verified.");

            // =========================================================================
            // Cat 6: Cat6_ModesRedstoneEnergyAndMenu
            // =========================================================================
            LOGGER.info("[Cat 6/8: ModesRedstoneEnergyAndMenu] Testing TurretEnableType, Redstone response, TurretMode, PlayerTargeting, energy and container layouts...");
            TurretEntity turret6 = new TurretEntity(EntityTypeRegistry.TURRET.get(), level);
            turret6.setTurretType(TurretType.MODERN);
            turret6.setPos(0.5, 100.0, 0.5);
            level.addFreshEntity(turret6);

            // ALWAYS_ON
            turret6.setEnableType(TurretEnableType.ALWAYS_ON);
            turret6.tick();
            assertCondition(turret6.isEnabled(), "ALWAYS_ON turret must be enabled");

            // ALWAYS_OFF
            turret6.setEnableType(TurretEnableType.ALWAYS_OFF);
            turret6.tick();
            assertCondition(!turret6.isEnabled(), "ALWAYS_OFF turret must be disabled");
            assertCondition(TurretState.getState(turret6) == TurretState.DISABLED, "Ticked state must transition to DISABLED under ALWAYS_OFF");

            // REDSTONE_ON: disabled without redstone, enabled with adjacent redstone block
            BlockPos redstonePos = turret6.blockPosition().east();
            level.setBlockAndUpdate(redstonePos, Blocks.AIR.defaultBlockState());
            turret6.setEnableType(TurretEnableType.REDSTONE_ON);
            turret6.tick();
            assertCondition(!turret6.isEnabled(), "REDSTONE_ON must be disabled without redstone block");

            level.setBlockAndUpdate(redstonePos, Blocks.REDSTONE_BLOCK.defaultBlockState());
            turret6.tick();
            assertCondition(turret6.isEnabled(), "REDSTONE_ON must be enabled when adjacent redstone block is present");

            // REDSTONE_OFF: disabled with adjacent redstone block, enabled when removed
            turret6.setEnableType(TurretEnableType.REDSTONE_OFF);
            turret6.tick();
            assertCondition(!turret6.isEnabled(), "REDSTONE_OFF must be disabled when adjacent redstone block is present");

            level.setBlockAndUpdate(redstonePos, Blocks.AIR.defaultBlockState());
            turret6.tick();
            assertCondition(turret6.isEnabled(), "REDSTONE_OFF must be enabled when adjacent redstone block is removed");

            turret6.setEnableType(TurretEnableType.ALWAYS_ON);

            // Modes & PlayerTargeting
            turret6.setMode(TurretMode.AGGRESSIVE);
            assertCondition(turret6.getMode() == TurretMode.AGGRESSIVE, "Mode must be AGGRESSIVE");
            turret6.setMode(TurretMode.CONSERVATIVE);
            assertCondition(turret6.getMode() == TurretMode.CONSERVATIVE, "Mode must be CONSERVATIVE");
            turret6.setMode(TurretMode.ADAPTIVE);
            assertCondition(turret6.getMode() == TurretMode.ADAPTIVE, "Mode must be ADAPTIVE");

            turret6.setPlayerTargeting(PlayerTargeting.RETALIATE);
            assertCondition(turret6.getPlayerTargeting() == PlayerTargeting.RETALIATE, "PlayerTargeting must be RETALIATE");
            turret6.setPlayerTargeting(PlayerTargeting.ALL);
            assertCondition(turret6.getPlayerTargeting() == PlayerTargeting.ALL, "PlayerTargeting must be ALL");
            turret6.setPlayerTargeting(PlayerTargeting.NEVER);
            assertCondition(turret6.getPlayerTargeting() == PlayerTargeting.NEVER, "PlayerTargeting must be NEVER");

            // TurretEnergyStorage
            TurretEnergyStorage energyStorage = new TurretEnergyStorage(10000, 200);
            assertCondition(energyStorage.getMaxEnergyStored() == 10000, "Max energy should be 10000");
            assertCondition(energyStorage.getEnergyStored() == 0, "Energy stored should be 0 initially");
            int receivedEnergy = energyStorage.receiveEnergy(500, false);
            assertCondition(receivedEnergy == 200, "Received energy must be capped at maxReceive (200)");
            assertCondition(energyStorage.getEnergyStored() == 200, "Energy stored must now be 200");
            energyStorage.consume(80);
            assertCondition(energyStorage.getEnergyStored() == 120, "Energy stored must be 120 after consuming 80");

            // TurretContainer & TurretLayout across all 10 TurretTypes
            for (TurretType type : TurretType.values()) {
                TurretLayout layout = TurretLayout.forType(type);
                assertCondition(layout.rows >= 1 && layout.columns >= 1, "Layout dimensions invalid for " + type);
                assertCondition(layout.ammoSlots == layout.rows * layout.columns, "ammoSlots calculation mismatch for " + type);

                TurretEntity layoutTurret = new TurretEntity(EntityTypeRegistry.TURRET.get(), level);
                layoutTurret.setTurretType(type);
                TurretContainer container = new TurretContainer(layoutTurret, layout.ammoSlots);
                assertCondition(container.getContainerSize() == layout.ammoSlots + TurretContainer.FIRST_AMMO_SLOT, "Container size mismatch for " + type);
                layoutTurret.discard();
            }

            turret6.discard();
            passed++;
            LOGGER.info("[Cat 6/8: ModesRedstoneEnergyAndMenu] PASS: Redstone enable states, targeting modes, energy storage, and layouts verified.");

            // =========================================================================
            // Cat 7: Cat7_TaCZGunInventoryAndAmmoCollection
            // =========================================================================
            LOGGER.info("[Cat 7/8: TaCZGunInventoryAndAmmoCollection] Equipping TaCZ AK47, verifying ACTIVE state, and pulling ammo from Chest below...");
            TurretEntity turret7 = new TurretEntity(EntityTypeRegistry.TURRET.get(), level);
            turret7.setTurretType(TurretType.MODERN);
            turret7.setPos(0.5, 100.0, 0.5);
            turret7.setAttachFace(Direction.DOWN);
            level.addFreshEntity(turret7);

            ItemStack ak47 = GunItemBuilder.create()
                    .setId(Identifier.fromNamespaceAndPath("tacz", "ak47"))
                    .setAmmoCount(30)
                    .setFireMode(FireMode.AUTO)
                    .build();
            turret7.setGunStack(ak47);
            assertCondition(turret7.hasGun(), "turret.hasGun() must be true after setGunStack");

            turret7.tick();
            assertCondition(TurretState.getState(turret7) == TurretState.ACTIVE,
                    "TurretState must transition to ACTIVE after equipping loaded gun and ticking (was " + TurretState.getState(turret7) + ")");

            BlockPos chestPos = turret7.blockPosition().below();
            level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
            BlockEntity chestBe = level.getBlockEntity(chestPos);
            assertCondition(chestBe instanceof Container, "BlockEntity below turret must be a Container (Chest)");
            Container chest = (Container) chestBe;

            ItemStack ammo762 = AmmoItemBuilder.create()
                    .setId(Identifier.fromNamespaceAndPath("tacz", "762x39"))
                    .setCount(60)
                    .build();
            chest.setItem(0, ammo762);
            assertCondition(!chest.getItem(0).isEmpty(), "Chest slot 0 must contain 762x39 ammo");

            turret7.collectAmmo();

            boolean pulledAmmo = false;
            for (int slot = 0; slot < turret7.getInventory().getSlots(); slot++) {
                ItemStack slotStack = turret7.getInventory().getStackInSlot(slot);
                if (!slotStack.isEmpty()) {
                    pulledAmmo = true;
                    break;
                }
            }
            assertCondition(pulledAmmo, "Ammo must be pulled from Chest into turret.getInventory()");

            turret7.discard();
            level.setBlockAndUpdate(chestPos, Blocks.AIR.defaultBlockState());
            passed++;
            LOGGER.info("[Cat 7/8: TaCZGunInventoryAndAmmoCollection] PASS: TaCZ AK47 gun equipped, ACTIVE state reached, and ammo collected from Chest.");

            // =========================================================================
            // Cat 8: Cat8_SentryLoadoutAndCombatAI
            // =========================================================================
            LOGGER.info("[Cat 8/8: SentryLoadoutAndCombatAI] Spawning Sentry with loadout, and testing Combat AI vs Hostile Zombie & Owner/Ally protection...");
            TurretEntity sentry = EntityTypeRegistry.SENTRY.get().create(level, EntitySpawnReason.EVENT);
            assertCondition(sentry != null, "Failed to instantiate Sentry entity");
            sentry.setPos(0.5, 100.0, 0.5);
            assertCondition(sentry.isSentry(), "sentry.isSentry() must be true");

            Optional<SentryLoadout> defaultLoadout = SentryLoadout.get(SentryLoadout.DEFAULT);
            assertCondition(defaultLoadout.isPresent(), "SentryLoadout.get(SentryLoadout.DEFAULT) must be loaded from datapack");

            sentry.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(0, 100, 0)), EntitySpawnReason.EVENT, null);
            assertCondition(sentry.hasGun(), "Sentry must be equipped with a TaCZ gun from loadout");
            sentry.discard();

            // Combat Turret vs Hostile Zombie and Owner/Ally Protection
            UUID ownerId = UUID.randomUUID();
            UUID allyId = UUID.randomUUID();
            TurretAllies.get(server).addAlly(ownerId, allyId);

            TurretEntity combatTurret = new TurretEntity(EntityTypeRegistry.TURRET.get(), level);
            combatTurret.owner = ownerId;
            combatTurret.setTurretType(TurretType.MODERN);
            combatTurret.setPos(0.5, 100.0, 0.5);
            combatTurret.setAttachFace(Direction.DOWN);
            combatTurret.setNoGravity(true);
            level.addFreshEntity(combatTurret);

            ItemStack combatGun = GunItemBuilder.create()
                    .setId(Identifier.fromNamespaceAndPath("tacz", "ak47"))
                    .setAmmoCount(30)
                    .setFireMode(FireMode.AUTO)
                    .build();
            combatTurret.setGunStack(combatGun);
            combatTurret.tick();
            assertCondition(TurretState.getState(combatTurret) == TurretState.ACTIVE, "Combat turret must be ACTIVE");

            Zombie hostileZombie = new Zombie(level);
            hostileZombie.setPos(6.5, 100.0, 0.5);
            hostileZombie.setNoGravity(true);
            level.addFreshEntity(hostileZombie);

            assertCondition(combatTurret.isValidTarget(hostileZombie), "turret.isValidTarget(hostileZombie) must be true");
            assertCondition(combatTurret.canDamage(hostileZombie), "turret.canDamage(hostileZombie) must be true");
            assertCondition(!combatTurret.isProtectedFromFire(hostileZombie), "Hostile zombie must not be protected from turret fire");

            BrainUtil.setTargetOfEntity(combatTurret, hostileZombie);
            int initialAmmo = com.tacz.guns.api.item.IGun.getIGunOrNull(combatTurret.getGunStack()).getCurrentAmmoCount(combatTurret.getGunStack());
            assertCondition(initialAmmo == 30, "Initial ammo must be 30");

            for (int t = 0; t < 30; t++) {
                combatTurret.tick();
                combatTurret.customServerAiStep(level);
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }

            int finalAmmo = com.tacz.guns.api.item.IGun.getIGunOrNull(combatTurret.getGunStack()).getCurrentAmmoCount(combatTurret.getGunStack());
            LOGGER.info("[Cat 8 debug] Ammo before: {}, ammo after combat ticks: {}", initialAmmo, finalAmmo);
            assertCondition(finalAmmo < initialAmmo, "Turret must have fired bullets (final ammo < initial ammo)");

            // Test auto reload capability
            assertCondition(combatTurret.tacz$getItemHandler(null).isPresent(), "Turret tacz$getItemHandler must be present");
            IGun iGun = com.tacz.guns.api.item.IGun.getIGunOrNull(combatTurret.getGunStack());
            Identifier akAmmoId = TimelessAPI.getCommonGunIndex(iGun.getGunId(combatTurret.getGunStack())).get().getGunData().getAmmoId();
            ItemStack ammoStack = com.tacz.guns.api.item.builder.AmmoItemBuilder.create().setId(akAmmoId).setCount(64).build();
            combatTurret.getInventory().setStackInSlot(0, ammoStack);
            assertCondition(combatTurret.hasAmmo(), "Turret with ammo in inventory must return hasAmmo() == true");

            // Empty the gun to 0 rounds to trigger auto-reload
            iGun.setCurrentAmmoCount(combatTurret.getGunStack(), 0);
            iGun.setBulletInBarrel(combatTurret.getGunStack(), false);
            assertCondition(!combatTurret.gunHasAmmo(), "Gun must have 0 ammo");

            // Tick turret - should detect !gunHasAmmo() and start reload
            IGunOperator gunOperator = IGunOperator.fromLivingEntity(combatTurret);
            ItemStack gunStk = combatTurret.getGunStack();
            com.tacz.guns.api.item.gun.AbstractGunItem agi = (com.tacz.guns.api.item.gun.AbstractGunItem) gunStk.getItem();
            LOGGER.info("[Cat 8 debug] Pre-reload checks: shootCooldown={}, drawCooldown={}, needCheckAmmo={}, canReload={}, hasAmmoSource={}, hasAmmoInventory={}",
                    gunOperator.getSynShootCoolDown(),
                    gunOperator.getSynDrawCoolDown(),
                    gunOperator.needCheckAmmo(),
                    agi.canReload(combatTurret, gunStk),
                    com.tacz.guns.api.item.ammo.AmmoSourceRegistry.hasAmmo(combatTurret, gunStk),
                    com.tacz.guns.api.item.ammo.AmmoSourceRegistry.hasAmmo(combatTurret.getInventory(), gunStk));

            // Wait for shoot cooldown from previous combat shots to elapse before auto-reloading
            long cdWaitStart = System.currentTimeMillis();
            while (gunOperator.getSynShootCoolDown() > 0 && (System.currentTimeMillis() - cdWaitStart < 1000)) {
                try { Thread.sleep(20); } catch (InterruptedException ignored) {}
            }

            combatTurret.tick();
            boolean isReloading = gunOperator.getSynReloadState().getStateType().isReloading();
            LOGGER.info("[Cat 8 debug] Auto-reload triggered: isReloading={}", isReloading);
            assertCondition(isReloading, "Turret must initiate auto-reload when gun is empty and inventory has ammo");

            // Wait for reload to complete (ak47 reload takes ~2-3 seconds, tick until completed)
            long reloadWaitStart = System.currentTimeMillis();
            while (gunOperator.getSynReloadState().getStateType().isReloading() && (System.currentTimeMillis() - reloadWaitStart < 4000)) {
                combatTurret.tick();
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
            int reloadedAmmo = iGun.getCurrentAmmoCount(combatTurret.getGunStack());
            int remainingInventoryAmmo = combatTurret.getInventory().getStackInSlot(0).getCount();
            LOGGER.info("[Cat 8 debug] Ammo after auto-reload: {} (inventory ammo count: {})", reloadedAmmo, remainingInventoryAmmo);
            assertCondition(reloadedAmmo > 0, "Gun must have refilled ammo from inventory (reloadedAmmo > 0)");
            assertCondition(remainingInventoryAmmo < 64, "Inventory ammo must be consumed during reload");

            Zombie ownerDummy = new Zombie(level);
            ownerDummy.setUUID(ownerId);
            ownerDummy.setPos(1.5, 100.0, 0.5);
            assertCondition(combatTurret.isProtectedFromFire(ownerDummy), "Owner must be protected from fire");
            assertCondition(!combatTurret.isValidTarget(ownerDummy), "Owner must not be a valid target");

            Zombie allyDummy = new Zombie(level);
            allyDummy.setUUID(allyId);
            allyDummy.setPos(2.5, 100.0, 0.5);
            assertCondition(combatTurret.isProtectedFromFire(allyDummy), "Ally must be protected from fire");
            assertCondition(!combatTurret.isValidTarget(allyDummy), "Ally must not be a valid target");

            combatTurret.discard();
            hostileZombie.discard();
            ownerDummy.discard();
            allyDummy.discard();

            passed++;
            LOGGER.info("[Cat 8/8: SentryLoadoutAndCombatAI] PASS: Sentry loadout equipped, Hostile Zombie targeted, and Owner/Ally protected.");

            LOGGER.info("=== TACZ TURRETS 26.3 PREPROD SERVER TEST REPORT: {}/{} PASSED ===", passed, total);
            server.halt(false);
        } catch (Throwable t) {
            LOGGER.error("=== TACZ TURRETS 26.3 PREPROD SERVER TEST FAILED ({}/{} PASSED) ===", passed, total, t);
            Runtime.getRuntime().halt(1);
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
