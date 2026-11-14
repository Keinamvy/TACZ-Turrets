package com.entropy.tacz_turrets.compat;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class OpenPACCompat {
    public static boolean isAlly(MinecraftServer server, UUID owner, UUID target) {
        return false;
    }
}
