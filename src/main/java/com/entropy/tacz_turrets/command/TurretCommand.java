package com.entropy.tacz_turrets.command;

import com.entropy.tacz_turrets.TACZTurrets;
import com.entropy.tacz_turrets.util.TurretAllies;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TurretCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("turret")
                .then(Commands.literal("trust")
                        .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                .executes(context -> setTrusted(context, true))))
                .then(Commands.literal("untrust")
                        .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                .executes(context -> setTrusted(context, false))))
                .then(Commands.literal("trusted")
                        .executes(TurretCommand::listTrusted)));
    }

    private static int setTrusted(CommandContext<CommandSourceStack> context, boolean trust) throws CommandSyntaxException {
        UUID owner = context.getSource().getPlayerOrException().getUUID();
        TurretAllies allies = TurretAllies.get(context.getSource().getServer());
        int changed = 0;
        for (net.minecraft.server.players.NameAndId profile : GameProfileArgument.getGameProfiles(context, "players")) {
            if (trust ? allies.addAlly(owner, profile.id()) : allies.removeAlly(owner, profile.id())) changed++;
        }
        Component message = Component.translatable(trust ? "command.tacz_turrets.trusted" : "command.tacz_turrets.untrusted", changed);
        context.getSource().sendSuccess(() -> message, false);
        return changed;
    }

    private static int listTrusted(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID owner = context.getSource().getPlayerOrException().getUUID();
        MinecraftServer server = context.getSource().getServer();
        Set<UUID> trusted = TurretAllies.get(server).getAllies(owner);

        if (trusted.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("command.tacz_turrets.trusted_none"), false);
            return 0;
        }

        String names = trusted.stream()
                .map(id -> {
                    var player = server.getPlayerList().getPlayer(id);
                    return player != null ? player.getGameProfile().name() : id.toString();
                })
                .collect(Collectors.joining(", "));
        context.getSource().sendSuccess(() -> Component.translatable("command.tacz_turrets.trusted_list", trusted.size(), names), false);
        return trusted.size();
    }
}
