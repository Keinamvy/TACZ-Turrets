package com.entropy.tacz_turrets.item;

import com.entropy.tacz_turrets.registry.EntityTypeRegistry;
import com.entropy.tacz_turrets.turret.TurretEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

public class SentrySpawnEggItem extends Item {
    public SentrySpawnEggItem() {
        super(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, com.entropy.tacz_turrets.TACZTurrets.id("sentry_turret_spawn_egg"))));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Direction attach = context.getClickedFace().getOpposite();
        BlockPos clicked = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(clicked);
        if (state.getCollisionShape(context.getLevel(), clicked).isEmpty()) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos spawnPos = clicked.relative(context.getClickedFace());
        TurretEntity sentry = EntityTypeRegistry.SENTRY.get().spawn(level, context.getItemInHand(), context.getPlayer(), spawnPos, EntitySpawnReason.SPAWN_ITEM_USE, true, false);
        if (sentry != null) {
            sentry.setAttachFace(attach);
            context.getItemInHand().shrink(1);
            level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, clicked);
        }
        return InteractionResult.CONSUME;
    }
}
