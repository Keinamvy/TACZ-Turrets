package com.entropy.tacz_turrets.turret.ai;

import com.entropy.tacz_turrets.turret.TurretEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryCondition;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.tslat.smartbrainlib.api.core.behaviour.base.ExtendedBehaviour;
import net.tslat.smartbrainlib.library.object.MemoryTest;
import net.tslat.smartbrainlib.util.BrainUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class TaczShootAttack<E extends TurretEntity> extends ExtendedBehaviour<E> {
    private static final MemoryTest MEMORY_REQUIREMENTS = MemoryTest.builder(2).hasMemory(MemoryModuleType.ATTACK_TARGET).noMemory(MemoryModuleType.ATTACK_COOLING_DOWN);
    private @Nullable LivingEntity target;

    public TaczShootAttack() {
        runFor(e -> 1);
        cooldownFor(e -> 0);
    }

    @Override
    public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull E turret) {
        target = BrainUtil.getTargetOfEntity(turret);
        return turret.isEnabled() && target != null && target.isAlive() && BrainUtil.canSee(turret, target) && turret.isValidTarget(target);
    }

    @Override
    protected void start(E turret) {
        if (target == null || !target.isAlive() || !BrainUtil.canSee(turret, target)) return;
        turret.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        BehaviorUtils.lookAtEntity(turret, target);
        if (turret.hasGun() && (turret.hasLineOfSight(target) || BrainUtil.canSee(turret, target))) turret.tryShoot();
    }

    @Override
    protected boolean shouldKeepRunning(E turret) {
        return checkExtraStartConditions((ServerLevel) turret.level(), turret);
    }

    @Override
    protected void tick(E turret) {
        start(turret);
    }
}
