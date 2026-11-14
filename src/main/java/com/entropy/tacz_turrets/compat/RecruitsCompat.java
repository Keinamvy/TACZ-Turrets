package com.entropy.tacz_turrets.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

import java.util.UUID;

public class RecruitsCompat {
    public static boolean isSameTeam(LivingEntity a, LivingEntity b) {
        return false;
    }

    public static boolean isOwnRecruit(UUID owner, LivingEntity target) {
        return false;
    }

    public static boolean isAlliedFaction(PlayerTeam ownerTeam, Team targetTeam) {
        return false;
    }
}
