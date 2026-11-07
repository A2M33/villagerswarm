![Villager Swarm banner: the mod icon and name, the chat command /villagers follow 40 answered with "12 villagers are following you", and four readings: villagers run at 6.9 blocks/s against 5.6 for a sprinting player, a 40-block follow radius (1 to 256), and a 12-block leap range toward a higher player.](docs/banner.png)

# Villager Swarm

A Fabric mod for Minecraft 1.21.1. Spawn a crowd of villagers, run one command, and they chase you. They run faster than you sprint and leap up ledges to reach you.

## Commands

| Command | Effect |
|---|---|
| `/villagers follow` | Villagers within 64 blocks chase you |
| `/villagers follow <radius>` | Same, with a radius from 1 to 256 |
| `/villagers stop` | Releases them |

Villagers you spawn after running the command join in once they're inside the radius. While you're dead the swarm waits and picks up again when you respawn. Leaving the server ends it. The commands need operator rights (permission level 2, the same as `/summon`). In singleplayer that means cheats have to be on.

## Install

1. Install Fabric Loader for 1.21.1.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and `villagerswarm-1.0.0.jar` in your `mods` folder.

The mod runs on the server side. On a multiplayer server, only the server needs it.

## How it works

Villagers use Minecraft's Brain AI instead of the goal system most mobs use. Every 4 ticks the mod writes the player into each villager's `WALK_TARGET` and `LOOK_TARGET` memories, which overrides their work and wander tasks. Each villager runs at about 6.9 blocks/s (a sprinting player does 5.6).

A villager on the ground leaps when you stand at least 1 block above it within 12 blocks, or when something blocks its way. The mod sizes the launch from gravity and air drag so the villager lands near you.

## Build

Fabric Loom needs JDK 25 to run. The mod itself compiles for Java 21.

```
gradlew build         # jar lands in build/libs
gradlew runGameTest   # runs the in-game tests on a headless server
```

The game tests cover running to a target, leaping onto a 3-block ledge, and the follow and stop commands.

## License

MIT
