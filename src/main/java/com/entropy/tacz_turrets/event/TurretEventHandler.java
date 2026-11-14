package com.entropy.tacz_turrets.event;

import com.entropy.tacz_turrets.config.TACZTurretsConfig;
import com.entropy.tacz_turrets.turret.RetaliateTargeting;
import com.entropy.tacz_turrets.turret.SentryLoadout;
import com.entropy.tacz_turrets.turret.SentrySpawn;
import com.entropy.tacz_turrets.turret.TurretEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TurretEventHandler {
    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            TurretEntity turret = getAttackingTurret(source);
            if (turret != null && turret.isProtectedFromFire(entity)) {
                return false;
            }
            handleLivingHurt(entity, source);
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            handleLivingDeath(entity);
        });

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SentryLoadout.Listener());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SentrySpawn.Listener());
    }

    private static void handleLivingHurt(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide()) return;

        TurretEntity turret = getAttackingTurret(source);
        if (turret != null) {
            if (TACZTurretsConfig.CREDIT_KILLS_TO_OWNER.get()) {
                Player owner = turret.getOwnerPlayer();
                if (owner != null && owner != victim) victim.setLastHurtByPlayer(owner, 100);
            }
            return;
        }

        if (TACZTurretsConfig.PROTECT_OWNER.get() && victim instanceof Player owner && source.getEntity() instanceof LivingEntity attacker) {
            defendOwner(owner, attacker);
        }
    }

    private static void handleLivingDeath(LivingEntity entity) {
        if (TACZTurretsConfig.RETALIATE_TARGETING.get() != RetaliateTargeting.CLEAR_ON_DEATH) return;
        if (!(entity instanceof Player player) || player.level().isClientSide()) return;

        double range = TACZTurretsConfig.maxRange();
        for (TurretEntity turret : player.level().getEntitiesOfClass(TurretEntity.class, AABB.ofSize(player.position(), range * 2, range * 2, range * 2))) {
            turret.forgetRetaliation(player.getUUID());
        }
    }

    @Nullable
    private static TurretEntity getAttackingTurret(DamageSource source) {
        if (source.getEntity() instanceof TurretEntity turret) return turret;
        if (source.getDirectEntity() instanceof TurretEntity turret) return turret;
        return null;
    }

    private static void defendOwner(Player owner, LivingEntity attacker) {
        if (attacker == owner) return;
        double range = TACZTurretsConfig.maxRange();
        List<TurretEntity> turrets = owner.level().getEntitiesOfClass(TurretEntity.class, AABB.ofSize(owner.position(), range * 2, range, range * 2), turret -> owner.getUUID().equals(turret.owner) && turret.distanceTo(owner) <= turret.stats().range.get());
        for (TurretEntity turret : turrets) {
            if (attacker instanceof Player player) turret.markRetaliation(player);
            if (turret.isValidTarget(attacker)) turret.alertTo(attacker);
        }
    }
}
