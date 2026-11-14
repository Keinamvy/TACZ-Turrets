package com.entropy.tacz_turrets.network;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.menu.TurretMenu;
import com.entropy.tacz_turrets.turret.TurretEntity;
import com.entropy.tacz_turrets.util.TurretAllies;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public record ToggleAllyPacket(UUID target) implements CustomPacketPayload {
    public static final Type<ToggleAllyPacket> TYPE = new Type<>(TACZTurrets.id("toggle_ally"));
    public static final StreamCodec<ByteBuf, ToggleAllyPacket> STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(ToggleAllyPacket::new, ToggleAllyPacket::target);

    @Override
    public Type<ToggleAllyPacket> type() {
        return TYPE;
    }

    public static void handle(ToggleAllyPacket packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> packet.apply(context.player()));
    }

    private void apply(ServerPlayer sender) {
        if (sender == null || !(sender.containerMenu instanceof TurretMenu menu)) return;
        TurretEntity turret = menu.getTurret();
        if (turret == null || !turret.isOwnedBy(sender) || turret.owner == null || turret.owner.equals(target)) return;
        if (sender.level().getServer() != null) {
            TurretAllies.get(sender.level().getServer()).toggleAlly(turret.owner, target);
        }
    }
}
