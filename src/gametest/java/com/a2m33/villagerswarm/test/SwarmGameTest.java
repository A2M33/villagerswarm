package com.a2m33.villagerswarm.test;

import com.a2m33.villagerswarm.SwarmBehavior;
import com.a2m33.villagerswarm.VillagerSwarm;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class SwarmGameTest implements FabricGameTest {
	private static final int FLOOR_Y = 1;
	private static final int STAND_Y = FLOOR_Y + 1;

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void villagerRunsToLeader(TestContext context) {
		placeFloor(context);
		ArmorStandEntity leader = context.spawnEntity(EntityType.ARMOR_STAND, 7, STAND_Y, 7);
		VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, 1, STAND_Y, 1);
		double[] topSpeed = {0.0};
		Vec3d[] lastPos = {villager.getPos()};

		context.runAtEveryTick(() -> {
			SwarmBehavior.drive(villager, leader);
			topSpeed[0] = Math.max(topSpeed[0], villager.getPos().distanceTo(lastPos[0]) * 20.0);
			lastPos[0] = villager.getPos();
			if (villager.distanceTo(leader) <= 3.0F) {
				System.out.println("villagerswarm top speed: " + topSpeed[0] + " blocks/s");
				context.complete();
			}
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void villagerLeapsOntoLedge(TestContext context) {
		placeFloor(context);
		for (int x = 4; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				for (int y = STAND_Y; y < STAND_Y + 3; y++) {
					context.setBlockState(x, y, z, Blocks.STONE);
				}
			}
		}
		int ledgeTop = STAND_Y + 3;
		ArmorStandEntity leader = context.spawnEntity(EntityType.ARMOR_STAND, 5, ledgeTop, 3);
		VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, 1, STAND_Y, 3);
		int absoluteTop = context.getAbsolutePos(new BlockPos(0, ledgeTop, 0)).getY();

		context.runAtEveryTick(() -> {
			SwarmBehavior.drive(villager, leader);
			if (villager.isOnGround() && villager.getY() >= absoluteTop) {
				context.complete();
			}
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void commandsToggleFollowing(TestContext context) {
		placeFloor(context);
		VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, 1, STAND_Y, 1);
		FakePlayer player = FakePlayer.get(context.getWorld());
		player.setPosition(context.getAbsolute(new Vec3d(4, STAND_Y, 4)));
		// Pin the level so the test does not depend on how the test server ranks a fake player.
		ServerCommandSource source = player.getCommandSource().withLevel(2);
		CommandManager commands = context.getWorld().getServer().getCommandManager();

		commands.executeWithPrefix(source, "villagers follow 16");
		context.assertTrue(VillagerSwarm.isLeader(player.getUuid()), "player was not registered as leader");
		context.assertTrue(villager.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET), "villager got no walk target");

		commands.executeWithPrefix(source, "villagers stop");
		context.assertFalse(VillagerSwarm.isLeader(player.getUuid()), "player is still a leader");
		context.assertFalse(villager.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET), "villager kept its walk target");
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void commandsNeedOperator(TestContext context) {
		FakePlayer player = FakePlayer.get(context.getWorld(), new GameProfile(UUID.randomUUID(), "swarm-guest"));
		ServerCommandSource source = player.getCommandSource().withLevel(0);
		context.getWorld().getServer().getCommandManager().executeWithPrefix(source, "villagers follow");
		context.assertFalse(VillagerSwarm.isLeader(player.getUuid()), "a non-operator started a swarm");
		context.complete();
	}

	private static void placeFloor(TestContext context) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				context.setBlockState(x, FLOOR_Y, z, Blocks.STONE);
			}
		}
	}
}
