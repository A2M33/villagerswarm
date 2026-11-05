package com.a2m33.villagerswarm;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VillagerSwarm implements ModInitializer {
	public static final int DEFAULT_RADIUS = 64;
	private static final int MAX_RADIUS = 256;
	private static final int TICK_INTERVAL = 4;

	/** Players the swarm is following, with the radius each one picked. */
	private static final Map<UUID, Integer> LEADERS = new HashMap<>();

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(CommandManager.literal("villagers")
				.then(CommandManager.literal("follow")
					.executes(context -> follow(context, DEFAULT_RADIUS))
					.then(CommandManager.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
						.executes(context -> follow(context, IntegerArgumentType.getInteger(context, "radius")))))
				.then(CommandManager.literal("stop")
					.executes(VillagerSwarm::stop))));

		ServerTickEvents.END_SERVER_TICK.register(VillagerSwarm::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LEADERS.clear());
	}

	public static boolean isLeader(UUID player) {
		return LEADERS.containsKey(player);
	}

	/** Drives every villager in range towards the player and returns how many there were. */
	public static int pull(ServerPlayerEntity player, int radius) {
		Box area = player.getBoundingBox().expand(radius);
		List<VillagerEntity> villagers = player.getServerWorld()
			.getEntitiesByClass(VillagerEntity.class, area, VillagerEntity::isAlive);
		villagers.forEach(villager -> SwarmBehavior.drive(villager, player));
		return villagers.size();
	}

	private static int follow(CommandContext<ServerCommandSource> context, int radius) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		LEADERS.put(player.getUuid(), radius);
		int count = pull(player, radius);
		context.getSource().sendFeedback(() -> Text.literal(count + " villagers are following you"), false);
		return count;
	}

	private static int stop(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		LEADERS.remove(player.getUuid());
		int count = releaseAll(player);
		context.getSource().sendFeedback(() -> Text.literal("Villagers released"), false);
		return count;
	}

	private static int releaseAll(ServerPlayerEntity player) {
		Box area = player.getBoundingBox().expand(MAX_RADIUS);
		List<VillagerEntity> villagers = player.getServerWorld()
			.getEntitiesByClass(VillagerEntity.class, area, VillagerEntity::isAlive);
		villagers.forEach(SwarmBehavior::release);
		return villagers.size();
	}

	private static void tick(MinecraftServer server) {
		if (LEADERS.isEmpty() || server.getTicks() % TICK_INTERVAL != 0) {
			return;
		}
		LEADERS.forEach((uuid, radius) -> {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
			if (player != null && !player.isSpectator()) {
				pull(player, radius);
			}
		});
	}
}
