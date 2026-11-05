package com.a2m33.villagerswarm;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Villagers run on a Brain instead of goals, so following means writing into its memories. */
public final class SwarmBehavior {
	// Mob speed scales with the square of this: 0.8 is about 7 blocks/s, faster than a sprinting player.
	private static final float RUN_SPEED = 0.8F;
	private static final int STOP_DISTANCE = 2;
	private static final double LEAP_RANGE = 12.0;
	private static final double MIN_LEAP_HEIGHT = 1.25;
	private static final double MAX_LEAP_HEIGHT = 6.0;
	private static final double GRAVITY = 0.08;
	private static final double AIR_DRAG = 0.09;

	private SwarmBehavior() {
	}

	public static void drive(VillagerEntity villager, Entity leader) {
		// Walking off mid-trade would close the player's trading screen.
		if (villager.hasCustomer()) {
			return;
		}
		if (villager.isSleeping()) {
			villager.wakeUp();
		}
		villager.fallDistance = 0.0F;

		Brain<VillagerEntity> brain = villager.getBrain();
		brain.remember(MemoryModuleType.WALK_TARGET, new WalkTarget(leader, RUN_SPEED, STOP_DISTANCE));
		brain.remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(leader, true));

		if (shouldLeap(villager, leader)) {
			leap(villager, leader);
		}
	}

	public static void release(VillagerEntity villager) {
		Brain<VillagerEntity> brain = villager.getBrain();
		brain.forget(MemoryModuleType.WALK_TARGET);
		brain.forget(MemoryModuleType.LOOK_TARGET);
	}

	private static boolean shouldLeap(VillagerEntity villager, Entity leader) {
		if (!villager.isOnGround()) {
			return false;
		}
		double height = leader.getY() - villager.getY();
		return villager.horizontalCollision || (height >= 1.0 && horizontalDistance(villager, leader) <= LEAP_RANGE);
	}

	private static void leap(VillagerEntity villager, Entity leader) {
		double height = MathHelper.clamp(leader.getY() - villager.getY() + 1.0, MIN_LEAP_HEIGHT, MAX_LEAP_HEIGHT);
		double upward = Math.sqrt(2.0 * GRAVITY * height) * 1.1;

		// Air drag eats horizontal speed, so the launch speed is sized to cover the gap before it runs out.
		double distance = horizontalDistance(villager, leader);
		double forward = MathHelper.clamp(distance * AIR_DRAG, 0.2, 0.6);
		Vec3d direction = leader.getPos().subtract(villager.getPos()).multiply(1.0, 0.0, 1.0).normalize();

		villager.setVelocity(direction.x * forward, upward, direction.z * forward);
		villager.velocityModified = true;
	}

	private static double horizontalDistance(Entity from, Entity to) {
		return Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());
	}
}
