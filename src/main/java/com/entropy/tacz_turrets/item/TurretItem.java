package com.entropy.tacz_turrets.item;

import com.entropy.tacz_turrets.client.renderer.TurretItemRenderer;
import com.entropy.tacz_turrets.config.TACZTurretsConfig;
import com.entropy.tacz_turrets.registry.SoundRegistry;
import com.entropy.tacz_turrets.turret.TurretEntity;
import com.entropy.tacz_turrets.turret.TurretType;
import com.entropy.tacz_turrets.util.PlacedTurrets;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class TurretItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final TurretType type;

    public TurretItem(TurretType type) {
        super(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, com.entropy.tacz_turrets.TACZTurrets.id(type.id() + "_turret"))));
        this.type = type;
    }

    public TurretType getType() {
        return type;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private TurretItemRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new TurretItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Direction attach = context.getClickedFace().getOpposite();
        if (turretInTheWay(level, pos, attach)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!TACZTurretsConfig.enabled(type)) {
            if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer sp) sp.sendSystemMessage(Component.translatable("message.tacz_turrets.turret_disabled"), true);
            return InteractionResult.FAIL;
        }
        int limit = TACZTurretsConfig.MAX_TURRETS_PER_PLAYER.get();
        if (limit > 0 && !context.getPlayer().isCreative() && level.getServer() != null && PlacedTurrets.get(level.getServer()).count(context.getPlayer().getUUID()) >= limit) {
            if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer sp) sp.sendSystemMessage(Component.translatable("message.tacz_turrets.turret_limit", limit), true);
            return InteractionResult.FAIL;
        }
        TurretEntity turret = new TurretEntity(level, pos, context.getPlayer(), type);
        turret.setAttachFace(attach);
        turret.loadPaint(context.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
        level.addFreshEntity(turret);
        turret.trackPlacement();
        turret.playTurretSound(SoundRegistry.TURRET_PLACE.get());
        if (!context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    private static boolean turretInTheWay(Level level, BlockPos pos, Direction attach) {
        BlockPos landing = pos;
        while (attach == Direction.DOWN && landing.getY() > level.getMinY() && level.getBlockState(landing.below()).getCollisionShape(level, landing.below()).isEmpty()) {
            landing = landing.below();
        }
        return !level.getEntitiesOfClass(TurretEntity.class, new AABB(landing).minmax(new AABB(pos))).isEmpty();
    }
}
