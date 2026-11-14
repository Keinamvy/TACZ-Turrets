package com.entropy.tacz_turrets.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class TACZTurretsNetwork {
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(ToggleAllyPacket.TYPE, ToggleAllyPacket.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToggleAllyPacket.TYPE, ToggleAllyPacket::handle);
    }

    public static void sendToServer(ToggleAllyPacket packet) {
        ClientPlayNetworking.send(packet);
    }
}
