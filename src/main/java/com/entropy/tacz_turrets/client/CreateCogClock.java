package com.entropy.tacz_turrets.client;

import com.entropy.tacz_turrets.turret.TurretEntity;
import net.minecraft.core.BlockPos;

public class CreateCogClock {
    public static float degrees(TurretEntity turret, float partialTick) {
        float time = turret.tickCount + partialTick;
        BlockPos pos = turret.blockPosition();
        float offset = (pos.getX() + pos.getY() + pos.getZ() - pos.get(turret.getAttachFace().getAxis())) % 2 == 0 ? 22.5F : 0.0F;
        return (time * turret.getRotationSpeed() * 3.0F / 10.0F + offset) % 360.0F;
    }
}
