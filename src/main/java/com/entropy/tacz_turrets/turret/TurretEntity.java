package com.entropy.tacz_turrets.turret;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.compat.CreateCompat;
import com.entropy.tacz_turrets.compat.FTBTeamsCompat;
import com.entropy.tacz_turrets.compat.OpenPACCompat;
import com.entropy.tacz_turrets.config.TACZTurretsConfig;
import com.entropy.tacz_turrets.item.CamoItem;
import com.entropy.tacz_turrets.menu.TurretLayout;
import com.entropy.tacz_turrets.menu.TurretMenu;
import com.entropy.tacz_turrets.registry.EntityTypeRegistry;
import com.entropy.tacz_turrets.registry.ItemRegistry;
import com.entropy.tacz_turrets.registry.SoundRegistry;
import com.entropy.tacz_turrets.registry.TagRegistry;
import com.entropy.tacz_turrets.turret.ai.TaczShootAttack;
import com.entropy.tacz_turrets.util.Enums;
import com.entropy.tacz_turrets.util.PlacedTurrets;
import com.entropy.tacz_turrets.util.RegistryFilter;
import com.entropy.tacz_turrets.util.TurretAllies;
import com.entropy.tacz_turrets.util.TurretEnergyStorage;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import cn.sh1rocu.tacz.api.mixin.ItemHandlerCapability;
import cn.sh1rocu.tacz.util.forge.LazyOptional;
import cn.sh1rocu.tacz.util.itemhandler.IItemHandler;
import com.tacz.guns.util.AttachmentDataUtils;
import com.mojang.datafixers.util.Pair;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.behaviour.base.ExtendedBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.base.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetPlayerLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRetaliateTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.TargetOrRetaliate;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;
import net.tslat.smartbrainlib.library.object.ExactPositionTracker;
import net.tslat.smartbrainlib.util.BrainUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TurretEntity extends Mob implements SmartBrainOwner<TurretEntity>, GeoEntity, MenuProvider, ItemHandlerCapability {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final IGunOperator gunOperator = IGunOperator.fromLivingEntity(this);

    private static final boolean FTB_TEAMS = FabricLoader.getInstance().isModLoaded("ftbteams");
    private static final boolean OPEN_PAC = FabricLoader.getInstance().isModLoaded("openpartiesandclaims");
    private static final int TARGET_SPREAD_INTERVAL = 10;
    private static final int TARGET_LOST_LOOK_TICKS = 60;
    private static final double TARGET_SPREAD_REACH = 4.0D;
    private static final double TARGET_SPREAD_MIN_RADIUS = 16.0D;
    private static final double TARGET_SWITCH_MARGIN = 0.5D;
    private static final int CONSERVATIVE_SHOT_INTERVAL = 8;
    private static final int RECOIL_TICKS = 3;
    private static final float RECOIL_DEGREES = 9.0F;
    private static final float RECOIL_PUSH = 0.09F;
    private static final int REPAIR_INTERACT_GRACE = 10;
    private static final int RETALIATE_UNTIL_DEATH = -1;
    private static final int LOCK_ON_TICKS = 20;
    private static final int LOCK_ON_GAP = 20;
    private static final int PAN_COOLDOWN = 20;
    private static final float IDLE_WALL_TILT = 45.0F;
    private static final EntityDataAccessor<Integer> RECOIL = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> ATTACH_FACE = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> PAINT = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<String> OWNER_ID = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> ROTATION_SPEED = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);
    private static final String PAINT_TAG = "Paint";
    public static final int DEATH_MESSAGES = 3;
    private boolean gunDrawn = false;
    private Identifier loadout = SentryLoadout.DEFAULT;
    private boolean rolled = false;
    private int sentryRange = 32;
    private boolean infiniteAmmo = false;
    private boolean tracked = false;
    private int lockOnTicks = 0;
    private int lastTargetTick = -100;
    private boolean wasTurning = false;
    private int lastPanTick = -100;
    private TurretEnableType enableType = TurretEnableType.ALWAYS_ON;
    private TurretMode mode = TurretMode.AGGRESSIVE;
    private PlayerTargeting playerTargeting = PlayerTargeting.RETALIATE;
    private String ownerName = "";
    private int lastShotTick = 0;
    private boolean hadTarget = false;
    private int lastRepairTick = -100;
    private UUID retaliateTarget;
    private int retaliateTicks = 0;
    private final ItemStackHandler inventory = new ItemStackHandler(TurretLayout.forType(TurretType.MODERN).ammoSlots);
    private final List<ItemStack> overflow = new ArrayList<>();
    private final TurretEnergyStorage energy = new TurretEnergyStorage(TurretType.MODERN.stats().energyCapacity.get(), TurretType.MODERN.stats().energyTransferRate.get());
    public UUID owner;

    public TurretEntity(Level level, BlockPos pos, Player player, TurretType type) {
        this(EntityTypeRegistry.TURRET.get(), level);
        setTurretType(type);
        setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        owner = player.getUUID();
        ownerName = player.getGameProfile().name();
        entityData.set(OWNER_ID, owner.toString());
    }

    public TurretEntity(EntityType<? extends TurretEntity> type, Level level) {
        super(type, level);
        lookControl = new LookControl(this) {
            @Override
            public void tick() {
                if (getAttachFace().getAxis() == Direction.Axis.Y) {
                    super.tick();
                } else if (lookAtCooldown > 0) {
                    lookAtCooldown--;
                    turnTowards(new Vector3f((float) (wantedX - getX()), (float) (wantedY - getEyeY()), (float) (wantedZ - getZ())), yMaxRotSpeed);
                }
            }
        };
        setYRot(0.0F);
        setYHeadRot(0.0F);
        setYBodyRot(0.0F);
        gunOperator.initialData();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, TurretState.NO_GUN.ordinal());
        builder.define(RECOIL, 0);
        builder.define(ATTACH_FACE, Direction.DOWN);
        builder.define(TYPE, TurretType.MODERN.ordinal());
        builder.define(PAINT, ItemStack.EMPTY);
        builder.define(OWNER_ID, "");
        builder.define(ROTATION_SPEED, 0.0F);
    }

    public TurretType getTurretType() {
        return Enums.byOrdinal(TurretType.class, entityData.get(TYPE));
    }

    public void setTurretType(TurretType type) {
        entityData.set(TYPE, type.ordinal());
        energy.setLimits(stats().energyCapacity.get(), stats().energyTransferRate.get());
        resizeInventory();
    }

    public TACZTurretsConfig.TurretStats stats() {
        return getTurretType().stats();
    }

    public Direction getAttachFace() {
        return entityData.get(ATTACH_FACE);
    }

    public void setAttachFace(Direction face) {
        entityData.set(ATTACH_FACE, face);
    }

    public static Quaternionf frame(Direction up) {
        return up == Direction.DOWN ? new Quaternionf().rotationZ(Mth.PI) : new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), up.step());
    }

    public static Vector2f toLocal(Direction up, float yaw, float pitch) {
        Vector3f look = frame(up).conjugate().transform(Vec3.directionFromRotation(pitch, yaw).toVector3f());
        return new Vector2f((float) Math.toDegrees(Math.atan2(-look.x, look.z)), (float) Math.toDegrees(Math.asin(Mth.clamp(-look.y, -1.0F, 1.0F))));
    }

    private void turnTowards(Vector3f target, float maxDegrees) {
        Vector3f look = Vec3.directionFromRotation(getXRot(), getYHeadRot()).toVector3f();
        float angle = look.angle(target.normalize());
        if (!(angle > 1.0E-4F)) return;
        new Quaternionf().slerp(new Quaternionf().rotationTo(look, target), Math.min(1.0F, maxDegrees * Mth.DEG_TO_RAD / angle)).transform(look);
        setYHeadRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        setXRot((float) Math.toDegrees(Math.asin(Mth.clamp(-look.y, -1.0F, 1.0F))));
    }

    private Vec3 idleLookDirection() {
        Direction up = getAttachFace().getOpposite();
        boolean wall = up.getAxis() != Direction.Axis.Y;
        float yaw = wall ? toLocal(up, getYHeadRot(), getXRot()).x + (random.nextBoolean() ? 1.0F : -1.0F) * Mth.randomBetween(random, 30.0F, 90.0F) : random.nextFloat() * 360.0F;
        return new Vec3(frame(up).transform(Vec3.directionFromRotation(wall ? -IDLE_WALL_TILT : 0.0F, yaw).toVector3f()));
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ATTACH_FACE.equals(key)) refreshDimensions();
    }

    @Override
    public boolean isNoGravity() {
        return getAttachFace() != Direction.DOWN || super.isNoGravity();
    }

    @Override
    protected @NotNull EntityDimensions getDefaultDimensions(@NotNull Pose pose) {
        EntityDimensions dimensions = super.getDefaultDimensions(pose);
        return getAttachFace() == Direction.DOWN ? dimensions : dimensions.withEyeHeight(dimensions.height() * (getAttachFace() == Direction.UP ? 0.25F : 0.5F));
    }

    public static AttributeSupplier.@NotNull Builder createLivingAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, Arrays.stream(TurretType.values()).mapToInt(type -> type.defaultRange * 2).max().orElse(128)).add(Attributes.ARMOR, TurretType.MODERN.defaultArmor).add(Attributes.MAX_HEALTH, TurretType.MODERN.defaultHealth);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            items.add(inventory.getStackInSlot(i));
        }
        output.store("Inventory", ItemStack.OPTIONAL_CODEC.listOf(), items);
        output.storeNullable("Owner", UUIDUtil.CODEC, owner);
        output.putString("EnableType", enableType.name());
        output.putString("Mode", mode.name());
        output.putString("PlayerTargeting", playerTargeting.name());
        output.putString("OwnerName", ownerName);
        output.putInt("Energy", energy.getEnergyStored());
        output.putString("AttachFace", getAttachFace().name());
        output.putString("Type", getTurretType().name());
        output.store("Paint", ItemStack.OPTIONAL_CODEC, getPaint());
        if (!isSentry()) return;
        output.putString("Loadout", loadout.toString());
        output.putBoolean("Rolled", rolled);
        output.putInt("Range", sentryRange);
        output.putBoolean("InfiniteAmmo", infiniteAmmo);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("Inventory", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(items -> {
            for (int i = 0; i < Math.min(items.size(), inventory.getSlots()); i++) {
                inventory.setStackInSlot(i, items.get(i));
            }
        });
        setTurretType(Enums.byName(input.getString("Type").orElse(""), TurretType.MODERN));
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        entityData.set(OWNER_ID, owner == null ? "" : owner.toString());
        enableType = Enums.byName(input.getString("EnableType").orElse(""), TurretEnableType.ALWAYS_ON);
        mode = Enums.byName(input.getString("Mode").orElse(""), TurretMode.AGGRESSIVE);
        playerTargeting = Enums.byName(input.getString("PlayerTargeting").orElse(""), PlayerTargeting.RETALIATE);
        ownerName = input.getString("OwnerName").orElse("");
        energy.setEnergy(input.getInt("Energy").orElse(0));
        setAttachFace(Enums.byName(input.getString("AttachFace").orElse(""), input.getBooleanOr("Ceiling", false) ? Direction.UP : Direction.DOWN));
        input.read("Paint", ItemStack.OPTIONAL_CODEC).ifPresent(this::setPaint);
        input.getString("Loadout").ifPresent(str -> loadout = Objects.requireNonNullElse(Identifier.tryParse(str), SentryLoadout.DEFAULT));
        rolled = input.getBooleanOr("Rolled", false);
        sentryRange = input.getInt("Range").orElse(32);
        infiniteAmmo = input.getBooleanOr("InfiniteAmmo", false);
    }

    public boolean isSentry() {
        return getType() == EntityTypeRegistry.SENTRY.get();
    }

    private void rollLoadout(Holder<Biome> biome) {
        SentryLoadout.get(loadout).ifPresent(sentry -> sentry.apply(this, biome));
        rolled = true;
    }

    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull EntitySpawnReason spawnType, @Nullable SpawnGroupData data) {
        if (!isSentry()) return super.finalizeSpawn(level, difficulty, spawnType, data);
        rollLoadout(level.getBiome(blockPosition()));
        return data;
    }

    public void setLoadout(Identifier id) {
        loadout = id;
    }

    public void setSentryRange(int range) {
        sentryRange = range;
    }

    public boolean hasInfiniteAmmo() {
        return infiniteAmmo || !TACZTurretsConfig.CONSUME_AMMO.get();
    }

    public boolean usesMagazine() {
        return isSentry() ? TACZTurretsConfig.SENTRIES_RELOAD.get() : !hasInfiniteAmmo();
    }

    private void useRound() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(getGunStack(), inventory.getStackInSlot(slot))) {
                inventory.extractItem(slot, 1, false);
                return;
            }
        }
        IGun gun = IGun.getIGunOrNull(getGunStack());
        if (gun != null) gun.setCurrentAmmoCount(getGunStack(), Math.max(0, gun.getCurrentAmmoCount(getGunStack()) - 1));
    }

    public void setInfiniteAmmo(boolean infinite) {
        infiniteAmmo = infinite;
    }

    public void trackPlacement() {
        tracked = true;
        if (owner != null && !isSentry() && level().getServer() != null) PlacedTurrets.get(level().getServer()).add(owner, getUUID());
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (reason.shouldDestroy() && owner != null && level().getServer() != null) PlacedTurrets.get(level().getServer()).remove(owner, getUUID());
        super.remove(reason);
    }

    public boolean isOwnerId(UUID id) {
        return id != null && id.toString().equals(entityData.get(OWNER_ID));
    }

    private double difficultyScale() {
        if (!TACZTurretsConfig.SENTRY_DIFFICULTY_SCALING.get()) return 1.0D;
        return switch (level().getDifficulty()) {
            case EASY -> 0.75D;
            case HARD -> 1.25D;
            default -> 1.0D;
        };
    }

    public double reloadSpeed() {
        return stats().reloadSpeed.get() * (isSentry() ? TACZTurretsConfig.SENTRY_RELOAD_SPEED_MULTIPLIER.get() : 1.0D);
    }

    public double fireRate() {
        double rate = TACZTurretsConfig.FIRE_RATE_STATS.get() ? stats().fireRate.get() : 1.0D;
        if (isSentry()) return rate * TACZTurretsConfig.SENTRY_FIRE_RATE_MULTIPLIER.get();
        if (hasCog() && getRotationSpeed() != 0.0F && TACZTurretsConfig.ROTATION_FIRE_RATE.get()) {
            rate *= Math.min(1.0D, Math.abs(getRotationSpeed()) / TACZTurretsConfig.FULL_FIRE_RATE_ROTATION.get());
        }
        return rate;
    }

    @Override
    protected int getBaseExperienceReward(@NotNull ServerLevel level) {
        return isSentry() ? TACZTurretsConfig.SENTRY_EXPERIENCE.get() : 0;
    }

    public float damageMultiplier(LivingEntity victim) {
        if (isSentry()) return (float) (TACZTurretsConfig.SENTRY_DAMAGE_MULTIPLIER.get() * difficultyScale());
        return (float) (TACZTurretsConfig.TURRET_DAMAGE_MULTIPLIER.get() * (victim instanceof Player ? TACZTurretsConfig.PVP_DAMAGE_MULTIPLIER.get() : 1.0D));
    }

    @Override
    public void die(@NotNull DamageSource source) {
        Component message = getCombatTracker().getDeathMessage();
        super.die(source);
        if (dead && !isSentry() && TACZTurretsConfig.NOTIFY_OWNER.get() && getOwnerPlayer() instanceof ServerPlayer player) {
            player.sendSystemMessage(message);
        }
    }

    public ItemStack getGunStack() {
        return getMainHandItem();
    }

    public void setGunStack(ItemStack stack) {
        setItemSlot(EquipmentSlot.MAINHAND, stack);
    }

    public ItemStack getLaserStack() {
        return getOffhandItem();
    }

    public void setLaserStack(ItemStack stack) {
        setItemSlot(EquipmentSlot.OFFHAND, stack);
    }

    public boolean isLaser(ItemStack stack) {
        IAttachment attachment = IAttachment.getIAttachmentOrNull(stack);
        return attachment != null && attachment.getType(stack) == AttachmentType.LASER;
    }

    public boolean hasLaser() {
        return isLaser(getLaserStack());
    }

    public ItemStack getPaint() {
        return entityData.get(PAINT);
    }

    public void setPaint(ItemStack stack) {
        entityData.set(PAINT, stack);
    }

    public static boolean isPaint(ItemStack stack) {
        return stack.getItem() instanceof BannerItem || stack.getItem() instanceof DyeItem || stack.getItem() instanceof CamoItem;
    }

    private Tag savePaint() {
        if (getPaint().isEmpty()) return new CompoundTag();
        return ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, getPaint()).result().orElseGet(CompoundTag::new);
    }

    public void loadPaint(CompoundTag tag) {
        if (tag != null && tag.contains(PAINT_TAG)) {
            ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, tag.get(PAINT_TAG)).result().ifPresent(this::setPaint);
        }
    }

    public ItemStack toItem() {
        ItemStack stack = new ItemStack(ItemRegistry.turret(getTurretType()));
        if (getPaint().isEmpty()) return stack;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(PAINT_TAG, savePaint()));
        return stack;
    }

    public ModernKineticGunItem getGunItem() {
        return hasGun() ? (ModernKineticGunItem) getGunStack().getItem() : null;
    }

    public boolean hasMinigun() {
        return hasGun() && TimelessAPI.getGunDisplay(getGunStack()).map(display -> display.getThirdPersonAnimation().equals("minigun")).orElse(false);
    }

    public boolean hasGun() {
        return getGunStack().getItem() instanceof ModernKineticGunItem;
    }

    public boolean gunHasAmmo() {
        if (!hasGun()) return false;
        if (getGunItem().useInventoryAmmo(getGunStack())) {
            return getGunItem().hasInventoryAmmo(this, getGunStack(), gunOperator.needCheckAmmo());
        }
        return getGunItem().getCurrentAmmoCount(getGunStack()) > 0 || hasChamberedRound();
    }

    private boolean hasChamberedRound() {
        ItemStack gunStack = getGunStack();
        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null || !iGun.hasBulletInBarrel(gunStack)) return false;
        return TimelessAPI.getCommonGunIndex(iGun.getGunId(gunStack))
                .map(index -> index.getGunData().getBolt() != Bolt.OPEN_BOLT)
                .orElse(false);
    }

    public boolean isEnabled() {
        return TurretState.getState(this) != TurretState.DISABLED;
    }

    public boolean isSniper() {
        if (!hasGun()) return false;
        return TimelessAPI.getCommonGunIndex(getGunItem().getGunId(getGunStack()))
                .map(index -> TACZTurretsConfig.sniperGunTypes.contains(index.getType()))
                .orElse(false);
    }

    public double getRange() {
        if (isSentry()) return Math.min(TACZTurretsConfig.SNIPER_SENTRY_RANGE.get() && isSniper() ? sentryRange * 2 : sentryRange, TACZTurretsConfig.maxRange());
        return isSniper() ? stats().sniperRange.get() : stats().range.get();
    }

    public void tryShoot() {
        if (!isEnabled() || lockOnTicks > 0) return;
        if (isConservingAmmo() && tickCount - lastShotTick < CONSERVATIVE_SHOT_INTERVAL) return;
        gunOperator.aim(true);
        ShootResult result = shoot();
        switch (result) {
            case SUCCESS -> {
                lastShotTick = tickCount;
                if (!usesMagazine() && !hasInfiniteAmmo()) useRound();
                if (TACZTurretsConfig.TURRET_RECOIL.get()) entityData.set(RECOIL, RECOIL_TICKS);
                if (usesEnergy()) energy.consume(stats().energyPerShot.get());
            }
            case NEED_BOLT -> gunOperator.bolt();
            case NO_AMMO -> {
                collectAmmo();
                gunOperator.reload();
            }
            case NOT_DRAW -> gunOperator.draw(this::getGunStack);
        }
        if (TACZTurretsConfig.LOG_TURRET_SHOOT_RESULTS.get()) TACZTurrets.LOGGER.info("Turret shoot result {}", result);
    }

    private ShootResult shoot() {
        float inaccuracy = getInaccuracy();
        if (inaccuracy <= 0.0F) {
            return gunOperator.shoot(() -> getViewXRot(1), () -> getViewYRot(1));
        }
        float pitchOffset = (float) random.nextGaussian() * inaccuracy;
        float yawOffset = (float) random.nextGaussian() * inaccuracy;
        return gunOperator.shoot(() -> getViewXRot(1) + pitchOffset, () -> getViewYRot(1) + yawOffset);
    }

    private float getInaccuracy() {
        float inaccuracy = switch (TACZTurretsConfig.INACCURACY_MODE.get()) {
            case RANDOM -> stats().randomInaccuracy.get().floatValue();
            case DISTANCE -> {
                LivingEntity target = BrainUtil.getTargetOfEntity(this);
                if (target == null) yield 0.0F;
                double distance = Math.sqrt(distanceToSqr(target));
                yield (float) (stats().distanceInaccuracy.get() * Math.min(1.0D, distance / getRange()));
            }
        };
        return isSentry() ? inaccuracy / TACZTurretsConfig.SENTRY_ACCURACY_MULTIPLIER.get().floatValue() : inaccuracy;
    }

    public boolean hasAmmo() {
        if (hasInfiniteAmmo()) return true;
        if (gunHasAmmo()) return true;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (isRightAmmo(inventory.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    public boolean isRightAmmo(ItemStack stack) {
        if (stack.getItem() instanceof IAmmoBox ammoBox) {
            if (ammoBox.isAllTypeCreative(stack)) {
                return true;
            }
            return hasGun() && ammoBox.isAmmoBoxOfGun(getGunStack(), stack);
        }
        return hasGun() && stack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(getGunStack(), stack);
    }

    public boolean hasCreativeAmmo() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (isCreativeAmmo(inventory.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    public boolean isCreativeAmmo(ItemStack stack) {
        return stack.getItem() instanceof IAmmoBox ammoBox && (ammoBox.isCreative(stack) || ammoBox.isAllTypeCreative(stack));
    }

    @Nullable
    private BlockEntity getSupplyBlockEntity() {
        BlockEntity blockEntity = level().getBlockEntity(blockPosition());
        return blockEntity == null ? level().getBlockEntity(blockPosition().relative(getAttachFace())) : blockEntity;
    }

    private Direction getSupplySide() {
        return getAttachFace().getOpposite();
    }

    public void collectAmmo() {
        if (!shouldCollectAmmo()) return;
        BlockEntity blockEntity = getSupplyBlockEntity();
        if (blockEntity instanceof Container container) {
            WorldlyContainer worldly = container instanceof WorldlyContainer wc ? wc : null;
            Direction supplySide = getSupplySide();
            int size = worldly != null ? worldly.getSlotsForFace(supplySide).length : container.getContainerSize();
            for (int i = 0; i < size; i++) {
                int slot = worldly != null ? worldly.getSlotsForFace(supplySide)[i] : i;
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || !isRightAmmo(stack)) continue;
                if (worldly != null && !worldly.canTakeItemThroughFace(slot, stack, supplySide)) continue;
                int fits = stack.getCount() - ItemHandlerHelper.insertItemStacked(inventory, stack, true).getCount();
                if (fits <= 0) continue;
                ItemStack extracted = container.removeItem(slot, fits);
                ItemHandlerHelper.insertItemStacked(inventory, extracted, false);
                container.setChanged();
                if (isCreativeAmmo(extracted)) return;
            }
        }
    }

    public boolean shouldCollectAmmo() {
        return TACZTurretsConfig.PULL_AMMO_FROM_STORAGE.get() && !hasInfiniteAmmo() && isEnabled() && !hasCreativeAmmo();
    }

    public boolean usesEnergy() {
        return !isSentry() && getTurretType() != TurretType.BRASS && TACZTurretsConfig.requirePower(getTurretType());
    }

    public boolean hasEnoughEnergy() {
        if (!usesEnergy()) return true;
        return energy.getEnergyStored() >= Math.max(1, stats().energyPerShot.get());
    }

    private void tickEnergy() {
        if (!usesEnergy()) return;
        energy.setLimits(stats().energyCapacity.get(), stats().energyTransferRate.get());
        if (stats().energyIdleDrain.get() > 0 && !enableType.shouldDisable(level(), blockPosition())) {
            energy.consume(stats().energyIdleDrain.get());
        }
        collectEnergy();
    }

    private void collectEnergy() {
        int space = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (space <= 0) return;
    }

    public float getRotationSpeed() {
        return entityData.get(ROTATION_SPEED);
    }

    private boolean hasCog() {
        return getTurretType() == TurretType.BRASS && TurretType.BRASS.isAvailable();
    }

    public boolean hasRotation() {
        return !hasCog() || isSentry() || !TACZTurretsConfig.requirePower(TurretType.BRASS) || getRotationSpeed() != 0.0F;
    }

    private void tickRotation() {
        entityData.set(ROTATION_SPEED, hasCog() ? CreateCompat.cogSpeed(level(), blockPosition(), getAttachFace().getAxis()) : 0.0F);
    }

    private void syncAttributes() {
        double armor = stats().armor.get() * (isSentry() ? TACZTurretsConfig.SENTRY_ARMOR_MULTIPLIER.get() : 1.0D);
        if (getAttributeBaseValue(Attributes.ARMOR) != armor) getAttribute(Attributes.ARMOR).setBaseValue(armor);
        double health = stats().health.get() * (isSentry() ? TACZTurretsConfig.SENTRY_HEALTH_MULTIPLIER.get() * difficultyScale() : 1.0D);
        if (getAttributeBaseValue(Attributes.MAX_HEALTH) == health) return;
        float fraction = getHealth() / getMaxHealth();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        setHealth(fraction * getMaxHealth());
    }

    private void tickPassiveHealing() {
        if (!(isSentry() ? TACZTurretsConfig.SENTRY_PASSIVE_HEALING : TACZTurretsConfig.PASSIVE_HEALING).get()) return;
        if (getHealth() >= getMaxHealth()) return;
        if (tickCount % TACZTurretsConfig.PASSIVE_HEAL_INTERVAL.get() != 0) return;
        heal(stats().passiveHealAmount.get().floatValue());
    }

    @Override
    public void tick() {
        Vec3 motion = getDeltaMovement();
        if (motion.x != 0.0D || motion.z != 0.0D || motion.y > 0.0D) setDeltaMovement(0.0D, Math.min(motion.y, 0.0D), 0.0D);
        super.tick();
        if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
        }
        onTickServerSide();

        if (!level().isClientSide() && hasGun() && !gunOperator.getSynReloadState().getStateType().isReloading()) {
            if (!gunHasAmmo()) {
                gunOperator.reload();
            } else if (BrainUtil.getTargetOfEntity(this) == null && getTarget() == null && tickCount - lastShotTick > 60 && shouldTacticalReload()) {
                gunOperator.reload();
            }
        }
    }

    public boolean shouldTacticalReload() {
        if (!hasGun() || !hasAmmo()) return false;
        ItemStack gun = getGunStack();
        if (!(gun.getItem() instanceof ModernKineticGunItem gunItem)) return false;
        if (gunItem.useInventoryAmmo(gun)) return false;
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) return false;
        return TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).map(index -> {
            int max = AttachmentDataUtils.getAmmoCountWithAttachment(gun, index.getGunData());
            return iGun.getCurrentAmmoCount(gun) < max;
        }).orElse(false);
    }

    @Override
    public LazyOptional<IItemHandler> tacz$getItemHandler(Direction dir) {
        return LazyOptional.of(this::getInventory);
    }

    public boolean isConservingAmmo() {
        if (mode == TurretMode.CONSERVATIVE) return true;
        if (mode != TurretMode.ADAPTIVE) return false;
        LivingEntity target = BrainUtil.getTargetOfEntity(this);
        if (target == null) return false;
        return distanceToSqr(target) > (double) TACZTurretsConfig.ADAPTIVE_RANGE.get() * TACZTurretsConfig.ADAPTIVE_RANGE.get();
    }

    private float getRecoilProgress(float partialTick) {
        int recoil = entityData.get(RECOIL);
        if (recoil <= 0) return 0.0F;
        return Mth.clamp((recoil - partialTick) / RECOIL_TICKS, 0.0F, 1.0F);
    }

    public float getRecoilDegrees(float partialTick) {
        if (TACZTurretsConfig.RECOIL_TYPE.get() != RecoilType.BOUNCE) return 0.0F;
        return RECOIL_DEGREES * getRecoilProgress(partialTick);
    }

    public float getRecoilPush(float partialTick) {
        if (TACZTurretsConfig.RECOIL_TYPE.get() != RecoilType.PUSH) return 0.0F;
        return RECOIL_PUSH * getRecoilProgress(partialTick);
    }

    private void dropStack(ItemStack stack) {
        if (!stack.isEmpty() && level() instanceof ServerLevel sl) spawnAtLocation(sl, stack);
    }

    private void onTickServerSide() {
        if (!level().isClientSide()) {
            if (isSentry() && !rolled) rollLoadout(level().getBiome(blockPosition()));
            if (!tracked) trackPlacement();
            BlockPos support = blockPosition().relative(getAttachFace());
            if (getAttachFace() != Direction.DOWN && !Block.isFaceFull(level().getBlockState(support).getCollisionShape(level(), support), getAttachFace().getOpposite())) setAttachFace(Direction.DOWN);
            dropOverflow();
            if (getAttachFace() == Direction.DOWN && !onGround() && !level().getEntitiesOfClass(TurretEntity.class, getBoundingBox(), turret -> turret != this).isEmpty()) {
                dropContents();
                dropStack(toItem());
                playTurretSound(SoundRegistry.TURRET_PICKUP.get());
                discard();
                return;
            }
            int recoil = entityData.get(RECOIL);
            if (recoil > 0) entityData.set(RECOIL, recoil - 1);
            tickEnergy();
            tickRotation();
            syncAttributes();
            tickPassiveHealing();
            tickLockOn();
            tickPanSound();
            if (isEnabled()) {
                if (hasGun()) {
                    if (!gunDrawn) {
                        gunOperator.draw(this::getGunStack);
                        gunDrawn = true;
                    }
                    if (gunOperator.getSynReloadState().getStateType().isReloading()) {
                        TurretState.RELOADING.setState(this);
                    } else if (hasAmmo()) {
                        TurretState.ACTIVE.setState(this);
                    } else {
                        TurretState.NO_AMMO.setState(this);
                        collectAmmo();
                    }
                } else {
                    TurretState.NO_GUN.setState(this);
                }
                if (shouldDisable()) {
                    TurretState.DISABLED.setState(this);
                }
            } else if (!shouldDisable()) {
                TurretState.NO_GUN.setState(this);
            }
        }
    }

    private boolean shouldDisable() {
        return enableType.shouldDisable(level(), blockPosition()) || !hasEnoughEnergy() || !hasRotation();
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        boolean manages = canInteract(player);
        if (!manages && isSentry()) return InteractionResult.PASS;
        if (!manages && player.isCrouching()) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (manages) {
            InteractionResult result = manageInteract(player, hand);
            if (result != null) return result;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            TurretLayout layout = TurretLayout.forType(getTurretType());
            serverPlayer.openMenu(new ExtendedMenuProvider<FriendlyByteBuf>() {
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                    return TurretEntity.this.createMenu(containerId, playerInventory, player);
                }

                @Override
                public Component getDisplayName() {
                    return TurretEntity.this.getDisplayName();
                }

                @Override
                public FriendlyByteBuf getScreenOpeningData(ServerPlayer player) {
                    FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                    writeMenuData(buf, layout, manages);
                    return buf;
                }
            });
        }
        return InteractionResult.SUCCESS;
    }

    private void writeMenuData(FriendlyByteBuf buf, TurretLayout layout, boolean manages) {
        buf.writeVarInt(getId());
        buf.writeByte(layout.rows);
        buf.writeByte(layout.columns);
        buf.writeUtf(getOwnerName());
        Set<UUID> allies = getAllies();
        buf.writeVarInt(allies.size());
        for (UUID ally : allies) buf.writeUUID(ally);
        buf.writeBoolean(manages);
    }

    @Nullable
    private InteractionResult manageInteract(Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (!player.isCrouching() && TACZTurretsConfig.repairItems.matches(heldStack.getItem())) {
            if (getHealth() < getMaxHealth()) {
                heal(stats().repairAmount.get().floatValue());
                if (!player.isCreative()) heldStack.shrink(1);
                playRepairSound();
                spawnRepairParticles();
            }
            lastRepairTick = tickCount;
            return InteractionResult.SUCCESS;
        }
        if (player.isCrouching() && heldStack.is(ItemTags.AXES)) {
            if (!getPaint().isEmpty()) {
                setPaint(ItemStack.EMPTY);
                heldStack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                playTurretSound(SoundEvents.AXE_SCRAPE.value());
                if (level() instanceof ServerLevel serverLevel) serverLevel.sendParticles(ParticleTypes.SCRAPE, getX(), getY() + getBbHeight() * 0.6D, getZ(), 12, getBbWidth() * 0.5D, getBbHeight() * 0.4D, getBbWidth() * 0.5D, 0.0D);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isCrouching() && isPaint(heldStack)) {
            ItemStack paint = heldStack.copyWithCount(1);
            if (!ItemStack.matches(getPaint(), paint)) {
                setPaint(paint);
                if (!player.isCreative() && (paint.getItem() instanceof CamoItem ? TACZTurretsConfig.CONSUME_CAMOS : paint.getItem() instanceof DyeItem ? TACZTurretsConfig.CONSUME_DYES : TACZTurretsConfig.CONSUME_BANNERS).get()) heldStack.shrink(1);
                playTurretSound(SoundEvents.DYE_USE);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isCrouching()) {
            boolean itemsToInventory = TACZTurretsConfig.PICKUP_ITEMS_TO_INVENTORY.get();
            ItemStack gunStack = getGunStack();
            setGunStack(ItemStack.EMPTY);
            giveOrDrop(player, gunStack, itemsToInventory);
            ItemStack laserStack = getLaserStack();
            setLaserStack(ItemStack.EMPTY);
            giveOrDrop(player, laserStack, itemsToInventory);
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                giveOrDrop(player, inventory.extractItem(slot, inventory.getStackInSlot(slot).getCount(), false), itemsToInventory);
            }
            if (!player.isCreative()) giveOrDrop(player, toItem(), TACZTurretsConfig.PICKUP_TURRET_TO_INVENTORY.get());
            playTurretSound(SoundRegistry.TURRET_PICKUP.get());
            discard();
            return InteractionResult.SUCCESS;
        }
        if (tickCount - lastRepairTick < REPAIR_INTERACT_GRACE) {
            return InteractionResult.SUCCESS;
        }
        return null;
    }

    private void giveOrDrop(Player player, ItemStack stack, boolean toInventory) {
        if (!stack.isEmpty() && (!toInventory || !player.getInventory().add(stack))) dropStack(stack);
    }

    public void playTurretSound(SoundEvent sound) {
        playTurretSound(sound, 1.0F);
    }

    public void playTurretSound(SoundEvent sound, float volume) {
        if (!TACZTurretsConfig.ENABLE_SOUNDS.get()) return;
        playSound(sound, volume, 0.9F + random.nextFloat() * 0.2F);
    }

    private void tickLockOn() {
        if (lockOnTicks > 0) lockOnTicks--;
        if (BrainUtil.getTargetOfEntity(this) == null || !hasGun() || !isEnabled()) return;
        if (TACZTurretsConfig.LOCK_ON_DELAY.get() && tickCount - lastTargetTick > LOCK_ON_GAP) {
            lockOnTicks = LOCK_ON_TICKS;
            playTurretSound(SoundRegistry.TURRET_LOCK_ON.get(), TACZTurretsConfig.LOCK_ON_SOUND_RANGE.get() / 16.0F);
        }
        lastTargetTick = tickCount;
    }

    private void tickPanSound() {
        boolean turning = Math.abs(Mth.wrapDegrees(yHeadRot - yHeadRotO)) > 1.0F || Math.abs(getXRot() - xRotO) > 1.0F;
        if (turning && !wasTurning && TACZTurretsConfig.PAN_SOUNDS.get() && BrainUtil.getTargetOfEntity(this) == null && tickCount - lastPanTick > PAN_COOLDOWN) {
            lastPanTick = tickCount;
            playTurretSound(SoundRegistry.TURRET_PAN.get());
        }
        wasTurning = turning;
    }

    private void playRepairSound() {
        if (!TACZTurretsConfig.ENABLE_SOUNDS.get()) return;
        List<? extends String> sounds = TACZTurretsConfig.REPAIR_SOUNDS.get();
        if (sounds.isEmpty()) return;
        Identifier soundId = Identifier.tryParse(sounds.get(random.nextInt(sounds.size())));
        if (soundId == null) return;
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(soundId);
        if (sound != null) playSound(sound, 1.0F, 0.9F + random.nextFloat() * 0.2F);
    }

    private void spawnRepairParticles() {
        if (!TACZTurretsConfig.REPAIR_PARTICLES.get()) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight() * 0.6D, getZ(), 12, getBbWidth() * 0.5D, getBbHeight() * 0.4D, getBbWidth() * 0.5D, 0.0D);
    }

    private void resizeInventory() {
        int desired = TurretLayout.forType(getTurretType()).ammoSlots;
        if (inventory.getSlots() == desired) return;
        List<ItemStack> kept = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) kept.add(inventory.getStackInSlot(slot));
        inventory.setSize(desired);
        for (int slot = 0; slot < kept.size(); slot++) {
            ItemStack stack = kept.get(slot);
            if (stack.isEmpty()) continue;
            if (slot < desired) {
                inventory.setStackInSlot(slot, stack);
            } else {
                overflow.add(stack);
            }
        }
    }

    private void dropOverflow() {
        if (overflow.isEmpty()) return;
        for (ItemStack stack : overflow) dropStack(stack);
        overflow.clear();
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new TurretMenu(containerId, playerInventory, this, TurretLayout.forType(getTurretType()), getOwnerName(), getAllies(), canInteract(player));
    }

    public boolean canInteract(Player player) {
        return isOwnedBy(player) || (TACZTurretsConfig.ALLIES_HAVE_PERMS.get() && isAlliedWithOwner(player));
    }

    public boolean isOwnedBy(Player player) {
        boolean isOp = player instanceof ServerPlayer sp && sp.level().getServer() != null && sp.level().getServer().getPlayerList().isOp(sp.nameAndId());
        if (isSentry()) return player.isCreative() || isOp;
        return owner == null || player.getUUID().equals(owner) || player.isCreative() || (TACZTurretsConfig.OP_BYPASS.get() && isOp);
    }

    public boolean canAcceptAmmo(ItemStack stack) {
        if (isRightAmmo(stack)) return true;
        return !hasGun() && (stack.getItem() instanceof IAmmo || stack.getItem() instanceof IAmmoBox);
    }

    public boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof ModernKineticGunItem;
    }

    public void onInventoryChanged() {
        gunDrawn = false;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEquipmentPacket(getId(), List.of(Pair.of(EquipmentSlot.MAINHAND, getMainHandItem().copy()))));
        }
    }

    public TurretEnableType getEnableType() {
        return enableType;
    }

    public void setEnableType(TurretEnableType type) {
        enableType = type;
    }

    public TurretMode getMode() {
        return mode;
    }

    public void setMode(TurretMode turretMode) {
        mode = turretMode;
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public TurretEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull ServerLevel level, @NotNull DamageSource source, boolean recentlyHit) {
        if (isSentry()) {
            super.dropCustomDeathLoot(level, source, recentlyHit);
            dropInventory();
            if (recentlyHit && random.nextFloat() < TACZTurretsConfig.SENTRY_TURRET_DROP_CHANCE.get()) dropStack(toItem());
            return;
        }
        boolean dropTurret = TACZTurretsConfig.DROP_TURRET_ON_DEATH.get();
        if (dropTurret) dropStack(toItem());
        if (!TACZTurretsConfig.DROP_ITEMS_ON_DEATH.get()) return;
        dropContents();
    }

    private void dropInventory() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) dropStack(inventory.extractItem(i, inventory.getStackInSlot(i).getCount(), false));
        }
    }

    private void dropContents() {
        dropInventory();
        if (hasGun()) dropStack(getGunStack());
        setGunStack(ItemStack.EMPTY);
        if (!getLaserStack().isEmpty()) dropStack(getLaserStack());
        setLaserStack(ItemStack.EMPTY);
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float damage) {
        if (source.getEntity() instanceof TurretEntity turret && isSameSide(turret)) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity entity) {
            if (entity instanceof Player player) markRetaliation(player);
            if (isValidTarget(entity)) {
                alertTo(entity);
                for (TurretEntity turret : level.getEntitiesOfClass(TurretEntity.class, AABB.ofSize(position(), 64, 16, 64), e -> e.getSensing().hasLineOfSight(entity) || BehaviorUtils.entityIsVisible(e.getBrain(), entity))) {
                    if (entity instanceof Player player && isSameSide(turret) && Objects.equals(turret.owner, owner)) turret.markRetaliation(player);
                    if (turret.isValidTarget(entity)) turret.alertTo(entity);
                }
            }
        }

        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean isInvulnerableTo(@NotNull ServerLevel level, @NotNull DamageSource source) {
        if (source.getEntity() != null && source.getEntity().getUUID().equals(owner)) {
            return false;
        }
        if (!TACZTurretsConfig.TURRETS_TAKE_DAMAGE.get() && !isSentry()) {
            return !source.isCreativePlayer() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        }
        if (source.is(DamageTypes.LAVA)) return TACZTurretsConfig.LAVA_IMMUNE.get() || isInvulnerable() || isRemoved();
        return super.isInvulnerableTo(level, source);
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return TACZTurretsConfig.ENABLE_SOUNDS.get() ? SoundRegistry.TURRET_HURT.get() : null;
    }

    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance effect) {
        return !TACZTurretsConfig.effectBlacklist.matches(effect.getEffect().value()) && super.canBeAffected(effect);
    }

    @Override
    public ItemStack getPickResult() {
        return isSentry() ? new ItemStack(ItemRegistry.SENTRY_SPAWN_EGG.get()) : toItem();
    }

    @Override
    protected @NotNull Component getTypeName() {
        return isSentry() ? super.getTypeName() : ItemRegistry.turret(getTurretType()).getName(ItemStack.EMPTY);
    }

    @Override
    public boolean fireImmune() {
        return TACZTurretsConfig.FIRE_IMMUNE.get();
    }

    public void setTarget(@Nullable LivingEntity entity) {
        if (!isEnabled()) return;
        if (entity instanceof Player player) setLastHurtByPlayer(player, 100);
        super.setTarget(entity);
    }

    @Override
    public void customServerAiStep(@NotNull ServerLevel level) {
        super.customServerAiStep(level);
        if (!hasGun() || !isEnabled()) return;
        if (retaliateTicks < 0 && TACZTurretsConfig.RETALIATE_TARGETING.get() != RetaliateTargeting.CLEAR_ON_DEATH) retaliateTicks = retaliationTicks();
        if (retaliateTicks > 0 && --retaliateTicks == 0) retaliateTarget = null;
        retargetImmediately();
        spreadTargets();
    }

    private void retargetImmediately() {
        LivingEntity current = BrainUtil.getTargetOfEntity(this);
        if (current != null && current.isAlive()) {
            hadTarget = true;
            return;
        }
        if (!hadTarget) return;
        hadTarget = false;
        PositionTracker lastLook = BrainUtil.getMemory(this, MemoryModuleType.LOOK_TARGET);
        if (lastLook != null) BrainUtil.setForgettableMemory(this, MemoryModuleType.LOOK_TARGET, new ExactPositionTracker(lastLook.currentPosition()), TARGET_LOST_LOOK_TICKS + random.nextInt(40));
        if (!isEnabled()) return;
        NearestVisibleLivingEntities visible = BrainUtil.getMemory(this, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (visible != null) visible.findClosest(this::shouldTarget).ifPresent(this::alertTo);
    }

    private void spreadTargets() {
        if (!TACZTurretsConfig.BETTER_TARGETING.get()) return;
        if (tickCount % TARGET_SPREAD_INTERVAL != 0) return;

        LivingEntity current = BrainUtil.getTargetOfEntity(this);
        NearestVisibleLivingEntities visible = BrainUtil.getMemory(this, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (current == null || visible == null) return;

        double range = getRange();
        double currentDistance = distanceToSqr(current);
        double reachSquared = Math.min(range * range, Math.max(currentDistance * TARGET_SPREAD_REACH, TARGET_SPREAD_MIN_RADIUS * TARGET_SPREAD_MIN_RADIUS));

        Map<LivingEntity, Integer> claims = new HashMap<>();
        for (TurretEntity ally : level().getEntitiesOfClass(TurretEntity.class, getBoundingBox().inflate(range), turret -> turret != this && isSameSide(turret))) {
            LivingEntity target = BrainUtil.getTargetOfEntity(ally);
            if (target != null) claims.merge(target, 1, Integer::sum);
        }
        int currentClaims = claims.getOrDefault(current, 0);

        LivingEntity best = current;
        int bestClaims = currentClaims;
        double bestDistance = currentDistance;
        for (LivingEntity candidate : visible.findAll(entity -> entity != current && distanceToSqr(entity) <= reachSquared && shouldTarget(entity))) {
            int candidateClaims = claims.getOrDefault(candidate, 0);
            double distance = distanceToSqr(candidate);
            if (candidateClaims < bestClaims || (candidateClaims == bestClaims && distance < bestDistance)) {
                best = candidate;
                bestClaims = candidateClaims;
                bestDistance = distance;
            }
        }

        if (best != current && (bestClaims < currentClaims || bestDistance < currentDistance * TARGET_SWITCH_MARGIN)) {
            alertTo(best);
        }
    }

    @Override
    public List<? extends BehaviorControl<?>> getAlwaysRunningBehaviours(TurretEntity entity) {
        return List.of(new TargetOrRetaliate<TurretEntity>().alertPotentialAllyIf((e, l) -> l instanceof TurretEntity turret && e.isSameSide(turret)).canRetaliateAgainst(l -> l != null && isValidTarget(l)).alertAlliesIf((m, e) -> e instanceof LivingEntity living && m.isValidTarget(living) && m.getSensing().hasLineOfSight(living)).runFor(e -> 999), new LookAtTarget<TurretEntity>().runFor(e -> e.getRandom().nextInt(40, 300)));
    }

    @Override
    public List<? extends BehaviorControl<?>> getIdleBehaviours(TurretEntity entity) {
        return List.of(new FirstApplicableBehaviour<TurretEntity>(new TargetOrRetaliate<TurretEntity>().canRetaliateAgainst(l -> l != null && isValidTarget(l)), new SetPlayerLookTarget<>(), idleLook()), new Idle<TurretEntity>().runFor(e -> e.getRandom().nextInt(30, 60)));
    }

    private static ExtendedBehaviour<TurretEntity> idleLook() {
        return new SetRandomLookTarget<TurretEntity>() {
            @Override
            protected void start(TurretEntity entity) {
                BrainUtil.setForgettableMemory(entity, MemoryModuleType.LOOK_TARGET, new ExactPositionTracker(entity.getEyePosition().add(entity.idleLookDirection())), lookTime.applyAsInt(entity));
            }
        }.lookChance(1.0F).lookTime(entity -> 40 + entity.getRandom().nextInt(60)).startCondition(entity -> TACZTurretsConfig.IDLE_LOOK_AROUND.get());
    }

    @Override
    public List<? extends BehaviorControl<?>> getFightingBehaviours(TurretEntity entity) {
        return List.of(
            new InvalidateAttackTarget<TurretEntity>()
                .invalidateIf((e, target) -> !target.isAlive() || (target instanceof Player player && player.getAbilities().invulnerable) || (!e.hasLineOfSight(target) && !BrainUtil.canSee(e, target)) || !e.isValidTarget(target))
                .ignoreFailedPathfinding(),
            new SetRetaliateTarget<>(),
            new TaczShootAttack<TurretEntity>().startCondition(e -> e.hasGun() && e.gunOperator.getSynShootCoolDown() <= 0)
        );
    }

    @Override
    public List<? extends ExtendedSensor<?>> getSensors(TurretEntity entity) {
        int range = TACZTurretsConfig.maxRange();
        return ObjectArrayList.of(new NearbyPlayersSensor<TurretEntity>().setRadius(range).setPredicate((e, p) -> e.getLastHurtByMob() != null && p.getUUID().equals(e.getLastHurtByMob().getUUID()) && e.isValidTarget(p)), new HurtBySensor<>(), new NearbyLivingEntitySensor<TurretEntity>().setRadius(range).setPredicate((turret, target) -> turret.shouldTarget(target)));
    }

    @Nullable
    public Player getOwnerPlayer() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    public void alertTo(LivingEntity target) {
        if (!isEnabled()) return;
        setTarget(target);
        getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
    }

    public boolean isAlliedWithOwner(LivingEntity target) {
        MinecraftServer server = level().getServer();
        if (owner != null && server != null && TurretAllies.get(server).isAlly(owner, target.getUUID())) return true;
        if (!TACZTurretsConfig.RESPECT_TEAMS.get()) return false;
        if (owner != null && server != null && target instanceof Player && isPartyAlly(server, target.getUUID())) return true;
        if (target.getTeam() == null) return false;
        if (isAlliedTo(target)) return true;
        PlayerTeam ownerTeam = server == null || ownerName.isEmpty() ? null : server.getScoreboard().getPlayersTeam(ownerName);
        if (ownerTeam != null && ownerTeam == target.getTeam()) return true;
        Player ownerPlayer = getOwnerPlayer();
        return ownerPlayer != null && ownerPlayer.isAlliedTo(target);
    }

    private boolean isPartyAlly(MinecraftServer server, UUID target) {
        return (FTB_TEAMS && FTBTeamsCompat.isAlly(owner, target)) || (OPEN_PAC && OpenPACCompat.isAlly(server, owner, target));
    }

    private boolean canTargetPlayers() {
        if (isSentry()) return playerTargeting != PlayerTargeting.NEVER && level().getDifficulty() != Difficulty.PEACEFUL;
        return TACZTurretsConfig.DAMAGE_PLAYERS.get() && playerTargeting != PlayerTargeting.NEVER;
    }

    public boolean isSameSide(TurretEntity turret) {
        return turret.isSentry() == isSentry();
    }

    public void markRetaliation(Player player) {
        retaliateTarget = player.getUUID();
        retaliateTicks = TACZTurretsConfig.RETALIATE_TARGETING.get() == RetaliateTargeting.CLEAR_ON_DEATH ? RETALIATE_UNTIL_DEATH : retaliationTicks();
    }

    private static int retaliationTicks() {
        return Math.max(1, TACZTurretsConfig.RETALIATION_TIMER.get() * 20);
    }

    public void forgetRetaliation(UUID target) {
        if (target.equals(retaliateTarget)) {
            retaliateTarget = null;
            retaliateTicks = 0;
        }
    }

    private boolean isRetaliating(LivingEntity target) {
        return retaliateTicks != 0 && target.getUUID().equals(retaliateTarget);
    }

    private boolean canEngagePlayer(Player player) {
        return playerTargeting == PlayerTargeting.ALL || isRetaliating(player);
    }

    public PlayerTargeting getPlayerTargeting() {
        return playerTargeting;
    }

    public void setPlayerTargeting(PlayerTargeting targeting) {
        playerTargeting = targeting;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Set<UUID> getAllies() {
        if (owner == null || level().getServer() == null) return Set.of();
        return TurretAllies.get(level().getServer()).getAllies(owner);
    }

    public boolean isValidTarget(LivingEntity target) {
        if (!isEnabled()) return false;
        if (target == this || !target.isAlive()) return false;
        if (target.isInvisible() && !TACZTurretsConfig.TARGET_INVISIBLE.get()) return false;
        if (target instanceof TurretEntity turret && (isSameSide(turret) || (isSentry() && !TACZTurretsConfig.SENTRIES_TARGET_TURRETS.get()))) return false;
        double range = getRange();
        if (isSentry() && TACZTurretsConfig.SENTRY_SNEAK_RANGE.get() && target instanceof Player && target.isCrouching() && target != getTarget()) range *= TACZTurretsConfig.SENTRY_SNEAK_RANGE_MULTIPLIER.get();
        if (distanceToSqr(target) > range * range) return false;
        if (target.getUUID().equals(owner)) return false;
        if (isAlliedWithOwner(target)) return false;
        if (target.getType().builtInRegistryHolder().is(TagRegistry.TURRET_IGNORED)) return false;
        if (TACZTurretsConfig.targetBlacklist.matches(target.getType())) return false;

        if (target instanceof Player player) return canTargetPlayers() && !player.isCreative() && !player.isSpectator() && canEngagePlayer(player);
        return !isSentry() || target instanceof TurretEntity || TACZTurretsConfig.SENTRIES_TARGET_MOBS.get();
    }

    public boolean isProtectedFromFire(LivingEntity victim) {
        if (!TACZTurretsConfig.DAMAGE_PLAYERS.get() && !isSentry() && victim instanceof Player) return true;
        if (victim instanceof TurretEntity turret && isSameSide(turret)) return true;
        if (TACZTurretsConfig.OWNER_TAKES_NO_DAMAGE.get() && victim.getUUID().equals(owner)) return true;
        if (TACZTurretsConfig.ALLIES_CANNOT_BE_DAMAGED.get() && isAlliedWithOwner(victim)) return true;
        return !canDamage(victim);
    }

    public boolean canDamage(LivingEntity victim) {
        if (victim instanceof Player || victim == getTarget() || victim == getLastHurtByMob()) return true;
        RegistryFilter<EntityType<?>> filter = TACZTurretsConfig.damageableEntities;
        return filter.isEmpty() ? isTargetableType(victim) : filter.matches(victim.getType());
    }

    private boolean shouldTarget(LivingEntity target) {
        if (!isValidTarget(target)) return false;
        if (target instanceof Player) return true;
        if (target == getTarget()) return true;
        return isTargetableType(target);
    }

    private boolean isTargetableType(LivingEntity target) {
        if (target.getType().builtInRegistryHolder().is(TagRegistry.TURRET_IGNORED)) return false;
        if (TACZTurretsConfig.targetBlacklist.matches(target.getType())) return false;
        if (target instanceof TurretEntity turret) return !isSameSide(turret) && (isSentry() ? this : turret).playerTargeting == PlayerTargeting.ALL;
        if (TACZTurretsConfig.IGNORE_NAMED_MOBS.get() && target.hasCustomName()) return false;
        if (TACZTurretsConfig.targetWhitelist.matches(target.getType())) return true;
        if (target.getType().builtInRegistryHolder().is(TagRegistry.TURRET_TARGETS)) return true;
        if (TACZTurretsConfig.TARGET_ALL_MOBS.get()) return true;
        if (target instanceof Enemy || target.getType().getCategory() == MobCategory.MONSTER) return true;
        return TACZTurretsConfig.TARGET_NEUTRAL_MOBS.get() && target instanceof NeutralMob neutral && neutral.isAngry() && !(target instanceof OwnableEntity ownable && ownable.getOwnerReference() != null);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new GroundPathNavigation(this, level) {
            @Override
            protected boolean canUpdatePath() {
                return false;
            }
        };
    }

    @Override
    public void move(@NotNull MoverType type, @NotNull Vec3 movement) {
        super.move(type, type == MoverType.SELF ? new Vec3(0.0D, Math.min(movement.y, 0.0D), 0.0D) : movement);
    }

    public void knockback(double pStrength, double pX, double pZ) {

    }

    @Override
    public boolean ignoreExplosion(@NotNull Explosion explosion) {
        return TACZTurretsConfig.EXPLOSION_IMMUNE.get();
    }

    public static class ItemStackHandler implements IItemHandler {
        protected NonNullList<ItemStack> stacks;

        public ItemStackHandler(int size) {
            this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        }

        public void setSize(int size) {
            if (size == stacks.size()) return;
            NonNullList<ItemStack> newStacks = NonNullList.withSize(size, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(stacks.size(), size); i++) {
                newStacks.set(i, stacks.get(i));
            }
            this.stacks = newStacks;
        }

        public int getSlots() {
            return stacks.size();
        }

        public ItemStack getStackInSlot(int slot) {
            validateSlotIndex(slot);
            return stacks.get(slot);
        }

        public void setStackInSlot(int slot, ItemStack stack) {
            validateSlotIndex(slot);
            stacks.set(slot, stack);
            onContentsChanged(slot);
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            validateSlotIndex(slot);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack existing = stacks.get(slot);
            int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
            if (!existing.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(existing, stack)) return stack;
                limit -= existing.getCount();
            }
            if (limit <= 0) return stack;
            boolean reachedLimit = stack.getCount() > limit;
            if (!simulate) {
                if (existing.isEmpty()) {
                    stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
                } else {
                    existing.grow(reachedLimit ? limit : stack.getCount());
                }
                onContentsChanged(slot);
            }
            return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount == 0) return ItemStack.EMPTY;
            validateSlotIndex(slot);
            ItemStack existing = stacks.get(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;
            int toExtract = Math.min(amount, existing.getMaxStackSize());
            if (existing.getCount() <= toExtract) {
                if (!simulate) {
                    stacks.set(slot, ItemStack.EMPTY);
                    onContentsChanged(slot);
                    return existing;
                } else {
                    return existing.copy();
                }
            } else {
                if (!simulate) {
                    stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
                    onContentsChanged(slot);
                }
                return existing.copyWithCount(toExtract);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }

        protected void onContentsChanged(int slot) {
        }

        protected void validateSlotIndex(int slot) {
            if (slot < 0 || slot >= stacks.size())
                throw new RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
        }
    }

    public static class ItemHandlerHelper {
        public static ItemStack insertItemStacked(ItemStackHandler inventory, ItemStack stack, boolean simulate) {
            if (inventory == null || stack.isEmpty()) return stack;
            if (!stack.isStackable()) {
                return insertItem(inventory, stack, simulate);
            }
            int size = inventory.getSlots();
            ItemStack remainder = stack.copy();
            for (int i = 0; i < size; i++) {
                ItemStack current = inventory.getStackInSlot(i);
                if (ItemStack.isSameItemSameComponents(current, remainder)) {
                    remainder = inventory.insertItem(i, remainder, simulate);
                    if (remainder.isEmpty()) break;
                }
            }
            if (remainder.isEmpty()) return ItemStack.EMPTY;
            for (int i = 0; i < size; i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    remainder = inventory.insertItem(i, remainder, simulate);
                    if (remainder.isEmpty()) break;
                }
            }
            return remainder;
        }

        public static ItemStack insertItem(ItemStackHandler inventory, ItemStack stack, boolean simulate) {
            if (inventory == null || stack.isEmpty()) return stack;
            ItemStack remainder = stack.copy();
            for (int i = 0; i < inventory.getSlots(); i++) {
                remainder = inventory.insertItem(i, remainder, simulate);
                if (remainder.isEmpty()) break;
            }
            return remainder;
        }
    }
}
